package com.example.batterymax.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedDeviceDao {

    @Upsert
    suspend fun upsert(device: TrackedDeviceEntity)

    @Query("DELETE FROM tracked_devices")
    suspend fun clear()

    @Query("SELECT * FROM tracked_devices WHERE enabled = 1 LIMIT 1")
    fun observeTracked(): Flow<TrackedDeviceEntity?>

    @Query("SELECT * FROM tracked_devices WHERE enabled = 1 LIMIT 1")
    suspend fun tracked(): TrackedDeviceEntity?
}
