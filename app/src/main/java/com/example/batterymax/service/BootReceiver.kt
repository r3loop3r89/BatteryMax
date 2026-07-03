package com.example.batterymax.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts battery monitoring after a reboot if the user had it enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            BatteryMonitorService.isEnabled(context)
        ) {
            BatteryMonitorService.start(context)
        }
    }
}
