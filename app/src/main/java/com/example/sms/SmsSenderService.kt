package com.example.sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.CampaignRepository
import com.example.data.ContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val repository: CampaignRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        const val SMSONLINEGH_SEND_URL = "https://api.smsonlinegh.com/v5/message/sms/send"
        const val ACTION_SMS_SENT = "com.example.sms.ACTION_SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.sms.ACTION_SMS_DELIVERED"
        const val EXTRA_DELIVERY_ID = "extra_delivery_id"
    }

    private var receiversRegistered = false

    private val sentReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val deliveryId = intent?.getLongExtra(EXTRA_DELIVERY_ID, -1L) ?: -1L
            if (deliveryId <= 0L) return
            val code = resultCode
            serviceScope.launch {
                if (code == Activity.RESULT_OK) {
                    repository.updateRecipientStatusAndRefreshBroadcast(
                        deliveryId = deliveryId,
                        newStatus = "SENT",
                        errorDetail = null
                    )
                } else {
                    val errorMsg = when (code) {
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Carrier generic failure (check SIM credit/plan)"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "No cellular network service"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "Invalid message format (Null PDU)"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "Cellular radio / Airplane mode is on"
                        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "SMS sending limit exceeded"
                        SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> "Short code not allowed"
                        else -> "SMS error (code $code)"
                    }
                    repository.updateRecipientStatusAndRefreshBroadcast(
                        deliveryId = deliveryId,
                        newStatus = "FAILED",
                        errorDetail = errorMsg
                    )
                }
            }
        }
    }

    private val deliveredReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val deliveryId = intent?.getLongExtra(EXTRA_DELIVERY_ID, -1L) ?: -1L
            if (deliveryId <= 0L) return
            val code = resultCode
            serviceScope.launch {
                if (code == Activity.RESULT_OK) {
                    repository.updateRecipientStatusAndRefreshBroadcast(
                        deliveryId = deliveryId,
                        newStatus = "DELIVERED",
                        errorDetail = null
                    )
                }
            }
        }
    }

    @Synchronized
    fun ensureReceiversRegistered() {
        if (receiversRegistered) return
        try {
            ContextCompat.registerReceiver(
                context.applicationContext,
                sentReceiver,
                IntentFilter(ACTION_SMS_SENT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            ContextCompat.registerReceiver(
                context.applicationContext,
                deliveredReceiver,
                IntentFilter(ACTION_SMS_DELIVERED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiversRegistered = true
        } catch (_: Exception) {
            // Ignore in headless unit test environments
        }
    }

    fun isSmsOnlineGhApiKeyConfigured(): Boolean {
        val key = BuildConfig.SMSONLINEGH_API_KEY.trim()
        return key.isNotEmpty() &&
            key != "YOUR_SMSONLINEGH_API_KEY" &&
            key != "MY_SMSONLINEGH_API_KEY"
    }

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

    /**
     * Sends SMS via the official SMSOnlineGH v5 API (https://dev.smsonlinegh.com/).
     * Endpoint: POST https://api.smsonlinegh.com/v5/message/sms/send
     * Headers:
     *   Content-Type: application/json
     *   Accept: application/json
     *   Authorization: key <SMSONLINEGH_API_KEY>
     * Payload:
     *   { "text": "...", "type": 0, "sender": "<SenderID>", "destinations": ["..."] }
     */
    suspend fun sendViaSmsOnlineGh(
        senderId: String,
        candidateLabel: String,
        messageTemplate: String,
        recipients: List<ContactEntity>
    ): Long = withContext(Dispatchers.IO) {
        val cleanSender = senderId.trim().take(11).ifEmpty { "CAMPAIGN" }
        val apiKey = BuildConfig.SMSONLINEGH_API_KEY.trim()

        if (!isSmsOnlineGhApiKeyConfigured()) {
            val (broadcastId, deliveries) = repository.createBroadcastWithRecipients(
                senderIdUsed = cleanSender,
                candidateLabel = candidateLabel,
                messageText = messageTemplate,
                recipients = recipients,
                sendMode = "SMSONLINEGH",
                initialOverallStatus = "FAILED",
                initialStatusDetail = "SMSONLINEGH_API_KEY is not set. Add your SMSOnlineGH API key in the AI Studio Secrets panel.",
                initialRecipientStatus = "FAILED"
            )
            deliveries.forEach {
                repository.updateRecipientStatusAndRefreshBroadcast(
                    it.id,
                    "FAILED",
                    "Missing SMSONLINEGH_API_KEY in AI Studio Secrets"
                )
            }
            return@withContext broadcastId
        }

        val (broadcastId, deliveries) = repository.createBroadcastWithRecipients(
            senderIdUsed = cleanSender,
            candidateLabel = candidateLabel,
            messageText = messageTemplate,
            recipients = recipients,
            sendMode = "SMSONLINEGH",
            initialOverallStatus = "SENDING",
            initialStatusDetail = "Submitting ${recipients.size} message(s) to SMSOnlineGH under Sender ID '$cleanSender'...",
            initialRecipientStatus = "SENDING"
        )

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        deliveries.forEach { delivery ->
            try {
                val personalizedText = personalizeMessage(messageTemplate, delivery.contactName)
                val formattedDestination = formatGhanaOrIntlPhone(delivery.phoneNumber)

                val payloadJson = JSONObject().apply {
                    put("text", personalizedText)
                    put("type", 0)
                    put("sender", cleanSender)
                    put("destinations", JSONArray().put(formattedDestination))
                }.toString()

                val request = Request.Builder()
                    .url(SMSONLINEGH_SEND_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "key $apiKey")
                    .post(payloadJson.toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    val parsedJson = try {
                        if (rawBody.isNotBlank()) JSONObject(rawBody) else null
                    } catch (_: Exception) {
                        null
                    }

                    val handshake = parsedJson?.optJSONObject("handshake")
                    val handshakeId = handshake?.optInt("id", -1) ?: -1
                    val handshakeLabel = handshake?.optString("label", "").orEmpty()

                    if (response.isSuccessful && (handshakeId == 0 || handshakeLabel.equals("HSHK_OK", ignoreCase = true))) {
                        // Inspect per-destination status in data.destinations if present
                        val destArray = parsedJson?.optJSONObject("data")?.optJSONArray("destinations")
                        val firstDest = if (destArray != null && destArray.length() > 0) {
                            destArray.optJSONObject(0)
                        } else {
                            null
                        }
                        val destStatusObj = firstDest?.optJSONObject("status")
                        val destStatusLabel = destStatusObj?.optString("label", "HSHK_OK").orEmpty()

                        when {
                            destStatusLabel.contains("REJECTED", ignoreCase = true) ||
                                destStatusLabel.contains("UNDELIVERED", ignoreCase = true) ||
                                destStatusLabel.contains("EXPIRED", ignoreCase = true) -> {
                                repository.updateRecipientStatusAndRefreshBroadcast(
                                    deliveryId = delivery.id,
                                    newStatus = "FAILED",
                                    errorDetail = "SMSOnlineGH rejected: $destStatusLabel"
                                )
                            }
                            destStatusLabel.equals("DS_DELIVERED", ignoreCase = true) -> {
                                repository.updateRecipientStatusAndRefreshBroadcast(
                                    deliveryId = delivery.id,
                                    newStatus = "DELIVERED",
                                    errorDetail = "DS_DELIVERED"
                                )
                            }
                            else -> {
                                // Honest status: submitted & accepted by SMSOnlineGH (HSHK_OK / DS_PENDING_ENROUTE)
                                repository.updateRecipientStatusAndRefreshBroadcast(
                                    deliveryId = delivery.id,
                                    newStatus = "SUBMITTED",
                                    errorDetail = destStatusLabel.ifBlank { "HSHK_OK" }
                                )
                            }
                        }
                    } else {
                        val reason = when {
                            handshakeLabel.isNotBlank() -> "SMSOnlineGH: $handshakeLabel"
                            !response.isSuccessful -> "SMSOnlineGH HTTP ${response.code}"
                            else -> "SMSOnlineGH rejected request"
                        }
                        repository.updateRecipientStatusAndRefreshBroadcast(
                            deliveryId = delivery.id,
                            newStatus = "FAILED",
                            errorDetail = reason
                        )
                    }
                }
            } catch (e: Exception) {
                repository.updateRecipientStatusAndRefreshBroadcast(
                    deliveryId = delivery.id,
                    newStatus = "FAILED",
                    errorDetail = "Network error: ${e.localizedMessage ?: "Could not reach api.smsonlinegh.com"}"
                )
            }
        }

        broadcastId
    }

    /**
     * Dispatches SMS messages directly via Android SmsManager.
     */
    suspend fun sendViaDirectSim(
        senderId: String,
        candidateLabel: String,
        messageTemplate: String,
        recipients: List<ContactEntity>
    ): Long = withContext(Dispatchers.IO) {
        ensureReceiversRegistered()

        if (!hasSendSmsPermission()) {
            val (broadcastId, _) = repository.createBroadcastWithRecipients(
                senderIdUsed = senderId,
                candidateLabel = candidateLabel,
                messageText = messageTemplate,
                recipients = recipients,
                sendMode = "DIRECT_SIM",
                initialOverallStatus = "FAILED",
                initialStatusDetail = "Failed: SEND_SMS permission was not granted.",
                initialRecipientStatus = "FAILED"
            )
            return@withContext broadcastId
        }

        val simReady = isSimReady()
        val smsManager: SmsManager? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (_: Exception) {
            null
        }

        if (smsManager == null) {
            val (broadcastId, deliveries) = repository.createBroadcastWithRecipients(
                senderIdUsed = senderId,
                candidateLabel = candidateLabel,
                messageText = messageTemplate,
                recipients = recipients,
                sendMode = "DIRECT_SIM",
                initialOverallStatus = "FAILED",
                initialStatusDetail = "Failed: Device does not support cellular SMS.",
                initialRecipientStatus = "FAILED"
            )
            deliveries.forEach {
                repository.updateRecipientStatusAndRefreshBroadcast(
                    it.id,
                    "FAILED",
                    "No cellular SMS hardware available"
                )
            }
            return@withContext broadcastId
        }

        val (broadcastId, deliveries) = repository.createBroadcastWithRecipients(
            senderIdUsed = senderId,
            candidateLabel = candidateLabel,
            messageText = messageTemplate,
            recipients = recipients,
            sendMode = "DIRECT_SIM",
            initialOverallStatus = "SENDING",
            initialStatusDetail = "Dispatching ${recipients.size} SMS message(s) via SIM...",
            initialRecipientStatus = "SENDING"
        )

        val immutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        deliveries.forEach { delivery ->
            try {
                val personalizedMessage = personalizeMessage(messageTemplate, delivery.contactName)

                val sentIntent = Intent(ACTION_SMS_SENT).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_DELIVERY_ID, delivery.id)
                }
                val deliveredIntent = Intent(ACTION_SMS_DELIVERED).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_DELIVERY_ID, delivery.id)
                }

                val sentPi = PendingIntent.getBroadcast(
                    context,
                    (delivery.id * 2).toInt(),
                    sentIntent,
                    immutableFlag
                )
                val deliveredPi = PendingIntent.getBroadcast(
                    context,
                    (delivery.id * 2 + 1).toInt(),
                    deliveredIntent,
                    immutableFlag
                )

                val parts = smsManager.divideMessage(personalizedMessage)
                if (parts.size <= 1) {
                    smsManager.sendTextMessage(
                        delivery.phoneNumber,
                        null,
                        personalizedMessage,
                        sentPi,
                        deliveredPi
                    )
                } else {
                    val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                        repeat(parts.size) { add(sentPi) }
                    }
                    val deliveredIntents = ArrayList<PendingIntent>(parts.size).apply {
                        repeat(parts.size) { add(deliveredPi) }
                    }
                    smsManager.sendMultipartTextMessage(
                        delivery.phoneNumber,
                        null,
                        parts,
                        sentIntents,
                        deliveredIntents
                    )
                }
            } catch (e: Exception) {
                repository.updateRecipientStatusAndRefreshBroadcast(
                    deliveryId = delivery.id,
                    newStatus = "FAILED",
                    errorDetail = e.localizedMessage ?: "Failed to queue SMS on SIM"
                )
            }
        }

        serviceScope.launch {
            delay(4000)
            if (!simReady) {
                deliveries.forEach { d ->
                    repository.updateRecipientStatusAndRefreshBroadcast(
                        deliveryId = d.id,
                        newStatus = "FAILED",
                        errorDetail = "No active SIM card detected. Use SMSOnlineGH or Device SMS App."
                    )
                }
            }
        }

        broadcastId
    }

    /**
     * Opens the device's default SMS application pre-populated with all selected recipient numbers
     * and the campaign message.
     */
    suspend fun sendViaSystemSmsApp(
        activityContext: Context,
        senderId: String,
        candidateLabel: String,
        messageTemplate: String,
        recipients: List<ContactEntity>
    ): Pair<Long, Boolean> {
        val (broadcastId, deliveries) = withContext(Dispatchers.IO) {
            repository.createBroadcastWithRecipients(
                senderIdUsed = senderId,
                candidateLabel = candidateLabel,
                messageText = messageTemplate,
                recipients = recipients,
                sendMode = "SMS_APP",
                initialOverallStatus = "OPENED_IN_SMS_APP",
                initialStatusDetail = "Opened in device SMS app (${recipients.size} recipient(s)). Tap send in your SMS app to complete.",
                initialRecipientStatus = "OPENED_IN_APP"
            )
        }

        val separator = if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) "," else ";"
        val numbersJoined = recipients.joinToString(separator) { it.phoneNumber }
        val resolvedBody = if (recipients.size == 1) {
            personalizeMessage(messageTemplate, recipients.first().name)
        } else {
            messageTemplate.replace("{name}", "Delegate", ignoreCase = true)
        }

        val launched = try {
            val smsUri = Uri.parse("smsto:${Uri.encode(numbersJoined)}")
            val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", resolvedBody)
                putExtra(Intent.EXTRA_TEXT, resolvedBody)
                if (activityContext !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            activityContext.startActivity(intent)
            true
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                deliveries.forEach { d ->
                    repository.updateRecipientStatusAndRefreshBroadcast(
                        deliveryId = d.id,
                        newStatus = "FAILED",
                        errorDetail = "No default SMS app found on device"
                    )
                }
            }
            false
        }

        return broadcastId to launched
    }

    private fun formatGhanaOrIntlPhone(raw: String): String {
        val digitsOnly = raw.filter { it.isDigit() }
        return if (digitsOnly.length == 10 && digitsOnly.startsWith("0")) {
            "233" + digitsOnly.substring(1)
        } else {
            digitsOnly
        }
    }

    private fun personalizeMessage(template: String, contactName: String): String {
        val cleanName = contactName.trim().ifEmpty { "Delegate" }
        return template
            .replace("{name}", cleanName, ignoreCase = true)
            .replace("{\$name}", cleanName, ignoreCase = true)
    }
}
