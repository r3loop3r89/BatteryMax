package com.example.batterymax

import android.app.Application
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.AppDatabase

class BatteryMaxApp : Application() {

    lateinit var repository: BatteryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = BatteryRepository(db.batterySampleDao(), db.trackedDeviceDao())
    }
}
