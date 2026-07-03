package com.example.batterymax.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormats {

    fun formatTime(millis: Long, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }

    /** Formats an hour-of-day fraction (0..24) for graph axes and markers. */
    fun formatHourOfDay(hourFraction: Double, use24Hour: Boolean): String {
        val totalMinutes = (hourFraction * 60).toInt().coerceIn(0, 23 * 60 + 59)
        val hours24 = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (use24Hour) {
            "%02d:%02d".format(hours24, minutes)
        } else {
            val amPm = if (hours24 < 12) "AM" else "PM"
            val hours12 = when (val hour = hours24 % 12) {
                0 -> 12
                else -> hour
            }
            "%d:%02d %s".format(hours12, minutes, amPm)
        }
    }
}
