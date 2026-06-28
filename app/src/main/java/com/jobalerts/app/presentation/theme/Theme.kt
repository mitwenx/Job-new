package com.jobalerts.app.presentation.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
// ??? Composition Local for dark-mode toggle access throughout the tree ???????
data class ThemeState(val isDark: Boolean, val toggle: (Boolean) -> Unit)
val LocalThemeState = compositionLocalOf<ThemeState> { error("No ThemeState provided") }
// ??? Color Schemes ????????????????????????????????????????????????????????????
private val LightColorScheme = lightColorScheme(
background = LightBackground,
surface = LightSurface,
onBackground = LightText,
onSurface = LightText,
outline = LightBorder,
outlineVariant = LightMuted,
primary = LightText,
onPrimary = LightBackground,
primaryContainer = LightSurface,
secondary = LightText,
onSecondary = LightBackground,
surfaceVariant = LightSurface,
onSurfaceVariant = LightMuted,
error = ColorUrgent,
onError = LightBackground
)
private val DarkColorScheme = darkColorScheme(
background = DarkBackground,
surface = DarkSurface,
onBackground = DarkText,
onSurface = DarkText,
outline = DarkBorder,
outlineVariant = DarkMuted,
primary = DarkText,
onPrimary = DarkBackground,
primaryContainer = DarkSurface,
secondary = DarkText,
onSecondary = DarkBackground,
surfaceVariant = DarkSurface,
onSurfaceVariant = DarkMuted,
error = ColorUrgent,
onError = DarkBackground
)
// ??? Theme Composable ?????????????????????????????????????????????????????????
@Composable
fun JobAlertsTheme(
darkTheme: Boolean,
onThemeToggle: (Boolean) -> Unit,
content: @Composable () -> Unit
) {
val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
CompositionLocalProvider(LocalThemeState provides ThemeState(darkTheme, onThemeToggle)) {
MaterialTheme(
colorScheme = colorScheme,
typography = Typography,
content = content
)
}
}