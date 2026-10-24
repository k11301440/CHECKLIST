package com.example.checklist.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.checklist.data.AccentTheme
import com.example.checklist.data.DarkThemeConfig
import com.example.checklist.data.ThemeMode

fun getSpaceXPalette(isDark: Boolean, accentTheme: AccentTheme): SpaceXColorPalette {
    return when (accentTheme) {
        AccentTheme.MONOCHROME -> if (isDark) DarkSpaceXPalette else LightSpaceXPalette
        AccentTheme.TRON -> if (isDark) TronSpaceXPalette else TronLightPalette
        AccentTheme.MONSTER -> if (isDark) MonsterSpaceXPalette else MonsterLightPalette
        AccentTheme.ANA -> if (isDark) AnaSpaceXPalette else AnaLightPalette
        AccentTheme.AIRDO -> if (isDark) AirdoSpaceXPalette else AirdoLightPalette
        AccentTheme.JAL -> if (isDark) JalSpaceXPalette else JalLightPalette
    }
}

@Composable
fun ChecklistTheme(
    darkThemeConfig: DarkThemeConfig = DarkThemeConfig.DARK,
    accentTheme: AccentTheme = AccentTheme.MONOCHROME,
    themeMode: ThemeMode? = null,
    uiSize: com.example.checklist.data.UiSize = com.example.checklist.data.UiSize.MEDIUM,
    content: @Composable () -> Unit,
) {
    val effectiveConfig = if (themeMode != null) {
        when (themeMode) {
            ThemeMode.SPACE_WHITE -> DarkThemeConfig.LIGHT
            ThemeMode.SYSTEM -> DarkThemeConfig.SYSTEM
            else -> DarkThemeConfig.DARK
        }
    } else {
        darkThemeConfig
    }

    val effectiveAccent = if (themeMode != null) {
        when (themeMode) {
            ThemeMode.SPACE_BLACK, ThemeMode.SPACE_WHITE, ThemeMode.SYSTEM -> AccentTheme.MONOCHROME
            ThemeMode.TRON_RED -> AccentTheme.TRON
            ThemeMode.MONSTER_GREEN -> AccentTheme.MONSTER
            ThemeMode.ANA_BLUE -> AccentTheme.ANA
            ThemeMode.JAL_RED -> AccentTheme.JAL
        }
    } else {
        accentTheme
    }

    val isDark = when (effectiveConfig) {
        DarkThemeConfig.DARK -> true
        DarkThemeConfig.LIGHT -> false
        DarkThemeConfig.SYSTEM -> isSystemInDarkTheme()
    }

    val palette = getSpaceXPalette(isDark, effectiveAccent)

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

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalSpaceXColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypography(uiSize),
            content = content
        )
    }
}
