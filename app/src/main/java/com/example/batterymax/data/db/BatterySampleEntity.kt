package com.example.batterymax.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single battery reading. [source] is [SOURCE_PHONE] for the phone battery,
 * or the Bluetooth MAC address of a tracked device.
 */
@Entity(
    tableName = "battery_samples",
    indices = [Index(value = ["source", "timestamp"])]
)
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val levelPercent: Int,
    val isCharging: Boolean,
    /** Battery temperature in degrees Celsius; null for Bluetooth devices. */
    val temperatureC: Float? = null,
    /** Battery voltage in millivolts; null for Bluetooth devices. */
    val voltageMv: Int? = null,
    val source: String
) {
    companion object {
        const val SOURCE_PHONE = "phone"
    }
}
