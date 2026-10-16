package com.example.checklist.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SpaceX Monochromatic Palettes - Pure Black & Crisp White
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
    val textDisabled: Color
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
    textDisabled = Color(0xFF383838)
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
    textDisabled = Color(0xFFCCCCCC)
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
