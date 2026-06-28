package com.jobalerts.app.presentation.details

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jobalerts.app.core.reminders.AlarmReceiver
import com.jobalerts.app.domain.models.JobPost
import com.jobalerts.app.presentation.settings.AppLogger
import com.jobalerts.app.presentation.theme.ColorSuccess
import com.jobalerts.app.presentation.theme.ColorUrgent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    job: JobPost?,
    onBack: () -> Unit,
    onSetAlarm: (JobPost) -> Unit
) {
    val context = LocalContext.current
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Loading state while job is being resolved
    if (job == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = 2.dp,
                trackColor = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    actionColor = MaterialTheme.colorScheme.background,
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
            // ── Toolbar ───────────────────────────────────────────────
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
                    "Job Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row {
                    // ── Real Share Intent ──────────────────────────────
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine(job.title)
                            appendLine("Category: ${job.category}")
                            appendLine("Qualification: ${job.qualificationTag}")
                            if (job.totalVacancies.isNotBlank())
                                appendLine("Vacancies: ${job.totalVacancies}")
                            appendLine("Last Date: ${job.lastDate}")
                            if (job.applyLink.isNotBlank())
                                appendLine("Apply: ${job.applyLink}")
                            appendLine()
                            appendLine("Shared via JobAlerts")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Job"))
                        AppLogger.info("JobDetailScreen", "Share triggered for: ${job.id}")
                    }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    // ── Smart Alarm Button ─────────────────────────────
                    IconButton(onClick = {
                        onSetAlarm(job)
                        scheduleDeadlineAlarm(context, job)
                        scope.launch {
                            snackbarState.showSnackbar("Alarm set for 1 day before ${job.lastDate}")
                        }
                    }) {
                        Icon(
                            Icons.Outlined.AlarmAdd,
                            contentDescription = "Set Alarm",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            // ── Scrollable Content ────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp)
            ) {
                // Category + Title
                Text(
                    text = job.category.uppercase(Locale.ENGLISH),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = job.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(24.dp))

                // ── Urgent Deadline Banner ───────────────────────────
                val today = System.currentTimeMillis()
                val deadlineMs = remember(job.lastDate) {
                    runCatching {
                        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                            .parse(job.lastDate)?.time
                    }.getOrNull()
                }
                val daysLeft = deadlineMs?.let { ((it - today) / (1000 * 60 * 60 * 24)).toInt() }
                if (daysLeft != null && daysLeft in 0..7) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorUrgent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, ColorUrgent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Schedule, null,
                                tint = ColorUrgent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Deadline in $daysLeft day${if (daysLeft != 1) "s" else ""}! " +
                                    "Apply before ${job.lastDate}.",
                                fontSize = 13.sp,
                                color = ColorUrgent,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 19.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // ── Info Box 1: Vacancy Details ──────────────────────
                InfoBox {
                    InfoRow("Total Vacancies", job.totalVacancies, valueColor = ColorSuccess)
                    InfoDivider()
                    InfoRow("Qualification", job.qualification)
                    InfoDivider()
                    InfoRow("Age Limit", job.ageLimit)
                    InfoDivider()
                    InfoRow("Application Fee", job.fee)
                }
                Spacer(Modifier.height(24.dp))

                // ── Section Label ────────────────────────────────────
                Text(
                    text = "IMPORTANT DATES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(12.dp))

                // ── Info Box 2: Dates ────────────────────────────────
                InfoBox {
                    InfoRow("Start Date", job.startDate)
                    InfoDivider()
                    InfoRow("Last Date", job.lastDate, valueColor = ColorUrgent)
                    InfoDivider()
                    InfoRow("Exam Date", job.examDate)
                }
                Spacer(Modifier.height(32.dp))

                // ── Apply Button ─────────────────────────────────────
                Button(
                    onClick = {
                        if (job.applyLink.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(job.applyLink))
                            context.startActivity(intent)
                            AppLogger.info("JobDetailScreen", "Opened apply link for ${job.id}")
                        }
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
                    Text("Apply Online Link", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                // ── Download PDF Button (if available) ──────────────
                if (job.pdfLink.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(job.pdfLink))
                            context.startActivity(intent)
                            AppLogger.info("JobDetailScreen", "Opened PDF for ${job.id}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.outline
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text("Download PDF Notice", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Description / Additional Info ───────────────────
                if (job.description.isNotBlank()) {
                    Spacer(Modifier.height(32.dp))
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = job.description,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// ── Info Box Composables ────────────────────────────────────────────────────
@Composable
fun InfoBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
fun InfoDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun InfoRow(
    key: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Alarm Scheduling ────────────────────────────────────────────────────────
/**
 * Schedules a system alarm to fire 24 hours before the job's last date.
 *
 * Bug 1 fix: uses [AlarmReceiver::class.java] (NOT `Class.forName("com.jobalerts.app.AlarmReceiver")`).
 * Also passes the `LAST_DATE` extra so the receiver can include it in the
 * notification body.
 *
 * Falls back gracefully if the date can't be parsed.
 */
fun scheduleDeadlineAlarm(context: Context, job: JobPost) {
    try {
        val formats = listOf(
            "dd MMMM yyyy", "d MMMM yyyy",
            "dd MMM yyyy", "d MMM yyyy",
            "dd/MM/yyyy", "yyyy-MM-dd"
        )
        var deadlineMs: Long? = null
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                deadlineMs = sdf.parse(job.lastDate)?.time
                if (deadlineMs != null) break
            } catch (_: Exception) {
                // try next format
            }
        }

        if (deadlineMs == null) {
            AppLogger.warn("AlarmScheduler", "Could not parse date '${job.lastDate}' — alarm not set")
            return
        }

        // Fire 24 hours before deadline
        val alarmMs = deadlineMs - (24 * 60 * 60 * 1000L)
        if (alarmMs <= System.currentTimeMillis()) {
            AppLogger.warn("AlarmScheduler", "Deadline already passed for ${job.id} — alarm not set")
            return
        }

        val notifyIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("JOB_ID", job.id)
            putExtra("JOB_TITLE", job.title)
            putExtra("LAST_DATE", job.lastDate)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            job.id.hashCode(),
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmMs, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmMs, pendingIntent)
        }
        AppLogger.info("AlarmScheduler", "Alarm set for ${job.id} at $alarmMs")
    } catch (e: Exception) {
        AppLogger.error("AlarmScheduler", "scheduleDeadlineAlarm failed: ${e.message}")
    }
}

/**
 * Cancels a previously scheduled alarm for a given job.
 */
fun cancelDeadlineAlarm(context: Context, jobId: String) {
    try {
        val notifyIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            jobId.hashCode(),
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        AppLogger.info("AlarmScheduler", "Alarm cancelled for $jobId")
    } catch (e: Exception) {
        AppLogger.error("AlarmScheduler", "cancelDeadlineAlarm failed: ${e.message}")
    }
}
