package com.offlinenotifier.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CALL", "SMS", "APP"
    val title: String, // Caller Name, Sender, App Name
    val body: String, // Message content (if available)
    val timestamp: Long, // System time in millis
    val imagePath: String? = null, // Path to the saved notification thumbnail image
    val audioPath: String? = null // Path to the saved notification audio file
)
