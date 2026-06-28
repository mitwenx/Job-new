# JobAlerts — Production-Ready Android App

A Jetpack Compose + Material 3 app that fetches government job listings from a
GitHub-hosted JSON + Markdown backend and shows them with filters, reminders,
alarms, and notifications. Built for F-Droid and Google Play Store.

> **Status:** Production-ready. All 8 critical bugs from the upgrade brief have
> been fixed. No placeholder code, no fake data, no known crashes.

---

## Tech Stack (do not change)

| Layer            | Library                                              |
|------------------|------------------------------------------------------|
| Language         | Kotlin 1.9.22                                        |
| UI               | Jetpack Compose + Material 3 (no XML layouts)        |
| Navigation       | Navigation Compose 2.7.7                             |
| Local DB         | Room 2.6.1                                           |
| Background work  | WorkManager 2.9.0                                    |
| Network          | Retrofit 2.9.0 + OkHttp 4.12.0 (with logging)       |
| Preferences      | AndroidX DataStore Preferences 1.0.0                |
| Permissions      | Accompanist Permissions 0.34.0                       |
| Splash           | AndroidX Core SplashScreen 1.0.1                     |
| DI               | Manual — no Hilt / no Koin (annotation processors    |
|                  | would break the simple build)                        |
| Min / Target SDK | 26 / 34 (Compile SDK 34)                             |
| Backend          | `https://raw.githubusercontent.com/mitwenx/Job-data/main/` (immutable) |

---

## Build

Requirements: **JDK 17**, **Android Studio Iguana+** (or Gradle 8.2 CLI).

```bash
# Debug build
./gradlew assembleDebug

# Release build (minified + resource-shrunk via proguard-rules.pro)
./gradlew assembleRelease

# Install on a connected device
./gradlew installDebug
```

Output APKs:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

The release build runs R8 with `proguard-rules.pro` — Retrofit, Gson, Room,
WorkManager, app models, and Kotlin coroutines are explicitly kept.

---

## Project Structure

```
JobAlerts/
├── app/
│   ├── build.gradle.kts                 ← Phase 1 deps + buildTypes + proguard
│   ├── proguard-rules.pro               ← NEW — production keep rules
│   └── src/main/
│       ├── AndroidManifest.xml          ← Updated: permissions, receivers, splash
│       ├── java/com/jobalerts/app/
│       │   ├── JobAlertsApp.kt          ← Builds AppContainer, channels, sync
│       │   ├── MainActivity.kt          ← installSplashScreen + deep-link
│       │   ├── core/
│       │   │   ├── config/AppConfigManager.kt
│       │   │   ├── di/AppContainer.kt           ← NEW — manual DI container
│       │   │   ├── notifications/NotificationHelper.kt  ← NEW — real notifs
│       │   │   ├── parser/JobMarkdownParser.kt
│       │   │   ├── reminders/
│       │   │   │   ├── AlarmReceiver.kt         ← Real notification (not Toast)
│       │   │   │   ├── BootReceiver.kt          ← NEW — re-arm alarms on boot
│       │   │   │   └── ReminderScheduler.kt     ← Multi-format date parser
│       │   │   └── sync/DataSyncWorker.kt       ← Full pipeline implementation
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── AppDatabase.kt           ← Singleton getInstance()
│       │   │   │   ├── ReminderEntity.kt
│       │   │   │   └── dao/ReminderDao.kt
│       │   │   ├── remote/
│       │   │   │   ├── GitHubApiService.kt
│       │   │   │   └── NetworkClient.kt         ← NEW — OkHttp + Retrofit
│       │   │   └── repository/JobRepositoryImpl.kt  ← Injected deps
│       │   ├── domain/
│       │   │   ├── models/JobPost.kt
│       │   │   └── repository/JobRepository.kt
│       │   └── presentation/
│       │       ├── details/JobDetailScreen.kt   ← Bug 1 fix + urgent banner
│       │       ├── home/
│       │       │   ├── JobsScreen.kt            ← Search + pull-refresh + skeleton
│       │       │   └── JobsViewModel.kt         ← allJobs + factory
│       │       ├── navigation/AppNavigation.kt  ← DataStore dark mode + deep-link
│       │       ├── reminders/RemindersScreen.kt ← Room-backed ViewModel
│       │       ├── settings/SettingsScreen.kt   ← Real submit + snackbar
│       │       └── theme/ (Color.kt, Theme.kt, Type.kt)
│       └── res/
│           ├── drawable/
│           │   ├── ic_launcher_foreground.xml
│           │   └── ic_notification.xml          ← NEW — white notif icon
│           ├── mipmap-anydpi-v26/ (ic_launcher.xml, ic_launcher_round.xml)
│           ├── values/ (colors.xml, strings.xml, themes.xml)
│           └── xml/
│               └── network_security_config.xml  ← NEW — HTTPS-only
├── build.gradle.kts                             ← Root
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew, gradlew.bat
├── data_config.json                             ← Reference config (mirror of backend)
└── .github/workflows/build-release.yml
```

---

## What Changed vs. the Original (Upgrade Brief Applied)

### 8 Critical Bugs Fixed

| # | Bug | File | Fix |
|---|-----|------|-----|
| 1 | `Class.forName("com.jobalerts.app.AlarmReceiver")` → ClassNotFoundException | `JobDetailScreen.kt` | Use `AlarmReceiver::class.java` + add `LAST_DATE` extra |
| 2 | `DataSyncWorker.doWork()` was a no-op | `DataSyncWorker.kt` | Full pipeline: fetch config → fetch index → diff against stored IDs → notify on matches → persist |
| 3 | `AppDatabase` never instantiated → NPE | `AppDatabase.kt` | Added thread-safe `getInstance(context)` singleton |
| 4 | Reminders stored in-memory → lost on restart | `RemindersScreen.kt` | `RemindersViewModel` now takes `ReminderDao`, observes `getAllReminders()` Flow |
| 5 | Dark mode toggle reset on cold start | `AppNavigation.kt` | Persisted via DataStore `booleanPreferencesKey("dark_mode")` |
| 6 | `JobRepositoryImpl` built Retrofit inline | `JobRepositoryImpl.kt` + `NetworkClient.kt` | Retrofit + OkHttp moved to `NetworkClient`; injected via constructor |
| 7 | `AlarmReceiver` showed a Toast | `AlarmReceiver.kt` + `NotificationHelper.kt` | Real `NotificationCompat` notification with high importance channel |
| 8 | `RemindersViewModel()` called with no-arg → crash after Bug 4 fix | `AppNavigation.kt` | Added `RemindersViewModel.factory(dao)` wired to `AppContainer` |

### Phase 1 — Gradle Dependencies

Added to `app/build.gradle.kts`:
- `com.squareup.okhttp3:okhttp:4.12.0` + `logging-interceptor:4.12.0`
- `androidx.core:core-splashscreen:1.0.1`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0`
- `androidx.lifecycle:lifecycle-runtime-compose:2.7.0`
- `com.google.accompanist:accompanist-permissions:0.34.0`
- `buildFeatures { buildConfig = true }`
- `buildTypes { release { minify + shrink + proguard } debug { .debug suffix } }`

### Phase 2 — New Core Infrastructure (7 new files)

- `core/di/AppContainer.kt` — manual DI container (database, network, repository)
- `data/remote/NetworkClient.kt` — OkHttp (30s timeouts, retry) + Retrofit + BASIC logging
- `core/notifications/NotificationHelper.kt` — two channels (deadline + new jobs), real `NotificationCompat`
- `core/reminders/BootReceiver.kt` — re-arms all alarms after `ACTION_BOOT_COMPLETED`
- `res/drawable/ic_notification.xml` — white silhouette icon for status bar
- `res/xml/network_security_config.xml` — HTTPS-only for `raw.githubusercontent.com`
- `proguard-rules.pro` — keeps Retrofit, Gson, Room, WorkManager, app models

### Phase 3 — Core File Rewrites (7 files)

- `JobAlertsApp.kt` — instantiates `AppContainer`, creates channels, schedules `DataSyncWorker` (30 min periodic, 15 min flex, exponential backoff)
- `MainActivity.kt` — `installSplashScreen()` + reads `navigate_to_job` extra for deep-link
- `AppDatabase.kt` — `@Volatile` `INSTANCE` + `synchronized` singleton builder
- `JobRepositoryImpl.kt` — injected `apiService` + `reminderDao`; per-job markdown errors swallowed gracefully
- `AlarmReceiver.kt` — calls `NotificationHelper.showDeadlineNotification` (no Toast)
- `ReminderScheduler.kt` — supports 6 date formats; `scheduleReminder` + `cancelReminder`
- `DataSyncWorker.kt` — full sync pipeline with SharedPreferences-backed user prefs

### Phase 4 — Presentation Layer Rewrites (6 files)

- `JobsViewModel.kt` — exposes `searchQuery`, `allJobs`, `selectedFilter`, `factory(repository)`
- `JobsScreen.kt` — search bar, pull-to-refresh, skeleton shimmer cards, vacancy badge + chevron + count on filter chips, urgent deadline label
- `JobDetailScreen.kt` — Bug 1 fix (`AlarmReceiver::class.java` + `LAST_DATE`), urgent deadline banner (≤7 days), clean share text (no emojis)
- `RemindersScreen.kt` — `RemindersViewModel(dao)` with `factory(dao)`, observes Room Flow, survives restarts
- `AppNavigation.kt` — `AppContainer`-based injection, DataStore dark mode, `POST_NOTIFICATIONS` permission (Android 13+), `allJobs`-based detail lookup, deep-link from notification
- `SettingsScreen.kt` — removed hardcoded "1,245 All Time" + "4,502" stats; replaced fake delay with real GitHub issue URL (`https://github.com/mitwenx/Job-data/issues/new?title=...&body=...`); Snackbar after each notification preference toggle

### Phase 5 — Manifest & Resources

- `AndroidManifest.xml` — `INTERNET`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`; `BootReceiver` with `BOOT_COMPLETED` filter; `AlarmReceiver`; `networkSecurityConfig`; `enableOnBackInvokedCallback`; splash theme
- `themes.xml` — `Theme.JobAlerts.Splash` parent `Theme.SplashScreen` with white background + animated launcher icon + `postSplashScreenTheme`

---

## Critical Rules Followed

1. ✅ Full file paths in every source file (package declarations match directories)
2. ✅ 100% complete files — no `// ... rest of file`, no `TODO`s, no truncation
3. ✅ All imports explicit
4. ✅ Package declarations match directory paths
5. ✅ No Hilt, no Koin — manual DI via `AppContainer`
6. ✅ All UI in Jetpack Compose + Material 3
7. ✅ All colors via `MaterialTheme.colorScheme.*` (except `ColorUrgent` / `ColorSuccess` / `ColorInfo` from `Color.kt`)
8. ✅ Never used `Class.forName()` — always `SomeClass::class.java`
9. ✅ No Toast in `BroadcastReceiver` — uses `NotificationHelper` or `Log`
10. ✅ All nullable/error cases handled; no force-unwraps (`!!`) without null checks
11. ✅ Files generated in dependency order: core → data → domain → presentation
12. ✅ Backend is immutable: `https://raw.githubusercontent.com/mitwenx/Job-data/main/` is the only data source

---

## Architecture Diagram (text)

```
┌─────────────────────────────────────────────────────────────────┐
│  MainActivity (splash + deep-link from notification)            │
└───────────────────────────┬─────────────────────────────────────┘
                            │ setContent
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  AppNavigation  (DataStore dark-mode + POST_NOTIFICATIONS perm) │
│  └─ NavHost with 4 top-level tabs + 4 detail screens            │
└───────────────────────────┬─────────────────────────────────────┘
                            │ uses
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  AppContainer (manual DI — created in JobAlertsApp.onCreate)    │
│  ├─ database: AppDatabase (Room singleton)                      │
│  │    └─ reminderDao(): ReminderDao                              │
│  ├─ networkClient: NetworkClient (OkHttp + Retrofit)            │
│  │    └─ apiService: GitHubApiService                            │
│  └─ jobRepository: JobRepositoryImpl(apiService, reminderDao)   │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ JobsViewModel│  │RemindersViewModel│  │ DataSyncWorker   │
│  (factory)   │  │   (factory(dao)) │  │  (WorkManager)   │
└──────┬───────┘  └────────┬─────────┘  └────────┬─────────┘
       │                   │                     │
       ▼                   ▼                     ▼
  JobRepository       ReminderDao           GitHub raw
  (fetch + parse)     (Room Flow)           (every 30 min)
                            │                     │
                            ▼                     ▼
                     AlarmReceiver       NotificationHelper
                     (BroadcastReceiver) (real NotificationCompat)
                            │
                            ▼
                     BootReceiver  ← reschedules all alarms after reboot
```

---

## Data Source (immutable)

- Config: `https://raw.githubusercontent.com/mitwenx/Job-data/main/data_config.json`
- Jobs index: `{api_base_url}{jobs_endpoint}` → returns `[{id, file, category, title, qualification_tag, last_date}, ...]`
- Per-job markdown: `{api_base_url}jobs/{file}` — parsed by `JobMarkdownParser` (TV/QU/AGE/FEE/SD/LD/ED/LINK/PDF prefixes)

User qualification prefs are persisted in `SharedPreferences("jobalerts_notif")`:
- `notif_10th`, `notif_12th`, `notif_grad`, `notif_engg`

Background sync known-job IDs are persisted in `DataStore("sync_state")` under
`known_job_ids` (comma-separated).

---

## License & Attribution

Open source. Built with Jetpack Compose. Data sourced only from official
government sources via the GitHub-hosted backend.
