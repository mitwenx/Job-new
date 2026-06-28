package com.jobalerts.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jobalerts.app.presentation.theme.ColorInfo
import com.jobalerts.app.presentation.theme.ColorSuccess
import com.jobalerts.app.presentation.theme.ColorUrgent
import com.jobalerts.app.presentation.theme.LocalThemeState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ════════════════════════════════════════════════════════════════════════════
// APP LOGGER — in-memory log ring buffer (max 300 entries)
// ════════════════════════════════════════════════════════════════════════════
enum class LogLevel { INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Long,
    val tag: String,
    val message: String,
    val level: LogLevel
)

object AppLogger {
    private val _logs = mutableStateListOf<LogEntry>()
    val logs: List<LogEntry> get() = _logs

    fun info(tag: String, message: String) = add(tag, message, LogLevel.INFO)
    fun warn(tag: String, message: String) = add(tag, message, LogLevel.WARN)
    fun error(tag: String, message: String) = add(tag, message, LogLevel.ERROR)

    private fun add(tag: String, message: String, level: LogLevel) {
        _logs.add(0, LogEntry(System.currentTimeMillis(), tag, message, level))
        if (_logs.size > 300) _logs.removeAt(_logs.size - 1)
    }

    fun clear() { _logs.clear() }
}

// ════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Notification Prefs (persisted to SharedPreferences)
    val prefs = remember { context.getSharedPreferences("jobalerts_notif", Context.MODE_PRIVATE) }
    var notif10th by remember { mutableStateOf(prefs.getBoolean("notif_10th", true)) }
    var notif12th by remember { mutableStateOf(prefs.getBoolean("notif_12th", true)) }
    var notifGrad by remember { mutableStateOf(prefs.getBoolean("notif_grad", true)) }
    var notifEngg by remember { mutableStateOf(prefs.getBoolean("notif_engg", false)) }

    fun saveNotifPref(key: String, value: Boolean, label: String) {
        prefs.edit().putBoolean(key, value).apply()
        AppLogger.info("Settings", "Pref saved: $key = $value")
        scope.launch {
            snackbar.showSnackbar("$label alerts ${if (value) "enabled" else "disabled"}")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(30.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            // ── Scrollable Body ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ─── APP PREFERENCES ─────────────────────────────────
                SettingsHeader("APP PREFERENCES")
                SettingsSwitchRow(
                    title = "Dark Mode",
                    subtitle = "Toggle true black OLED theme",
                    checked = themeState.isDark,
                    onChecked = { themeState.toggle(it) }
                )

                // ─── ALERTS & DATA ──────────────────────────────────
                SettingsHeader("ALERTS & DATA")
                SettingsSwitchRow(
                    title = "10th Pass Jobs",
                    subtitle = "Notifications for 10th/Matriculation jobs",
                    checked = notif10th,
                    onChecked = { notif10th = it; saveNotifPref("notif_10th", it, "10th Pass") }
                )
                SettingsSwitchRow(
                    title = "12th Pass Jobs",
                    subtitle = "Notifications for 12th/Intermediate jobs",
                    checked = notif12th,
                    onChecked = { notif12th = it; saveNotifPref("notif_12th", it, "12th Pass") }
                )
                SettingsSwitchRow(
                    title = "Graduation (Any)",
                    subtitle = "Notifications for any Bachelor's degree jobs",
                    checked = notifGrad,
                    onChecked = { notifGrad = it; saveNotifPref("notif_grad", it, "Graduation") }
                )
                SettingsSwitchRow(
                    title = "Engineering / ITI",
                    subtitle = "Notifications for technical qualification jobs",
                    checked = notifEngg,
                    onChecked = { notifEngg = it; saveNotifPref("notif_engg", it, "Engineering") }
                )
                SettingsNavRow(
                    title = "Notification Preferences",
                    subtitle = "Choose alerts based on qualification",
                    icon = Icons.Outlined.ArrowForward
                ) { navController.navigate("notification_prefs") }
                SettingsNavRow(
                    title = "Clear Offline Cache",
                    subtitle = cacheSizeLabel(context),
                    icon = null
                ) {
                    val deleted = context.cacheDir.deleteRecursively()
                    val msg = if (deleted) "Cache cleared successfully" else "Nothing to clear"
                    AppLogger.info("Settings", msg)
                    scope.launch { snackbar.showSnackbar(msg) }
                }

                // ─── COMMUNITY ──────────────────────────────────────
                SettingsHeader("COMMUNITY")
                SettingsNavRow(
                    title = "Submit an Update",
                    subtitle = "Help us by submitting missing alerts",
                    icon = Icons.Outlined.AddCircleOutline
                ) { navController.navigate("submit_update") }

                // ─── DEVELOPER ──────────────────────────────────────
                SettingsHeader("DEVELOPER")
                SettingsNavRow(
                    title = "App Logs",
                    subtitle = "${AppLogger.logs.size} entries",
                    icon = Icons.Outlined.Terminal
                ) { navController.navigate("app_logs") }

                // ─── APPLICATION ────────────────────────────────────
                SettingsHeader("APPLICATION")
                SettingsNavRow(
                    title = "About JobAlerts",
                    subtitle = "Version, Stats, and Source Code",
                    icon = Icons.Outlined.ArrowForward
                ) { navController.navigate("about_app") }
                SettingsNavRow(
                    title = "Privacy Policy",
                    subtitle = "Opens in browser",
                    icon = Icons.Outlined.OpenInNew
                ) {
                    openUrl(context, "https://jobalerts.app/privacy")
                    AppLogger.info("Settings", "Privacy Policy opened")
                }

                // Footer
                Text(
                    text = "JOBALERTS V1.0.1 · OPEN SOURCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// LOGS SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun AppLogsScreen(onBack: () -> Unit) {
    val logs = AppLogger.logs
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "App Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { AppLogger.clear() }) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Clear logs",
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No logs yet",
                    color = MaterialTheme.colorScheme.outlineVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            // Info bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${logs.size} entries · newest first",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val errorCount = logs.count { it.level == LogLevel.ERROR }
                    val warnCount = logs.count { it.level == LogLevel.WARN }
                    if (errorCount > 0)
                        Text(
                            "$errorCount ERR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorUrgent
                        )
                    if (warnCount > 0)
                        Text(
                            "$warnCount WARN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(logs) { entry ->
                    LogEntryRow(entry)
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH).format(Date(entry.timestamp))
    }
    val (levelColor, levelLabel) = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.outlineVariant to "I"
        LogLevel.WARN -> Color(0xFFE65100) to "W"
        LogLevel.ERROR -> ColorUrgent to "E"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Level badge
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(levelColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = levelLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.tag,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.message,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// NOTIFICATION PREFERENCES SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun NotificationPrefsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("jobalerts_notif", Context.MODE_PRIVATE) }
    var notif10th by remember { mutableStateOf(prefs.getBoolean("notif_10th", true)) }
    var notif12th by remember { mutableStateOf(prefs.getBoolean("notif_12th", true)) }
    var notifGrad by remember { mutableStateOf(prefs.getBoolean("notif_grad", true)) }
    var notifEngg by remember { mutableStateOf(prefs.getBoolean("notif_engg", false)) }

    fun save(key: String, value: Boolean, label: String) {
        prefs.edit().putBoolean(key, value).apply()
        AppLogger.info("NotifPrefs", "Saved $key = $value")
        scope.launch {
            snackbar.showSnackbar("$label alerts ${if (value) "enabled" else "disabled"}")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(30.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Alerts Setup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Select your qualifications. We will only send push notifications for " +
                    "jobs that match your profile.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outlineVariant,
                lineHeight = 21.sp,
                modifier = Modifier.padding(20.dp)
            )
            SettingsHeader("BY QUALIFICATION")
            SettingsSwitchRow("10th Pass Jobs", "Jobs requiring 10th/Matriculation", notif10th) {
                notif10th = it; save("notif_10th", it, "10th Pass")
            }
            SettingsSwitchRow("12th Pass Jobs", "Jobs requiring 12th/Intermediate", notif12th) {
                notif12th = it; save("notif_12th", it, "12th Pass")
            }
            SettingsSwitchRow("Graduation (Any)", "Any Bachelor's degree required", notifGrad) {
                notifGrad = it; save("notif_grad", it, "Graduation")
            }
            SettingsSwitchRow("Engineering / ITI", "Technical qualification requirements", notifEngg) {
                notifEngg = it; save("notif_engg", it, "Engineering")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ABOUT SCREEN — real values only (no hardcoded fake stats)
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "About JobAlerts",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkOutline,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "JobAlerts App",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Version 1.0.1 (F-Droid Build)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(32.dp))

            // Stats Info Box — real values only
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Log Entries",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        "${AppLogger.logs.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            // Links
            SettingsNavRow("GitHub Repository", "View open-source code", Icons.Outlined.Code) {
                openUrl(context, "https://github.com/mitwenx/Job-data")
                AppLogger.info("About", "GitHub link opened")
            }
            SettingsNavRow("Report a Bug", "Submit issues on Issue Tracker", Icons.Outlined.BugReport) {
                openUrl(context, "https://github.com/mitwenx/Job-data/issues")
                AppLogger.info("About", "Bug report link opened")
            }
            Spacer(Modifier.height(40.dp))
            Text(
                "Built with Jetpack Compose & GitHub Backend.\n" +
                    "Data collected from official sources only.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SUBMIT UPDATE SCREEN — opens a real GitHub issue in the browser
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SubmitUpdateScreen(onBack: () -> Unit) {
    var jobTitle by remember { mutableStateOf("") }
    var jobUrl by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(30.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Submit Update",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Know about a government job that's missing? Submit the job title or a " +
                        "link and our team will verify and add it.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    lineHeight = 21.sp
                )

                Column {
                    Text(
                        "JOB TITLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        placeholder = {
                            Text(
                                "e.g. SSC CGL 2026",
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        singleLine = true
                    )
                }

                Column {
                    Text(
                        "OFFICIAL URL (OPTIONAL)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = jobUrl,
                        onValueChange = { jobUrl = it },
                        placeholder = {
                            Text(
                                "https://ssc.nic.in/...",
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        singleLine = true
                    )
                }

                AnimatedVisibility(visible = submitted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSuccess.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .border(1.dp, ColorSuccess.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            tint = ColorSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Submission opened in your browser. Thank you!",
                            fontSize = 14.sp,
                            color = ColorSuccess
                        )
                    }
                }

                Button(
                    onClick = {
                        if (jobTitle.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Please enter a job title") }
                            return@Button
                        }
                        // Open a real GitHub issue pre-filled with the user's input.
                        val issueTitle = Uri.encode("New Job Submission: $jobTitle")
                        val issueBody = Uri.encode(
                            "**Job Title:** $jobTitle\n" +
                                "**URL:** ${jobUrl.ifBlank { "N/A" }}"
                        )
                        val issueUrl =
                            "https://github.com/mitwenx/Job-data/issues/new" +
                                "?title=$issueTitle&body=$issueBody"
                        openUrl(context, issueUrl)
                        AppLogger.info("SubmitUpdate", "Opened GitHub issue for: $jobTitle")
                        submitted = true
                        jobTitle = ""
                        jobUrl = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("Submit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// REUSABLE SETTING COMPONENTS
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 20.dp, top = 32.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            if (icon != null) {
                Icon(
                    icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedTrackColor = MaterialTheme.colorScheme.onBackground,
                    uncheckedThumbColor = MaterialTheme.colorScheme.background,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// HELPERS
// ════════════════════════════════════════════════════════════════════════════
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        AppLogger.error("Settings", "openUrl failed: ${e.message}")
    }
}

private fun cacheSizeLabel(context: Context): String {
    val bytes = context.cacheDir.walkTopDown().sumOf { it.length() }
    return when {
        bytes < 1024 -> "Free up local storage"
        bytes < 1024 * 1024 -> "Free up ${bytes / 1024} KB"
        else -> "Free up ${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
