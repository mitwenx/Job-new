package com.jobalerts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jobalerts.app.presentation.navigation.AppNavigation

/**
 * Single-activity host for the Compose UI.
 *
 * - Installs the Android 12+ SplashScreen API so the launcher icon shows
 *   cleanly during cold start.
 * - Reads the `navigate_to_job` extra from the launch intent (set by
 *   [com.jobalerts.app.core.notifications.NotificationHelper]) and forwards it
 *   to [AppNavigation] for deep-link handling.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Job ID passed from notification tap (deep-link)
        val initialJobId = intent?.getStringExtra("navigate_to_job")

        setContent {
            AppNavigation(initialJobId = initialJobId)
        }
    }
}
