package com.jobalerts.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jobalerts.app.JobAlertsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-schedules all deadline alarms after device reboot.
 *
 * Android clears all alarms set via [android.app.AlarmManager] on reboot, so we
 * read every persisted reminder from Room and re-arm each one through
 * [ReminderScheduler].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Device rebooted — rescheduling all alarms")

        val app = context.applicationContext as JobAlertsApp
        val db = app.container.database
        val scheduler = ReminderScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.reminderDao().getAllReminders().first().forEach { entity ->
                    scheduler.scheduleReminder(
                        jobId = entity.jobId,
                        jobTitle = entity.jobTitle,
                        lastDate = entity.lastDate
                    )
                    Log.d("BootReceiver", "Rescheduled: ${entity.jobTitle}")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule alarms", e)
            }
        }
    }
}
