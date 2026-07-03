package com.example.batterymax

import android.app.Application
import com.example.batterymax.data.AppPreferences
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.AppDatabase

class BatteryMaxApp : Application() {

    lateinit var repository: BatteryRepository
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        val db = AppDatabase.get(this)
        repository = BatteryRepository(db.batterySampleDao(), db.trackedDeviceDao())
    }
}
