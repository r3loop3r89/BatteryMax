package com.example.batterymax.data

import android.content.Context
import android.text.format.DateFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _use24HourClock = MutableStateFlow(
        prefs.getBoolean(KEY_USE_24_HOUR, DateFormat.is24HourFormat(appContext))
    )
    val use24HourClock: StateFlow<Boolean> = _use24HourClock.asStateFlow()

    fun setUse24HourClock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_24_HOUR, enabled).apply()
        _use24HourClock.value = enabled
    }

    companion object {
        private const val PREFS = "app_preferences"
        private const val KEY_USE_24_HOUR = "use_24_hour_clock"
    }
}
