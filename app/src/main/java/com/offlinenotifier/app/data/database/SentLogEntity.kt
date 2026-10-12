package com.offlinenotifier.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sent_logs")
data class SentLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timeSent: Long,
    val eventCount: Int,
    val status: String, // "SUCCESS", "FAILED"
    val eventSummary: String = "", // "📞2 📬3 🔔1" breakdown of event types
    val errorMessage: String? = null // error description if status is FAILED
)
