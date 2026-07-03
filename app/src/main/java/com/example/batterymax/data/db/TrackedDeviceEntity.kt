package com.example.batterymax.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The Bluetooth device the user selected for battery tracking. */
@Entity(tableName = "tracked_devices")
data class TrackedDeviceEntity(
    @PrimaryKey val address: String,
    val name: String,
    val enabled: Boolean = true
)
