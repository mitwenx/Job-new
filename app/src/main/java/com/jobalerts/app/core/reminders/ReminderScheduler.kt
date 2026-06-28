package com.jobalerts.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Schedules / cancels the system alarm that fires 24 hours before a job's last
 * date.
 *
 * Supports multiple common date formats seen in the backend data, so a typo in
 * a single job file (e.g. "April 15 2026" vs "15 April 2026") doesn't silently
 * skip the reminder.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val dateFormats = listOf(
        "dd MMMM yyyy", "d MMMM yyyy",
        "dd MMM yyyy", "d MMM yyyy",
        "dd/MM/yyyy", "yyyy-MM-dd"
    )

    /** Schedule an alarm 24h before [lastDate]. No-op if date can't be parsed or already passed. */
    fun scheduleReminder(jobId: String, jobTitle: String, lastDate: String) {
        val deadlineMs = parseDate(lastDate) ?: run {
            Log.w("ReminderScheduler", "Cannot parse date '$lastDate' for $jobId")
            return
        }

        val alarmMs = deadlineMs - (24 * 60 * 60 * 1000L)
        if (alarmMs <= System.currentTimeMillis()) {
            Log.w("ReminderScheduler", "Deadline already passed for $jobId")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("JOB_ID", jobId)
            putExtra("JOB_TITLE", jobTitle)
            putExtra("LAST_DATE", lastDate)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            jobId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmMs,
            pending
        )
        Log.d("ReminderScheduler", "Alarm set for $jobId at $alarmMs")
    }

    /** Cancel a previously-scheduled alarm for [jobId]. Safe to call if none exists. */
    fun cancelReminder(jobId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            jobId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        Log.d("ReminderScheduler", "Alarm cancelled for $jobId")
    }

    private fun parseDate(raw: String): Long? {
        for (fmt in dateFormats) {
            try {
                return SimpleDateFormat(fmt, Locale.ENGLISH).parse(raw)?.time
            } catch (_: Exception) {
                // try next format
            }
        }
        return null
    }
}
