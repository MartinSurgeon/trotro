package com.example.sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.BroadcastStatus
import com.example.data.CampaignRepository
import com.example.data.ContactEntity
import com.example.data.DeliveryStatus
import com.example.data.MessageBroadcastEntity
import com.example.data.RecipientDeliveryEntity
import com.example.data.SendMode
import com.example.data.SettingsStore
import com.example.util.MessageTemplates
import com.example.util.PhoneNumbers
import com.example.util.SmsCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmsSenderService(
    private val context: Context,
    private val repository: CampaignRepository,
    private val settings: SettingsStore
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val SMSONLINEGH_SEND_URL = "https://api.smsonlinegh.com/v5/message/sms/send"
        private const val GATEWAY_BATCH_SIZE = 100
        private const val SIM_SEND_SPACING_MS = 150L
    }

    fun isSmsOnlineGhApiKeyConfigured(): Boolean = settings.effectiveApiKey() != null

    fun hasSendSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isSimReady(): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm != null && tm.simState == TelephonyManager.SIM_STATE_READY
        } catch (_: Exception) {
            false
        }
    }

    /** Text actually sent: smart quotes and dashes flattened so they don't force Unicode billing. */
    private fun prepareText(message: String): String = SmsCounter.simplifyPunctuation(message.trim())

    /**
     * Sends via the SMSOnlineGH v5 API (https://dev.smsonlinegh.com/).
     * POST https://api.smsonlinegh.com/v5/message/sms/send
     * Headers: Content-Type/Accept: application/json, Authorization: key <API_KEY>
     * Payload: { "text": "...", "type": 0, "sender": "<SenderID>", "destinations": ["233..."] }
     *
     * Messages without {name} are identical for everyone, so they go out in batches of
     * [GATEWAY_BATCH_SIZE] numbers per request. Personalised messages need one request each.
     */
    suspend fun sendViaSmsOnlineGh(
        senderId: String,
        candidateLabel: String,
        messageTemplate: String,
        recipients: List<ContactEntity>,
        onBroadcastCreated: (Long) -> Unit = {}
    ): Long = withContext(Dispatchers.IO) {
        val cleanSender = senderId.trim().take(11)
        val text = prepareText(messageTemplate)
        val apiKey = settings.effectiveApiKey()

        val setupProblem = gatewayProblem(apiKey, cleanSender)
        if (setupProblem != null) {
            val (broadcastId, _) = repository.createBroadcastWithRecipients(
                senderIdUsed = cleanSender,
                candidateLabel = candidateLabel,
                messageText = text,
                recipients = recipients,
                sendMode = SendMode.SMSONLINEGH,
                initialOverallStatus = BroadcastStatus.FAILED,
                initialStatusDetail = setupProblem,
                initialRecipientStatus = DeliveryStatus.FAILED,
                initialRecipientDetail = setupProblem
            )
            onBroadcastCreated(broadcastId)
            return@withContext broadcastId
        }

        val (broadcastId, deliveries) = repository.createBroadcastWithRecipients(
            senderIdUsed = cleanSender,
            candidateLabel = candidateLabel,
            messageText = text,
            recipients = recipients,
            sendMode = SendMode.SMSONLINEGH,
            initialOverallStatus = BroadcastStatus.SENDING,
            initialStatusDetail = "Sending to ${recipients.size} people…",
            initialRecipientStatus = DeliveryStatus.SENDING
        )
        onBroadcastCreated(broadcastId)
        dispatchGateway(apiKey!!, cleanSender, text, deliveries)
        broadcastId
    }

    private fun gatewayProblem(apiKey: String?, sender: String): String? = when {
        apiKey == null -> "SMS Online GH is not set up. Add your API key in Settings."
        sender.isEmpty() -> "Choose a sender name in Settings before sending with SMS Online GH."
        else -> null
    }

    private suspend fun dispatchGateway(
        apiKey: String,
        sender: String,
        text: String,
        deliveries: List<RecipientDeliveryEntity>
    ) {
        if (MessageTemplates.containsName(text)) {
            deliveries.forEach { delivery ->
                val personal = MessageTemplates.personalize(text, delivery.contactName)
                submitToGateway(apiKey, sender, personal, listOf(delivery))
            }
        } else {
            deliveries.chunked(GATEWAY_BATCH_SIZE).forEach { batch ->
                submitToGateway(apiKey, sender, text, batch)
            }
        }
    }

    /**
     * Sends a message again to recipients of an existing message (already reset to "sending"),
     * using the same method it was first sent with.
     */
    suspend fun retry(broadcast: MessageBroadcastEntity, deliveries: List<RecipientDeliveryEntity>) =
        withContext(Dispatchers.IO) {
            if (deliveries.isEmpty()) return@withContext
            val problem: String? = when (broadcast.sendMode) {
                SendMode.SMSONLINEGH -> {
                    val apiKey = settings.effectiveApiKey()
                    gatewayProblem(apiKey, broadcast.senderIdUsed) ?: run {
                        dispatchGateway(apiKey!!, broadcast.senderIdUsed, broadcast.messageText, deliveries)
                        null
                    }
                }
                SendMode.DIRECT_SIM -> {
                    val smsManager = smsManagerOrNull()
                    simProblem(smsManager) ?: run {
                        dispatchSim(smsManager!!, broadcast.messageText, deliveries)
                        null
                    }
                }
                else -> "This message was sent from your Messages app, so it can't be retried here."
            }
            if (problem != null) {
                deliveries.forEach {
                    repository.updateRecipientStatusAndRefreshBroadcast(it.id, DeliveryStatus.FAILED, problem)
                }
            }
        }

    private suspend fun submitToGateway(
        apiKey: String,
        sender: String,
        text: String,
        batch: List<RecipientDeliveryEntity>
    ) {
        val numbers = batch.map { PhoneNumbers.toInternational(it.phoneNumber) }
        val payload = JSONObject().apply {
            put("text", text)
            put("type", 0)
            put("sender", sender)
            put("destinations", JSONArray(numbers))
        }.toString()

        val request = Request.Builder()
            .url(SMSONLINEGH_SEND_URL)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "key $apiKey")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val outcome: List<Pair<String, String?>> = try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                val handshake = json?.optJSONObject("handshake")
                val handshakeOk = handshake?.optInt("id", -1) == 0 ||
                    handshake?.optString("label").equals("HSHK_OK", ignoreCase = true)

                if (response.isSuccessful && handshakeOk) {
                    val destinations = json?.optJSONObject("data")?.optJSONArray("destinations")
                    numbers.mapIndexed { index, number ->
                        classifyDestination(findDestination(destinations, number, index))
                    }
                } else {
                    val reason = describeGatewayError(response.code, handshake?.optString("label").orEmpty())
                    numbers.map { DeliveryStatus.FAILED to reason }
                }
            }
        } catch (e: Exception) {
            val reason = "No internet connection, or SMS Online GH could not be reached."
            numbers.map { DeliveryStatus.FAILED to reason }
        }

        batch.zip(outcome).forEach { (delivery, result) ->
            repository.updateRecipientStatusAndRefreshBroadcast(delivery.id, result.first, result.second)
        }
    }

    private fun findDestination(destinations: JSONArray?, number: String, index: Int): JSONObject? {
        if (destinations == null) return null
        for (i in 0 until destinations.length()) {
            val item = destinations.optJSONObject(i) ?: continue
            val to = item.optString("to").ifBlank { item.optString("destination") }.filter { it.isDigit() }
            if (to.isNotEmpty() && to == number) return item
        }
        return destinations.optJSONObject(index)
    }

    private fun classifyDestination(destination: JSONObject?): Pair<String, String?> {
        val label = destination?.optJSONObject("status")?.optString("label").orEmpty()
        return when {
            label.equals("DS_DELIVERED", ignoreCase = true) -> DeliveryStatus.DELIVERED to null
            listOf("REJECT", "UNDELIV", "EXPIRED", "INVALID", "FAIL", "ERR").any { label.contains(it, ignoreCase = true) } ->
                DeliveryStatus.FAILED to "Rejected by SMS Online GH ($label). Check the number."
            else -> DeliveryStatus.SUBMITTED to null
        }
    }

    private fun describeGatewayError(httpCode: Int, label: String): String = when {
        httpCode == 401 || httpCode == 403 || label.contains("AUTH", ignoreCase = true) ->
            "SMS Online GH did not accept your API key. Check it in Settings."
        label.contains("BALANCE", ignoreCase = true) || label.contains("CREDIT", ignoreCase = true) ->
            "Not enough SMS credit on your SMS Online GH account."
        label.contains("SENDER", ignoreCase = true) ->
            "The sender name is not approved on your SMS Online GH account."
        label.isNotBlank() -> "SMS Online GH refused the message ($label)."
        else -> "SMS Online GH could not send the message (error $httpCode)."
    }

    /** Sends through the phone's own SIM using SmsManager. Costs the user's airtime or bundle. */
    suspend fun sendViaDirectSim(
        candidateLabel: String,
        messageTemplate: String,
        recipients: List<ContactEntity>,
        onBroadcastCreated: (Long) -> Unit = {}
    ): Long = withContext(Dispatchers.IO) {
        val text = prepareText(messageTemplate)
        val smsManager = smsManagerOrNull()

        val setupProblem = simProblem(smsManager)
        if (setupProblem != null) {
            val (broadcastId, _) = repository.createBroadcastWithRecipients(
                senderIdUsed = "",
                candidateLabel = candidateLabel,
                messageText = text,
                recipients = recipients,
                sendMode = SendMode.DIRECT_SIM,
                initialOverallStatus = BroadcastStatus.FAILED,
                initialStatusDetail = setupProblem,
                initialRecipientStatus = DeliveryStatus.FAILED,
                initialRecipientDetail = setupProblem
            )
            onBroadcastCreated(broadcastId)
            return@withContext broadcastId
        }

        val (broadcastId, deliveries) = repository.createBroadcastWithRecipients(
            senderIdUsed = "",
            candidateLabel = candidateLabel,
            messageText = text,
            recipients = recipients,
            sendMode = SendMode.DIRECT_SIM,
            initialOverallStatus = BroadcastStatus.SENDING,
            initialStatusDetail = "Sending to ${recipients.size} people…",
            initialRecipientStatus = DeliveryStatus.SENDING
        )
        onBroadcastCreated(broadcastId)
        dispatchSim(smsManager!!, text, deliveries)
        broadcastId
    }

    private fun smsManagerOrNull(): SmsManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    } catch (_: Exception) {
        null
    }

    private fun simProblem(smsManager: SmsManager?): String? = when {
        !hasSendSmsPermission() -> "Dawuro is not allowed to send SMS. Allow it in your phone settings."
        smsManager == null -> "This device cannot send SMS."
        !isSimReady() -> "No SIM card is ready. Check that your SIM is inserted and has signal."
        else -> null
    }

    private suspend fun dispatchSim(smsManager: SmsManager, text: String, deliveries: List<RecipientDeliveryEntity>) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        deliveries.forEach { delivery ->
            try {
                val personal = MessageTemplates.personalize(text, delivery.contactName)
                val sentPi = statusIntent(SmsStatusReceiver.ACTION_SMS_SENT, delivery.id, flags)
                val deliveredPi = statusIntent(SmsStatusReceiver.ACTION_SMS_DELIVERED, delivery.id, flags)
                val parts = smsManager.divideMessage(personal)
                if (parts.size <= 1) {
                    smsManager.sendTextMessage(delivery.phoneNumber, null, personal, sentPi, deliveredPi)
                } else {
                    smsManager.sendMultipartTextMessage(
                        delivery.phoneNumber,
                        null,
                        parts,
                        ArrayList(List(parts.size) { sentPi }),
                        ArrayList(List(parts.size) { deliveredPi })
                    )
                }
            } catch (e: Exception) {
                repository.updateRecipientStatusAndRefreshBroadcast(
                    deliveryId = delivery.id,
                    newStatus = DeliveryStatus.FAILED,
                    errorDetail = "The SMS could not be queued on your SIM."
                )
            }
            // Spacing out sends keeps the phone's SMS queue from choking on large lists.
            delay(SIM_SEND_SPACING_MS)
        }
    }

    private fun statusIntent(action: String, deliveryId: Long, flags: Int): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            this.action = action
            putExtra(SmsStatusReceiver.EXTRA_DELIVERY_ID, deliveryId)
        }
        val offset = if (action == SmsStatusReceiver.ACTION_SMS_SENT) 0 else 1
        return PendingIntent.getBroadcast(context, (deliveryId * 2 + offset).toInt(), intent, flags)
    }

    /**
     * Opens the phone's default SMS app pre-filled with the recipients and message. The user
     * presses send there, so Dawuro cannot track delivery.
     */
    suspend fun sendViaSystemSmsApp(
        activityContext: Context,
        candidateLabel: String,
        messageTemplate: String,
        recipients: List<ContactEntity>,
        onBroadcastCreated: (Long) -> Unit = {}
    ): Pair<Long, Boolean> {
        val text = prepareText(messageTemplate)
        val (broadcastId, deliveries) = withContext(Dispatchers.IO) {
            repository.createBroadcastWithRecipients(
                senderIdUsed = "",
                candidateLabel = candidateLabel,
                messageText = text,
                recipients = recipients,
                sendMode = SendMode.SMS_APP,
                initialOverallStatus = BroadcastStatus.OPENED_IN_SMS_APP,
                initialStatusDetail = "Opened in your Messages app. Press send there to finish.",
                initialRecipientStatus = DeliveryStatus.OPENED_IN_APP
            )
        }
        onBroadcastCreated(broadcastId)

        val separator = if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) "," else ";"
        val numbersJoined = recipients.joinToString(separator) { it.phoneNumber }
        // One shared message can't greet each person by name, so {name} is dropped.
        val body = MessageTemplates.personalize(text, recipients.singleOrNull()?.name)

        val launched = try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$numbersJoined")).apply {
                putExtra("sms_body", body)
                if (activityContext !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(intent)
            true
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                deliveries.forEach { d ->
                    repository.updateRecipientStatusAndRefreshBroadcast(
                        deliveryId = d.id,
                        newStatus = DeliveryStatus.FAILED,
                        errorDetail = "No Messages app was found on this phone."
                    )
                }
            }
            false
        }

        return broadcastId to launched
    }
}
