package com.example.ui.components

import com.example.data.BroadcastCounts
import com.example.data.DeliveryStatus
import com.example.data.RecipientDeliveryEntity
import com.example.data.SendMode

/**
 * One-glance result of a message, so users never have to add up statuses themselves.
 * "Sent" counts both "left the phone / accepted by the gateway" and "delivery confirmed".
 */
data class Outcome(
    val sent: Int,
    val failed: Int,
    val pending: Int,
    val total: Int
) {
    val isSending: Boolean get() = pending > 0
    val hasFailures: Boolean get() = failed > 0 && pending == 0
    val progress: Float get() = if (total == 0) 0f else (sent + failed).toFloat() / total

    /** The single sentence shown for a message, e.g. "42 of 45 sent". */
    val headline: String
        get() = when {
            total == 0 -> "No recipients"
            isSending -> "Sending… ${sent + failed} of $total done"
            failed == 0 -> if (total == 1) "Sent" else "Sent to all $total"
            sent == 0 -> if (total == 1) "Not sent" else "None of $total sent"
            else -> "$sent of $total sent"
        }

    val tone: StatusTone
        get() = when {
            isSending -> StatusTone.PROGRESS
            failed == 0 -> StatusTone.SUCCESS
            sent == 0 -> StatusTone.ERROR
            else -> StatusTone.WARNING
        }
}

fun outcomeOf(counts: BroadcastCounts?, totalFallback: Int, sendMode: String): Outcome? {
    if (sendMode == SendMode.SMS_APP) return null
    if (counts == null) return Outcome(0, 0, 0, totalFallback)
    return Outcome(
        sent = counts.sent + counts.delivered,
        failed = counts.failed,
        pending = counts.pending,
        total = counts.total
    )
}

data class FailureReason(val reason: String, val count: Int)

/**
 * Groups failed recipients by why they failed, most common first, so "45 failures" becomes
 * "No signal · 40" and "Number not valid · 5" — two problems instead of 45 rows.
 */
fun groupFailureReasons(deliveries: List<RecipientDeliveryEntity>): List<FailureReason> =
    deliveries
        .filter { it.status == DeliveryStatus.FAILED }
        .groupingBy { it.errorDetail?.takeIf(String::isNotBlank) ?: "The network didn't say why." }
        .eachCount()
        .map { (reason, count) -> FailureReason(reason, count) }
        .sortedByDescending { it.count }
