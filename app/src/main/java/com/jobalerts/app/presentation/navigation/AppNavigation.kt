package com.jobalerts.app.presentation.navigation

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jobalerts.app.JobAlertsApp
import com.jobalerts.app.presentation.details.JobDetailScreen
import com.jobalerts.app.presentation.home.JobsScreen
import com.jobalerts.app.presentation.home.JobsUiState
import com.jobalerts.app.presentation.home.JobsViewModel
import com.jobalerts.app.presentation.reminders.RemindersScreen
import com.jobalerts.app.presentation.reminders.RemindersViewModel
import com.jobalerts.app.presentation.settings.*
import com.jobalerts.app.presentation.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ── DataStore for persisted dark-mode preference ─────────────────────────────
private val Context.darkModeDataStore by preferencesDataStore(name = "theme_prefs")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

private fun isSystemDarkThemeDefault(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

// ── Root composable ──────────────────────────────────────────────────────────
@Composable
fun AppNavigation(initialJobId: String? = null) {
    val context = LocalContext.current

    // Read dark-mode preference from DataStore (Bug 5 fix). Falls back to system default.
    val darkModeFlow = remember {
        context.darkModeDataStore.data.map { prefs ->
            prefs[DARK_MODE_KEY] ?: isSystemDarkThemeDefault(context)
        }
    }
    val isDarkTheme by darkModeFlow.collectAsState(initial = isSystemInDarkTheme())

    val scope = rememberCoroutineScope()
    val onThemeToggle: (Boolean) -> Unit = { newValue ->
        scope.launch {
            context.darkModeDataStore.edit { it[DARK_MODE_KEY] = newValue }
        }
    }

    JobAlertsTheme(
        darkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle
    ) {
        AppContent(initialJobId = initialJobId)
    }
}

@Composable
private fun AppContent(initialJobId: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as JobAlertsApp

    // ── Notification Permission (Android 13+) ───────────────────────────────
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permState = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!permState.status.isGranted) {
                permState.launchPermissionRequest()
            }
        }
    }

    // ── ViewModels injected from AppContainer ───────────────────────────────
    val jobsViewModel: JobsViewModel = viewModel(
        factory = JobsViewModel.factory(app.container.jobRepository)
    )
    val remindersViewModel: RemindersViewModel = viewModel(
        factory = RemindersViewModel.factory(app.container.database.reminderDao())
    )

    // ── Navigation deep-link from notification ──────────────────────────────
    LaunchedEffect(initialJobId) {
        if (initialJobId != null) {
            navController.navigate("details/$initialJobId")
        }
    }

    // ── Determine which routes show the bottom bar ──────────────────────────
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = setOf("jobs", "results", "reminders", "settings")
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController, currentRoute)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "jobs",
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Tab 1: Jobs Feed ─────────────────────────────────────
            composable("jobs") {
                JobsScreen(
                    viewModel = jobsViewModel,
                    onJobClick = { jobId -> navController.navigate("details/$jobId") }
                )
            }

            // ── Tab 2: Results & Admit Cards ────────────────────────
            composable("results") {
                ResultsScreen(
                    jobsViewModel = jobsViewModel,
                    onJobClick = { jobId -> navController.navigate("details/$jobId") }
                )
            }

            // ── Tab 3: Reminders ────────────────────────────────────
            composable("reminders") {
                RemindersScreen(viewModel = remindersViewModel)
            }

            // ── Tab 4: Settings ─────────────────────────────────────
            composable("settings") {
                SettingsScreen(navController = navController)
            }

            // ── Job Detail ──────────────────────────────────────────
            // Use allJobs (not the filtered uiState) so we can always find a
            // job by ID even when an active filter would have hidden it.
            composable("details/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val allJobs by jobsViewModel.allJobs.collectAsState()
                val job = allJobs.find { it.id == jobId }

                JobDetailScreen(
                    job = job,
                    onBack = { navController.popBackStack() },
                    onSetAlarm = { jobPost ->
                        remindersViewModel.addReminder(jobPost)
                        // AlarmManager scheduling is done inside JobDetailScreen itself
                    }
                )
            }

            // ── About ───────────────────────────────────────────────
            composable("about_app") {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            // ── Notification Preferences ────────────────────────────
            composable("notification_prefs") {
                NotificationPrefsScreen(onBack = { navController.popBackStack() })
            }

            // ── App Logs ────────────────────────────────────────────
            composable("app_logs") {
                AppLogsScreen(onBack = { navController.popBackStack() })
            }

            // ── Submit Update ───────────────────────────────────────
            composable("submit_update") {
                SubmitUpdateScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// ── Results Screen (inline — reuses jobs data, filters by category) ──────────
@Composable
private fun ResultsScreen(
    jobsViewModel: JobsViewModel,
    onJobClick: (String) -> Unit
) {
    val uiState by jobsViewModel.uiState.collectAsState()
    var resultsFilter by remember { mutableStateOf("All Updates") }
    val resultFilters = listOf("All Updates", "Admit Cards", "Results Declared", "Answer Keys")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Results",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(resultFilters) { filter ->
                val isActive = filter == resultsFilter
                Box(
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isActive) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(24.dp)
                        )
                        .background(
                            if (isActive) MaterialTheme.colorScheme.onBackground
                            else Color.Transparent,
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { resultsFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        when (val state = uiState) {
            is JobsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
            is JobsUiState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Could not load results.\n${state.message}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            is JobsUiState.Success -> {
                // Filter to result-type jobs based on category keywords
                val resultJobs = state.jobs.filter { job ->
                    val cat = job.category.lowercase()
                    when (resultsFilter) {
                        "Admit Cards" -> cat.contains("admit") || cat.contains("hall ticket")
                        "Results Declared" -> cat.contains("result")
                        "Answer Keys" -> cat.contains("answer") || cat.contains("key")
                        else -> cat.contains("result") ||
                            cat.contains("admit") ||
                            cat.contains("answer") ||
                            cat.contains("key")
                    }
                }
                if (resultJobs.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No \"$resultsFilter\" updates available",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(resultJobs, key = { it.id }) { job ->
                            com.jobalerts.app.presentation.home.JobCardItem(
                                job = job,
                                onClick = { onJobClick(job.id) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Bottom Navigation Bar ────────────────────────────────────────────────────
@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
    ) {
        val navItems = listOf(
            NavItem("jobs", "Jobs", Icons.Outlined.WorkOutline, Icons.Filled.Work),
            NavItem("results", "Results", Icons.Outlined.Article, Icons.Filled.Article),
            NavItem("reminders", "Reminders", Icons.Outlined.Alarm, Icons.Filled.Alarm),
            NavItem("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
        )
        navItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector
)
