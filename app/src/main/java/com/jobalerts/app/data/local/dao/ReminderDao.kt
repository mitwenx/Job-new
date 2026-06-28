package com.jobalerts.app.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jobalerts.app.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ReminderDao {
@Query("SELECT * FROM reminders ORDER BY scheduledTimeMs ASC")
fun getAllReminders(): Flow<List<ReminderEntity>>
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertReminder(reminder: ReminderEntity)
@Query("DELETE FROM reminders WHERE jobId = :jobId")
suspend fun deleteReminder(jobId: String)
}