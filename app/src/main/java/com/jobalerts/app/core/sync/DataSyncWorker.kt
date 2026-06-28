package com.jobalerts.app.core.sync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jobalerts.app.core.config.AppConfigManager
import com.jobalerts.app.core.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import org.json.JSONArray

private val Context.syncDataStore by preferencesDataStore(name = "sync_state")
private val KEY_KNOWN_JOB_IDS = stringPreferencesKey("known_job_ids")

/**
 * Periodic background sync worker.
 *
 * Pipeline:
 *   1. Fetch the app config (backend base URL + endpoint).
 *   2. Fetch the latest jobs index JSON from GitHub raw.
 *   3. Compare against the set of previously-seen job IDs in DataStore.
 *   4. For each new job that matches the user's qualification preferences
 *      (stored in SharedPreferences `jobalerts_notif`), increment the count.
 *   5. If count > 0, post a [NotificationHelper.showNewJobsNotification].
 *   6. Persist the new set of job IDs back to DataStore.
 *
 * Returns:
 *   - [Result.success] — sync completed (with or without new jobs).
 *   - [Result.retry]   — transient failure (network, parse) — WorkManager will
 *                       retry with exponential backoff.
 */
class DataSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("DataSyncWorker", "Background sync starting…")

            val config = AppConfigManager.fetchConfig()
                ?: return Result.retry()

            val indexUrl = config.apiBaseUrl + config.jobsEndpoint
            val indexJson = java.net.URL(indexUrl).readText()
            val array = JSONArray(indexJson)

            // Load previously known IDs
            val prefs = appContext.syncDataStore.data.first()
            val knownIds = prefs[KEY_KNOWN_JOB_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()

            // Load notification preferences
            val notifPrefs = appContext.getSharedPreferences(
                "jobalerts_notif",
                Context.MODE_PRIVATE
            )
            val notif10th = notifPrefs.getBoolean("notif_10th", true)
            val notif12th = notifPrefs.getBoolean("notif_12th", true)
            val notifGrad = notifPrefs.getBoolean("notif_grad", true)
            val notifEngg = notifPrefs.getBoolean("notif_engg", false)

            val currentIds = mutableSetOf<String>()
            var newMatchingCount = 0

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val jobId = obj.getString("id")
                val qual = obj.optString("qualification_tag", "Any")
                currentIds.add(jobId)

                if (jobId !in knownIds) {
                    val relevant = when {
                        qual.contains("10th", ignoreCase = true) -> notif10th
                        qual.contains("12th", ignoreCase = true) -> notif12th
                        qual.contains("Grad", ignoreCase = true) ||
                        qual.contains("Bachelor", ignoreCase = true) -> notifGrad
                        qual.contains("Engineer", ignoreCase = true) ||
                        qual.contains("ITI", ignoreCase = true) -> notifEngg
                        else -> notif10th || notif12th || notifGrad
                    }
                    if (relevant) newMatchingCount++
                }
            }

            if (newMatchingCount > 0) {
                Log.d(
                    "DataSyncWorker",
                    "$newMatchingCount new matching jobs — sending notification"
                )
                NotificationHelper.showNewJobsNotification(
                    context = appContext,
                    count = newMatchingCount,
                    qualificationLabel = "Government"
                )
            }

            // Persist current IDs
            appContext.syncDataStore.edit {
                it[KEY_KNOWN_JOB_IDS] = currentIds.joinToString(",")
            }

            Log.d(
                "DataSyncWorker",
                "Sync done. Total: ${currentIds.size}, New: $newMatchingCount"
            )
            Result.success()
        } catch (e: Exception) {
            Log.e("DataSyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}
