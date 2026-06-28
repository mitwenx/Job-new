package com.jobalerts.app.domain.repository
import com.jobalerts.app.domain.models.JobPost
import kotlinx.coroutines.flow.Flow
interface JobRepository {
suspend fun syncLatestJobs()
fun getJobs(): Flow<List<JobPost>>
suspend fun getJobById(jobId: String): JobPost?
}