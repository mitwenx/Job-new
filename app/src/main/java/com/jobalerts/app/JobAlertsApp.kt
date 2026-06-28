package com.jobalerts.app

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jobalerts.app.core.di.AppContainer
import com.jobalerts.app.core.notifications.NotificationHelper
import com.jobalerts.app.core.sync.DataSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Application entry point.
 *
 * - Builds the [AppContainer] (manual DI graph).
 * - Creates notification channels up-front so they exist before any worker fires.
 * - Schedules a periodic [DataSyncWorker] that polls the GitHub backend every
 *   ~30 minutes (15-minute flex) and posts a notification for new matching jobs.
 */
class JobAlertsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DataSyncWorker>(
            30, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "JOB_DATA_SYNC",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
