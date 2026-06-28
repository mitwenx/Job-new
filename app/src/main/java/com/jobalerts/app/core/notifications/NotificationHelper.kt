package com.jobalerts.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jobalerts.app.MainActivity
import com.jobalerts.app.R

/**
 * Centralised helper for posting system notifications.
 *
 * - Creates the two channels the app uses (deadline reminders + new-job alerts).
 * - Builds real [NotificationCompat] notifications (NOT Toasts).
 * - Tapping a deadline notification deep-links into the corresponding job via
 *   the `navigate_to_job` intent extra (consumed in [com.jobalerts.app.MainActivity]).
 */
object NotificationHelper {

    const val CHANNEL_DEADLINE = "deadline_reminders"
    const val CHANNEL_NEW_JOBS = "new_job_alerts"

    /** Call once from [com.jobalerts.app.JobAlertsApp.onCreate]. Safe to call multiple times. */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEADLINE,
                "Deadline Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you 24 hours before an application deadline closes"
                enableVibration(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW_JOBS,
                "New Job Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when new matching job listings are available"
                enableVibration(false)
            }
        )
    }

    /** Post a high-priority deadline notification for a single job. */
    fun showDeadlineNotification(
        context: Context,
        jobId: String,
        jobTitle: String,
        lastDate: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_job", jobId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            jobId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Deadline Tomorrow")
            .setContentText("Last date for \"$jobTitle\" is $lastDate")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Last date for \"$jobTitle\" is $lastDate. Don't forget to apply before the deadline closes!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(jobId.hashCode(), notification)
    }

    /** Post a notification announcing [count] new matching jobs. */
    fun showNewJobsNotification(context: Context, count: Int, qualificationLabel: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_JOBS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$count New Job${if (count > 1) "s" else ""} Available")
            .setContentText("New $qualificationLabel listings posted. Tap to view.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1001, notification)
    }
}
