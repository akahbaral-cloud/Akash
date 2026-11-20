package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AkashDarkColorScheme = darkColorScheme(
    primary = AkashCyan,
    onPrimary = SkyNightBackground,
    primaryContainer = AkashCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = SolarAmber,
    onSecondary = SkyNightBackground,
    secondaryContainer = SolarAmberContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = AuroraIndigo,
    onTertiary = SkyNightBackground,
    tertiaryContainer = AuroraIndigoContainer,
    onTertiaryContainer = TextPrimary,
    background = SkyNightBackground,
    onBackground = TextPrimary,
    surface = SkySurface,
    onSurface = TextPrimary,
    surfaceVariant = SkySurfaceContainer,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = SkySurfaceElevated,
    outline = OutlineSubtle,
    error = StatusError,
    onError = SkyNightBackground,
    errorContainer = StatusErrorContainer,
    onErrorContainer = TextPrimary
)

@Composable
fun AiAkashTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AkashDarkColorScheme,
        typography = AkashTypography,
        content = content
    )
}
