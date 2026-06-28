package com.jobalerts.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jobalerts.app.core.notifications.NotificationHelper

/**
 * Receives the deadline alarm fired by [ReminderScheduler] 24 hours before a
 * job's last date.
 *
 * Shows a real system notification via [NotificationHelper] — never a Toast.
 * Toasts are unreliable from BroadcastReceiver contexts and invisible while the
 * screen is off.
 *
 * Intent extras consumed:
 *   - `JOB_ID`     — the job's identifier (used as notification ID + deep-link)
 *   - `JOB_TITLE`  — human-readable job title
 *   - `LAST_DATE`  — formatted last date string
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getStringExtra("JOB_ID") ?: return
        val jobTitle = intent.getStringExtra("JOB_TITLE") ?: "Job Application"
        val lastDate = intent.getStringExtra("LAST_DATE") ?: ""

        Log.d("AlarmReceiver", "Deadline alarm fired: $jobTitle ($jobId)")

        NotificationHelper.showDeadlineNotification(
            context = context,
            jobId = jobId,
            jobTitle = jobTitle,
            lastDate = lastDate
        )
    }
}
