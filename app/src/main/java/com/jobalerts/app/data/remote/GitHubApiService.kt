package com.jobalerts.app.data.remote
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url
interface GitHubApiService {
@GET
suspend fun getJobsIndex(@Url url: String): ResponseBody
@GET
suspend fun getRawMarkdown(@Url url: String): ResponseBody
}