package com.jobalerts.app.data.repository

import android.util.Log
import com.jobalerts.app.core.config.AppConfigManager
import com.jobalerts.app.core.parser.JobMarkdownParser
import com.jobalerts.app.data.local.dao.ReminderDao
import com.jobalerts.app.data.remote.GitHubApiService
import com.jobalerts.app.domain.models.JobPost
import com.jobalerts.app.domain.repository.JobRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Concrete repository that fetches the job index + per-job markdown from the
 * GitHub backend, parses them into [JobPost]s, and exposes them as a Flow.
 *
 * Dependencies (apiService + reminderDao) are injected via constructor so the
 * class is testable and so we don't construct a fresh Retrofit on every
 * instance (fixes Bug 6 in the upgrade prompt).
 *
 * Per-job markdown fetch errors are swallowed gracefully — one bad file
 * should never break the whole list.
 */
class JobRepositoryImpl(
    private val apiService: GitHubApiService,
    private val reminderDao: ReminderDao
) : JobRepository {

    private val _jobsFlow = MutableStateFlow<List<JobPost>>(emptyList())

    override suspend fun syncLatestJobs() {
        withContext(Dispatchers.IO) {
            val config = AppConfigManager.fetchConfig()
                ?: throw IllegalStateException("Could not fetch app config. Check network.")

            val indexUrl = config.apiBaseUrl + config.jobsEndpoint
            val indexJson = apiService.getJobsIndex(indexUrl).string()
            val jsonArray = JSONArray(indexJson)

            val parsedJobs = mutableListOf<JobPost>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val jobId = obj.getString("id")
                val fileName = obj.getString("file")
                val category = obj.optString("category", "UPDATE")
                val title = obj.optString("title", "Job Notification")
                val qualTag = obj.optString("qualification_tag", "Any")
                val lastDate = obj.optString("last_date", "TBD")

                try {
                    val fileUrl = config.apiBaseUrl + "jobs/$fileName"
                    val markdown = apiService.getRawMarkdown(fileUrl).string()
                    parsedJobs.add(
                        JobMarkdownParser.parse(
                            markdown, jobId, category, title, qualTag, lastDate
                        )
                    )
                } catch (e: Exception) {
                    Log.w("JobRepo", "Skipping $jobId — ${e.message}")
                }
            }

            _jobsFlow.value = parsedJobs
            Log.d("JobRepo", "Sync done: ${parsedJobs.size} jobs")
        }
    }

    override fun getJobs(): Flow<List<JobPost>> = _jobsFlow.asStateFlow()

    override suspend fun getJobById(id: String): JobPost? =
        _jobsFlow.value.find { it.id == id }
}
