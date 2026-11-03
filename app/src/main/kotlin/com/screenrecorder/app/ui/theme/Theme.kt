package com.screenrecorder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = DarkBackground,
    primaryContainer = AccentCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AccentPurple,
    onSecondary = TextPrimary,
    secondaryContainer = AccentPurpleContainer,
    onSecondaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    error = AccentRed,
    onError = TextPrimary,
    errorContainer = AccentRedContainer,
    onErrorContainer = TextPrimary
)

@Composable
fun ScreenRecorderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
