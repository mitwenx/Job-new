package com.jobalerts.app.presentation.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.AlarmOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jobalerts.app.data.local.ReminderEntity
import com.jobalerts.app.data.local.dao.ReminderDao
import com.jobalerts.app.domain.models.JobPost
import com.jobalerts.app.presentation.details.cancelDeadlineAlarm
import com.jobalerts.app.presentation.settings.AppLogger
import com.jobalerts.app.presentation.theme.ColorInfo
import com.jobalerts.app.presentation.theme.ColorUrgent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// ── Reminder Data Class ──────────────────────────────────────────────────────
data class Reminder(
    val jobId: String,
    val title: String,
    val category: String,
    val lastDate: String, // e.g. "15 Apr 2026"
    val month: String,    // e.g. "APR"
    val day: String       // e.g. "15"
)

/**
 * ViewModel for the Reminders tab.
 *
 * Bug 4 fix: reminders are now backed by Room (via [ReminderDao]) instead of
 * an in-memory `MutableStateFlow<List<Reminder>>`. They survive app restarts
 * and are re-armed on device boot by [com.jobalerts.app.core.reminders.BootReceiver].
 *
 * Construct via [factory] so we can wire in the DAO from
 * [com.jobalerts.app.core.di.AppContainer].
 */
class RemindersViewModel(private val dao: ReminderDao) : ViewModel() {

    // Observe DB directly — always up-to-date, survives restarts
    val reminders: StateFlow<List<Reminder>> = dao.getAllReminders()
        .map { entities ->
            entities.map { entity ->
                val parts = entity.lastDate.split(" ", "/", "-")
                val day = parts.getOrNull(0)?.padStart(2, '0') ?: "??"
                val monthRaw = parts.getOrNull(1) ?: "??"
                Reminder(
                    jobId = entity.jobId,
                    title = entity.jobTitle,
                    category = "",
                    lastDate = entity.lastDate,
                    month = monthRaw.take(3).uppercase(),
                    day = day
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Called from JobDetailScreen after alarm is scheduled. */
    fun addReminder(job: JobPost) {
        viewModelScope.launch {
            try {
                val formats = listOf(
                    "dd MMMM yyyy", "d MMMM yyyy",
                    "dd MMM yyyy", "d MMM yyyy",
                    "dd/MM/yyyy", "yyyy-MM-dd"
                )
                var ms = 0L
                for (fmt in formats) {
                    ms = runCatching {
                        SimpleDateFormat(fmt, Locale.ENGLISH)
                            .parse(job.lastDate)?.time ?: 0L
                    }.getOrNull() ?: continue
                    if (ms > 0) break
                }
                dao.insertReminder(
                    ReminderEntity(
                        jobId = job.id,
                        jobTitle = job.title,
                        lastDate = job.lastDate,
                        scheduledTimeMs = ms - (24 * 60 * 60 * 1000L)
                    )
                )
                AppLogger.info("RemindersVM", "Saved reminder: ${job.id}")
            } catch (e: Exception) {
                AppLogger.error("RemindersVM", "addReminder failed: ${e.message}")
            }
        }
    }

    /** Called when user taps alarm-off; also cancels system alarm. */
    fun removeReminder(jobId: String) {
        viewModelScope.launch {
            try {
                dao.deleteReminder(jobId)
                AppLogger.info("RemindersVM", "Deleted reminder: $jobId")
            } catch (e: Exception) {
                AppLogger.error("RemindersVM", "removeReminder failed: ${e.message}")
            }
        }
    }

    fun hasReminder(jobId: String): Boolean = reminders.value.any { it.jobId == jobId }

    companion object {
        fun factory(dao: ReminderDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) =
                RemindersViewModel(dao) as T
        }
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────
@Composable
fun RemindersScreen(viewModel: RemindersViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Reminders",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (reminders.isEmpty()) {
            // ── Empty State ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "No active reminders",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Open any job and tap the alarm icon\nto set a deadline reminder.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }
        } else {
            // ── Info Banner ─────────────────────────────────────────
            Text(
                text = "Active Alarms. You will receive a local push notification 24 hours " +
                    "before the application deadline closes.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outlineVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            // ── List ────────────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(reminders, key = { it.jobId }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCancel = {
                            cancelDeadlineAlarm(context, reminder.jobId)
                            viewModel.removeReminder(reminder.jobId)
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Reminder Card ───────────────────────────────────────────────────────────
@Composable
fun ReminderCard(reminder: Reminder, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Calendar Box
        Column(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .defaultMinSize(minWidth = 65.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = reminder.month,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorUrgent,
                letterSpacing = 1.sp
            )
            Text(
                text = reminder.day,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(16.dp))

        // Job Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.category.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outlineVariant,
                letterSpacing = 0.5.sp
            )
            Text(
                text = reminder.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AlarmOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = ColorInfo
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Alarm active",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // Cancel Button
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Outlined.AlarmOff,
                contentDescription = "Cancel Reminder",
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}
