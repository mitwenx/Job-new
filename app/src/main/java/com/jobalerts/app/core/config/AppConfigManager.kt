package com.jobalerts.app.core.config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
data class AppConfig(val apiBaseUrl: String, val jobsEndpoint: String)
object AppConfigManager {
// Pointed to your actual GitHub Repository
private const val CONFIG_URL = "https://raw.githubusercontent.com/mitwenx/Job-data/main/data_config.json"
suspend fun fetchConfig(): AppConfig? = withContext(Dispatchers.IO) {
try {
val response = URL(CONFIG_URL).readText()
val json = JSONObject(response)
AppConfig(
apiBaseUrl = json.getString("api_base_url"),
jobsEndpoint = json.getString("jobs_endpoint")
)
} catch (e: Exception) {
null
}
}
}