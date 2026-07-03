package com.example.batterymax.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySampleDao {

    @Insert
    suspend fun insert(sample: BatterySampleEntity)

    @Query("SELECT * FROM battery_samples WHERE source = :source ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(source: String): Flow<BatterySampleEntity?>

    @Query("SELECT * FROM battery_samples WHERE source = :source ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(source: String): BatterySampleEntity?

    @Query(
        "SELECT * FROM battery_samples WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp ASC"
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<BatterySampleEntity>>

    @Query("DELETE FROM battery_samples WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
