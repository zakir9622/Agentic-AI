package com.zakir.vestra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Spatial Material 3 elevation tokens (dp). Prefer flat glass — shadows are GPU-risky. */
object SpatialElevation {
    const val Surface = 0f
    const val Raised = 0f
    const val Floating = 2f
    const val GlassOverlay = 0f
}

/**
 * Midnight Saffron atelier — Lookbook-specific fashion palette.
 * Warm saffron dye + deep ink silk. Avoids purple, cream+terracotta, and acid-neon defaults.
 */
@Immutable
data class VestraPalette(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceFloating: Color,
    val ink: Color,
    val inkMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentGlow: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val glassShadow: Color,
    val danger: Color,
    val atelierCanvas: Color,
    val atelierContainer: Color,
    val ivory: Color,
    val ivoryMuted: Color,
    val saffronDeep: Color,
    val silkMist: Color,
    val isDark: Boolean,
)

val LocalVestraPalette = staticCompositionLocalOf { LightPalette }

private val LightPalette = VestraPalette(
    canvas = Color(0xFFE8EEF2),
    surface = Color(0xFFF5F8FA),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceFloating = Color(0xFFD9E3EA),
    ink = Color(0xFF12181E),
    inkMuted = Color(0xFF5C6B76),
    accent = Color(0xFFB8792E),
    accentSoft = Color(0xFFD4A054),
    accentGlow = Color(0x33D4A054),
    glassFill = Color(0xF2F7FBFC),
    glassFillStrong = Color(0xFAFFFFFF),
    glassBorder = Color(0x66D4A054),
    glassHighlight = Color(0xCCFFFFFF),
    glassShadow = Color(0x1A12181E),
    danger = Color(0xFFC2410C),
    atelierCanvas = Color(0xFF0C0A09),
    atelierContainer = Color(0xFF171311),
    ivory = Color(0xFFF7F1E8),
    ivoryMuted = Color(0xFFB5A899),
    saffronDeep = Color(0xFF8B5A1E),
    silkMist = Color(0xFFD9E3EA),
    isDark = false,
)

private val DarkPalette = VestraPalette(
    canvas = Color(0xFF08070A),
    surface = Color(0xFF121014),
    surfaceRaised = Color(0xFF1C1917),
    surfaceFloating = Color(0xFF0E0C10),
    ink = Color(0xFFF3EDE4),
    inkMuted = Color(0xFFA89B8C),
    accent = Color(0xFFE0B45C),
    accentSoft = Color(0xFFF0C97A),
    accentGlow = Color(0x40E0B45C),
    glassFill = Color(0xF21C1917),
    glassFillStrong = Color(0xF828241F),
    glassBorder = Color(0x66E0B45C),
    glassHighlight = Color(0x33FFFFFF),
    glassShadow = Color(0x66000000),
    danger = Color(0xFFFB923C),
    atelierCanvas = Color(0xFF050408),
    atelierContainer = Color(0xFF14110F),
    ivory = Color(0xFFF3EDE4),
    ivoryMuted = Color(0xFFA89B8C),
    saffronDeep = Color(0xFFC9893A),
    silkMist = Color(0xFF3D342C),
    isDark = true,
)

/**
 * Bridge for existing call sites. [install] is called from [VestraTheme] on the
 * main thread before content composes — safe for Compose UI usage.
 */
object VestraColors {
    @Volatile
    private var active: VestraPalette = LightPalette

    fun install(palette: VestraPalette) {
        active = palette
    }

    val Canvas get() = active.canvas
    val Surface get() = active.surface
    val SurfaceRaised get() = active.surfaceRaised
    val SurfaceFloating get() = active.surfaceFloating
    val Ink get() = active.ink
    val InkMuted get() = active.inkMuted
    val Accent get() = active.accent
    val AccentSoft get() = active.accentSoft
    val AccentGlow get() = active.accentGlow
    val GlassFill get() = active.glassFill
    val GlassFillStrong get() = active.glassFillStrong
    val GlassBorder get() = active.glassBorder
    val GlassHighlight get() = active.glassHighlight
    val GlassShadow get() = active.glassShadow
    val Danger get() = active.danger
    val AtelierCanvas get() = active.atelierCanvas
    val AtelierContainer get() = active.atelierContainer
    val Ivory get() = active.ivory
    val IvoryMuted get() = active.ivoryMuted
    val SaffronDeep get() = active.saffronDeep
    val SilkMist get() = active.silkMist
}

private fun VestraPalette.toScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF1A1208),
        primaryContainer = accentSoft.copy(alpha = 0.22f),
        onPrimaryContainer = ivory,
        secondary = Color(0xFFC4A484),
        onSecondary = Color(0xFF1A1208),
        secondaryContainer = accentSoft.copy(alpha = 0.28f),
        onSecondaryContainer = ivory,
        tertiary = saffronDeep,
        onTertiary = Color(0xFF1A1208),
        tertiaryContainer = saffronDeep.copy(alpha = 0.35f),
        onTertiaryContainer = ivory,
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceFloating,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = Color(0xFF2A241E),
        outline = glassBorder,
        error = danger,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF3E2C4),
        onPrimaryContainer = Color(0xFF1A1208),
        secondary = Color(0xFF8B6B4A),
        onSecondary = Color.White,
        // FilterChip selected fill — must be saffron, not M3 purple defaults.
        secondaryContainer = Color(0xFFEFD9B0),
        onSecondaryContainer = Color(0xFF4A3214),
        tertiary = saffronDeep,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF0E0C8),
        onTertiaryContainer = Color(0xFF4A3214),
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceFloating,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = Color.White,
        outline = glassBorder,
        error = danger,
    )
}

@Composable
fun VestraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    VestraColors.install(palette)
    CompositionLocalProvider(LocalVestraPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toScheme(),
            typography = VestraTypography,
            content = content,
        )
    }
}
