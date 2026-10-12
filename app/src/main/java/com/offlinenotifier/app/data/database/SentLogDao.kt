package com.offlinenotifier.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SentLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SentLogEntity)
    
    @Query("SELECT * FROM sent_logs ORDER BY timeSent DESC")
    fun getAllLogsFlow(): Flow<List<SentLogEntity>>

    @Query("DELETE FROM sent_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM sent_logs WHERE id NOT IN (SELECT id FROM sent_logs ORDER BY timeSent DESC LIMIT :maxLogs)")
    suspend fun trimOldLogs(maxLogs: Int = 100)
}
