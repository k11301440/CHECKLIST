package com.example.checklist.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SpaceX Monochromatic & Futuristic Palettes
data class SpaceXColorPalette(
    val isDark: Boolean,
    val bg: Color,
    val surfaceDark: Color,
    val surfaceElevated: Color,
    val borderSubtle: Color,
    val borderProminent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val accent: Color = Color(0xFFFFFFFF)
)

val DarkSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF000000),
    surfaceDark = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF141414),
    borderSubtle = Color(0xFF262626),
    borderProminent = Color(0xFF404040),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF888888),
    textTertiary = Color(0xFF555555),
    textDisabled = Color(0xFF383838),
    accent = Color(0xFFFFFFFF)
)

val LightSpaceXPalette = SpaceXColorPalette(
    isDark = false,
    bg = Color(0xFFFFFFFF),
    surfaceDark = Color(0xFFF7F7F7),
    surfaceElevated = Color(0xFFEEEEEE),
    borderSubtle = Color(0xFFD6D6D6),
    borderProminent = Color(0xFFA0A0A0),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF555555),
    textTertiary = Color(0xFF888888),
    textDisabled = Color(0xFFCCCCCC),
    accent = Color(0xFF000000)
)

val TronSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF000000),
    surfaceDark = Color(0xFF0D0405),
    surfaceElevated = Color(0xFF18080A),
    borderSubtle = Color(0xFF381014),
    borderProminent = Color(0xFFFF1E27),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA8888A),
    textTertiary = Color(0xFF70484A),
    textDisabled = Color(0xFF3A2022),
    accent = Color(0xFFFF1E27) // TRON Cyber Red
)

val MonsterSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF000000),
    surfaceDark = Color(0xFF040D05),
    surfaceElevated = Color(0xFF08180A),
    borderSubtle = Color(0xFF103814),
    borderProminent = Color(0xFF00FF41),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF88A88A),
    textTertiary = Color(0xFF48704A),
    textDisabled = Color(0xFF203A22),
    accent = Color(0xFF00FF41) // MONSTER Energy Green
)

val LocalSpaceXColors = staticCompositionLocalOf { DarkSpaceXPalette }

object SpaceXTheme {
    val colors: SpaceXColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSpaceXColors.current
}

// 動的プロパティ（既存の全UIが自動でライト/ダーク切り替えに追従）
val SpaceBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.bg

val SurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.surfaceDark

val SurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.surfaceElevated

val BorderSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.borderSubtle

val BorderProminent: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.borderProminent

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.textSecondary

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.textTertiary

val TextDisabled: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.textDisabled

val SpaceXAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = SpaceXTheme.colors.accent
