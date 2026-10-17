package com.example.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.example.AppGraph
import com.example.data.DeliveryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives "sent" and "delivered" reports for SIM messages. Declared in the manifest so
 * reports are recorded even if the app was closed after sending.
 */
class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val deliveryId = intent.getLongExtra(EXTRA_DELIVERY_ID, -1L)
        if (deliveryId <= 0L) return
        val code = resultCode
        val action = intent.action
        val repository = AppGraph.repository(context)
        val pending = goAsync()

        scope.launch {
            try {
                when (action) {
                    ACTION_SMS_SENT -> if (code == Activity.RESULT_OK) {
                        repository.updateRecipientStatusAndRefreshBroadcast(deliveryId, DeliveryStatus.SENT)
                    } else {
                        repository.updateRecipientStatusAndRefreshBroadcast(
                            deliveryId,
                            DeliveryStatus.FAILED,
                            describeSendError(code)
                        )
                    }
                    ACTION_SMS_DELIVERED -> if (code == Activity.RESULT_OK) {
                        repository.updateRecipientStatusAndRefreshBroadcast(deliveryId, DeliveryStatus.DELIVERED)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SMS_SENT = "com.example.sms.ACTION_SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.sms.ACTION_SMS_DELIVERED"
        const val EXTRA_DELIVERY_ID = "extra_delivery_id"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun describeSendError(code: Int): String = when (code) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Network refused the SMS. Check your airtime or SMS bundle."
            SmsManager.RESULT_ERROR_NO_SERVICE -> "No network signal."
            SmsManager.RESULT_ERROR_NULL_PDU -> "The message could not be prepared."
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Airplane mode is on or the SIM is switched off."
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Your phone limited how many SMS can be sent. Wait a while, then retry."
            SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> "Sending to short codes is not allowed."
            else -> "The SMS could not be sent (code $code)."
        }
    }
}
