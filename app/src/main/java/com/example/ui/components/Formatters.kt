package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun peopleCount(count: Int): String = if (count == 1) "1 person" else "$count people"

/** Section title for grouping lists by day: "Today", "Yesterday", "This week", "March 2026". */
fun dateSection(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val sameYear = then.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    val dayDiff = today.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        sameYear && dayDiff in 2..6 -> "This week"
        else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/** "Today, 3:45 PM", "Yesterday, 9:02 AM", or "12 Mar, 4:10 PM". */
fun formatWhen(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val sameYear = then.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    val dayDiff = today.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "Today, $time"
        sameYear && dayDiff == 1 -> "Yesterday, $time"
        sameYear -> SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
