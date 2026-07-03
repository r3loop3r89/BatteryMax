package com.example.batterymax.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A [start, end) millisecond range covering one local calendar day. */
data class DayRange(val startMillis: Long, val endMillis: Long) {

    val isToday: Boolean
        get() = today().startMillis == startMillis

    fun previous(): DayRange = shift(-1)

    fun next(): DayRange = shift(1)

    private fun shift(days: Int): DayRange {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return of(cal)
    }

    fun label(): String {
        val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return format.format(Date(startMillis))
    }

    companion object {
        fun today(): DayRange = of(Calendar.getInstance())

        private fun of(cal: Calendar): DayRange {
            val start = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            return DayRange(start.timeInMillis, end.timeInMillis)
        }
    }
}
