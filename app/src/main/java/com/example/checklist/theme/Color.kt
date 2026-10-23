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
    borderSubtle = Color(0xFF2E2E2E),
    borderProminent = Color(0xFF555555),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB8B8B8),
    textTertiary = Color(0xFF8E8E8E),
    textDisabled = Color(0xFF606060),
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
    surfaceDark = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF1F0808),
    borderSubtle = Color(0xFF3E1212),
    borderProminent = Color(0xFFFF1E27),
    textPrimary = Color(0xFFFF5252),
    textSecondary = Color(0xFFFF8A80),
    textTertiary = Color(0xFFE57373),
    textDisabled = Color(0xFF753030),
    accent = Color(0xFFFF1E27)
)

val TronLightPalette = SpaceXColorPalette(
    isDark = false,
    bg = Color(0xFFFAFAFA),
    surfaceDark = Color(0xFFF2F2F2),
    surfaceElevated = Color(0xFFFFEBEE),
    borderSubtle = Color(0xFFFFCDD2),
    borderProminent = Color(0xFFE53935),
    textPrimary = Color(0xFFC62828),
    textSecondary = Color(0xFFE53935),
    textTertiary = Color(0xFFEF9A9A),
    textDisabled = Color(0xFFB0BEC5),
    accent = Color(0xFFD32F2F)
)

val MonsterSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF000000),
    surfaceDark = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF071F0C),
    borderSubtle = Color(0xFF0D3817),
    borderProminent = Color(0xFF00FF41),
    textPrimary = Color(0xFF69F0AE),
    textSecondary = Color(0xFF00E676),
    textTertiary = Color(0xFF66BB6A),
    textDisabled = Color(0xFF388E3C),
    accent = Color(0xFF00FF41)
)

val MonsterLightPalette = SpaceXColorPalette(
    isDark = false,
    bg = Color(0xFFFAFAFA),
    surfaceDark = Color(0xFFF2F2F2),
    surfaceElevated = Color(0xFFE8F5E9),
    borderSubtle = Color(0xFFC8E6C9),
    borderProminent = Color(0xFF00C853),
    textPrimary = Color(0xFF1B5E20),
    textSecondary = Color(0xFF2E7D32),
    textTertiary = Color(0xFF81C784),
    textDisabled = Color(0xFFB0BEC5),
    accent = Color(0xFF00A844)
)

val AnaSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF00050D),
    surfaceDark = Color(0xFF030D1A),
    surfaceElevated = Color(0xFF08172E),
    borderSubtle = Color(0xFF132B52),
    borderProminent = Color(0xFF0066CC),
    textPrimary = Color(0xFFE1EFFF),
    textSecondary = Color(0xFF80B3FF),
    textTertiary = Color(0xFF7CA6DC),
    textDisabled = Color(0xFF3E6088),
    accent = Color(0xFF1A8CFF)
)

val AnaLightPalette = SpaceXColorPalette(
    isDark = false,
    bg = Color(0xFFF7FAFD),
    surfaceDark = Color(0xFFEBF3FC),
    surfaceElevated = Color(0xFFDCEBFA),
    borderSubtle = Color(0xFFB8D7F7),
    borderProminent = Color(0xFF0066CC),
    textPrimary = Color(0xFF002D62),
    textSecondary = Color(0xFF0056B3),
    textTertiary = Color(0xFF6B8DB5),
    textDisabled = Color(0xFFA0B5CC),
    accent = Color(0xFF0066CC)
)

val AirdoSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF00080B),
    surfaceDark = Color(0xFF021217),
    surfaceElevated = Color(0xFF051D24),
    borderSubtle = Color(0xFF0B333F),
    borderProminent = Color(0xFFFFD200),
    textPrimary = Color(0xFFE0F7FA),
    textSecondary = Color(0xFF80DEEA),
    textTertiary = Color(0xFF4DD0E1),
    textDisabled = Color(0xFF456B75),
    accent = Color(0xFFFFD200)
)

val AirdoLightPalette = SpaceXColorPalette(
    isDark = false,
    bg = Color(0xFFF5FCFF),
    surfaceDark = Color(0xFFE1F5FE),
    surfaceElevated = Color(0xFFFFF9C4),
    borderSubtle = Color(0xFFB2EBF2),
    borderProminent = Color(0xFF0097A7),
    textPrimary = Color(0xFF004D40),
    textSecondary = Color(0xFF00838F),
    textTertiary = Color(0xFF4DD0E1),
    textDisabled = Color(0xFFB0BEC5),
    accent = Color(0xFFE6A800)
)

val JalSpaceXPalette = SpaceXColorPalette(
    isDark = true,
    bg = Color(0xFF080000),
    surfaceDark = Color(0xFF120303),
    surfaceElevated = Color(0xFF1E0808),
    borderSubtle = Color(0xFF401010),
    borderProminent = Color(0xFFD6001C),
    textPrimary = Color(0xFFFFEEEE),
    textSecondary = Color(0xFFFF7A7A),
    textTertiary = Color(0xFFD36868),
    textDisabled = Color(0xFF7A3030),
    accent = Color(0xFFE60012)
)

val JalLightPalette = SpaceXColorPalette(
    isDark = false,
    bg = Color(0xFFFFFDFC),
    surfaceDark = Color(0xFFFBEBEB),
    surfaceElevated = Color(0xFFFFEBEE),
    borderSubtle = Color(0xFFFFCDD2),
    borderProminent = Color(0xFFD6001C),
    textPrimary = Color(0xFF61000A),
    textSecondary = Color(0xFFB71C1C),
    textTertiary = Color(0xFFE57373),
    textDisabled = Color(0xFFCFD8DC),
    accent = Color(0xFFE60012)
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
