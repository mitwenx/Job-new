package com.jobalerts.app.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jobalerts.app.domain.models.JobPost
import com.jobalerts.app.presentation.theme.ColorUrgent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Jobs feed screen.
 *
 * Features:
 *   - Header with date + search toggle
 *   - Expandable search bar (filters title / category / qualification)
 *   - Filter chips with active count ("Graduation · 12")
 *   - Pull-to-refresh
 *   - Skeleton loading cards (shimmer)
 *   - Friendly error state with retry
 *   - Improved job cards: category + vacancy badge, title + chevron,
 *     qualification + urgent deadline row
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JobsScreen(viewModel: JobsViewModel, onJobClick: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var searchVisible by remember { mutableStateOf(false) }
    var localSearch by remember { mutableStateOf("") }

    val isLoading = uiState is JobsUiState.Loading
    val pullState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.fetchJobs() }
    )

    val currentDate = remember {
        SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
            .format(Date())
            .uppercase(Locale.ENGLISH)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header Row ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JobAlerts",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentDate,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(
                    onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) {
                            localSearch = ""
                            viewModel.setSearchQuery("")
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = if (searchVisible) "Close search" else "Search",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Search Bar ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = searchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = localSearch,
                onValueChange = {
                    localSearch = it
                    viewModel.setSearchQuery(it)
                },
                placeholder = {
                    Text(
                        "Search jobs, qualifications…",
                        color = MaterialTheme.colorScheme.outlineVariant,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search, null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (localSearch.isNotEmpty()) {
                        IconButton(onClick = {
                            localSearch = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                Icons.Outlined.Close, "Clear",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.onBackground
                ),
                singleLine = true
            )
        }

        // ── Filter Chips ─────────────────────────────────────────────
        val filters = listOf("All", "10th Pass", "12th Pass", "Graduation")
        val jobCount = if (uiState is JobsUiState.Success) (uiState as JobsUiState.Success).jobs.size else 0
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val isActive = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(
                            color = if (isActive) MaterialTheme.colorScheme.onBackground
                            else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.setFilter(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val label = if (isActive && jobCount > 0) "$filter · $jobCount" else filter
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        // ── Body ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullState)
        ) {
            when (val state = uiState) {
                is JobsUiState.Loading -> {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
                        items(6) { SkeletonJobCard() }
                    }
                }
                is JobsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    Icons.Outlined.WifiOff, null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                "Could not load jobs",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            OutlinedButton(
                                onClick = { viewModel.retry() },
                                shape = RoundedCornerShape(4.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        MaterialTheme.colorScheme.onBackground
                                    )
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                )
                            ) {
                                Text(
                                    "Try Again",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                is JobsUiState.Success -> {
                    if (state.jobs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (localSearch.isNotEmpty())
                                    "No results for \"$localSearch\""
                                else
                                    "No jobs found for \"$selectedFilter\"",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            items(state.jobs, key = { it.id }) { job ->
                                JobCardItem(
                                    job = job,
                                    onClick = { onJobClick(job.id) }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// ── Skeleton Card ────────────────────────────────────────────────────────────
@Composable
private fun SkeletonJobCard() {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    val shimmerColor = MaterialTheme.colorScheme.outline.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Box(
            Modifier
                .width(70.dp)
                .height(10.dp)
                .background(shimmerColor, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth(0.88f)
                .height(14.dp)
                .background(shimmerColor, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .background(shimmerColor, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

// ── Job Card ─────────────────────────────────────────────────────────────────
@Composable
fun JobCardItem(job: JobPost, onClick: () -> Unit) {
    val today = remember { System.currentTimeMillis() }
    val deadlineMs = remember(job.lastDate) {
        runCatching {
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).parse(job.lastDate)?.time
        }.getOrNull()
    }
    val daysLeft = deadlineMs?.let { ((it - today) / (1000 * 60 * 60 * 24)).toInt() }
    val isUrgent = daysLeft != null && daysLeft in 0..7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp)
    ) {
        // Category tag + vacancy badge row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = job.category.uppercase(Locale.ENGLISH),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outlineVariant,
                letterSpacing = 0.5.sp
            )
            if (job.totalVacancies.isNotBlank() && job.totalVacancies != "N/A") {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${job.totalVacancies} Posts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // Title + chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = job.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(10.dp))

        // Qualification + Deadline row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.School, null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    job.qualificationTag,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule, null,
                    modifier = Modifier.size(14.dp),
                    tint = ColorUrgent
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isUrgent && daysLeft != null)
                        "Ends ${job.lastDate} ($daysLeft days left!)"
                    else
                        "Ends ${job.lastDate}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorUrgent
                )
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}
