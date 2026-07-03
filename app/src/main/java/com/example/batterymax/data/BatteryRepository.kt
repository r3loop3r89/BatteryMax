package com.example.batterymax.data

import com.example.batterymax.data.db.BatterySampleDao
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.data.db.TrackedDeviceDao
import com.example.batterymax.data.db.TrackedDeviceEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class BatteryRepository(
    private val sampleDao: BatterySampleDao,
    private val deviceDao: TrackedDeviceDao
) {
    /** Last inserted (level, timestamp) per source, used by the sampling policy. */
    private val lastInserted = mutableMapOf<String, Pair<Int, Long>>()

    val phoneLatest: Flow<BatterySampleEntity?> =
        sampleDao.observeLatest(BatterySampleEntity.SOURCE_PHONE)

    fun latestFor(source: String): Flow<BatterySampleEntity?> = sampleDao.observeLatest(source)

    val trackedDevices: Flow<List<TrackedDeviceEntity>> = deviceDao.observeAllTracked()

    suspend fun trackedDevicesNow(): List<TrackedDeviceEntity> = deviceDao.allTracked()

    fun samplesBetween(startMillis: Long, endMillis: Long): Flow<List<BatterySampleEntity>> =
        sampleDao.observeBetween(startMillis, endMillis)

    /**
     * Inserts a sample only if the level changed or [MIN_SAMPLE_INTERVAL_MS] elapsed
     * since the last stored sample for this source. Returns true if inserted.
     */
    suspend fun recordSample(sample: BatterySampleEntity): Boolean {
        val last = lastInserted[sample.source] ?: sampleDao.latest(sample.source)
            ?.let { it.levelPercent to it.timestamp }
        if (last != null) {
            val (lastLevel, lastTime) = last
            if (lastLevel == sample.levelPercent &&
                sample.timestamp - lastTime < MIN_SAMPLE_INTERVAL_MS
            ) {
                return false
            }
        }
        sampleDao.insert(sample)
        lastInserted[sample.source] = sample.levelPercent to sample.timestamp
        return true
    }

    suspend fun pruneOldSamples(now: Long = System.currentTimeMillis()) {
        sampleDao.deleteOlderThan(now - RETENTION_MS)
    }

    /** Adds or updates a tracked Bluetooth device (does not remove others). */
    suspend fun addTrackedDevice(address: String, name: String) {
        deviceDao.upsert(TrackedDeviceEntity(address = address, name = name))
    }

    suspend fun removeTrackedDevice(address: String) {
        deviceDao.delete(address)
    }

    companion object {
        val MIN_SAMPLE_INTERVAL_MS: Long = TimeUnit.MINUTES.toMillis(5)
        val RETENTION_MS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
