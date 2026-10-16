package com.example.checklist.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.checklist.data.ThemeMode

@Composable
fun ChecklistTheme(
    themeMode: ThemeMode = ThemeMode.SPACE_BLACK,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SPACE_BLACK -> true
        ThemeMode.SPACE_WHITE -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val palette = if (isDark) DarkSpaceXPalette else LightSpaceXPalette

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.textPrimary,
            onPrimary = palette.bg,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.textSecondary,
            onSecondary = palette.bg,
            secondaryContainer = palette.surfaceDark,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.textTertiary,
            onTertiary = palette.bg,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.bg,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceDark,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.borderSubtle,
            outlineVariant = palette.borderProminent
        )
    } else {
        lightColorScheme(
            primary = palette.textPrimary,
            onPrimary = palette.bg,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.textSecondary,
            onSecondary = palette.bg,
            secondaryContainer = palette.surfaceDark,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.textTertiary,
            onTertiary = palette.bg,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.bg,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceDark,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.borderSubtle,
            outlineVariant = palette.borderProminent
        )
    }

    CompositionLocalProvider(LocalSpaceXColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
