package com.jobalerts.app.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "reminders")
data class ReminderEntity(
@PrimaryKey val jobId: String,
val jobTitle: String,
val lastDate: String,
val scheduledTimeMs: Long
)