package com.jobalerts.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jobalerts.app.data.local.dao.ReminderDao

/**
 * Room database for JobAlerts.
 *
 * Holds [ReminderEntity] rows — one per active deadline reminder. Persisted
 * across app restarts and re-armed on device reboot by
 * [com.jobalerts.app.core.reminders.BootReceiver].
 *
 * Use [getInstance] (singleton) — never construct the database inline.
 */
@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jobalerts.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
