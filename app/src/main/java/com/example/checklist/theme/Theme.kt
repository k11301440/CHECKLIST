package com.example.checklist.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpaceXColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = SpaceBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = SpaceBlack,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = TextPrimary,
    tertiary = TextSecondary,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SpaceBlack,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderProminent
)

@Composable
fun ChecklistTheme(
    content: @Composable () -> Unit,
) {
    // Pure Black SpaceX aesthetic is always applied regardless of system setting
    MaterialTheme(
        colorScheme = SpaceXColorScheme,
        typography = Typography,
        content = content
    )
}
