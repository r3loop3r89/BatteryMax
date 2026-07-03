package com.example.batterymax.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedDeviceDao {

    @Upsert
    suspend fun upsert(device: TrackedDeviceEntity)

    @Query("DELETE FROM tracked_devices WHERE address = :address")
    suspend fun delete(address: String)

    @Query("DELETE FROM tracked_devices")
    suspend fun clear()

    @Query("SELECT * FROM tracked_devices WHERE enabled = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTracked(): Flow<List<TrackedDeviceEntity>>

    @Query("SELECT * FROM tracked_devices WHERE enabled = 1 ORDER BY name COLLATE NOCASE ASC")
    suspend fun allTracked(): List<TrackedDeviceEntity>
}
