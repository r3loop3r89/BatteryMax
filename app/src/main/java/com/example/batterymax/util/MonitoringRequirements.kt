package com.example.batterymax.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

data class MonitoringRequirements(
    val notificationsGranted: Boolean,
    val notificationsRequired: Boolean,
    val bluetoothGranted: Boolean,
    val bluetoothRequired: Boolean,
    val batteryOptimizationDisabled: Boolean
) {
    val isReady: Boolean
        get() = (!notificationsRequired || notificationsGranted) &&
            (!bluetoothRequired || bluetoothGranted) &&
            batteryOptimizationDisabled

    fun missingLabels(): List<String> = buildList {
        if (notificationsRequired && !notificationsGranted) add("Notifications")
        if (bluetoothRequired && !bluetoothGranted) add("Bluetooth")
        if (!batteryOptimizationDisabled) add("Battery optimization")
    }

    fun settingsSnackbarMessage(): String {
        val missing = missingLabels()
        return if (missing.isEmpty()) {
            "Go to Settings to allow all permissions before starting monitoring"
        } else {
            "Go to Settings to allow ${missing.joinToString(", ")}"
        }
    }
}

fun checkMonitoringRequirements(context: Context): MonitoringRequirements {
    val notificationsRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val notificationsGranted = if (notificationsRequired) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val bluetoothRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val bluetoothGranted = if (bluetoothRequired) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    return MonitoringRequirements(
        notificationsGranted = notificationsGranted,
        notificationsRequired = notificationsRequired,
        bluetoothGranted = bluetoothGranted,
        bluetoothRequired = bluetoothRequired,
        batteryOptimizationDisabled = isBatteryOptimizationDisabled(context)
    )
}

fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}