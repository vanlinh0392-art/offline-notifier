package com.offlinenotifier.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Query("SELECT * FROM offline_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM offline_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<EventEntity>>

    @Query("DELETE FROM offline_events WHERE id IN (:idList)")
    suspend fun deleteEvents(idList: List<Int>)
    
    @Query("DELETE FROM offline_events")
    suspend fun clearAll()
}
