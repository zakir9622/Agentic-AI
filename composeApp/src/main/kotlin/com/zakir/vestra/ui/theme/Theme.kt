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
 * Futuristic atelier tokens — pearl day / graphite night with luminous teal glass.
 * Avoids purple gradients, cream+terracotta, and broadsheet looks.
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
    val isDark: Boolean,
)

val LocalVestraPalette = staticCompositionLocalOf { LightPalette }

private val LightPalette = VestraPalette(
    canvas = Color(0xFFE6EEF2),
    surface = Color(0xFFF4F8FA),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceFloating = Color(0xFFDCE7ED),
    ink = Color(0xFF0F1720),
    inkMuted = Color(0xFF5B6B78),
    accent = Color(0xFF0F766E),
    accentSoft = Color(0xFF14B8A6),
    accentGlow = Color(0x3314B8A6),
    // Near-opaque so body text stays ≥4.5:1 over glass; keep a soft edge only.
    glassFill = Color(0xF2F7FBFC),
    glassFillStrong = Color(0xFAFFFFFF),
    glassBorder = Color(0x6614B8A6),
    glassHighlight = Color(0xCCFFFFFF),
    glassShadow = Color(0x1A0F1720),
    danger = Color(0xFFE11D48),
    atelierCanvas = Color(0xFF0B0F14),
    atelierContainer = Color(0xFF151A22),
    ivory = Color(0xFFF0F7FA),
    ivoryMuted = Color(0xFF94A3B8),
    isDark = false,
)

private val DarkPalette = VestraPalette(
    canvas = Color(0xFF070A0E),
    surface = Color(0xFF10151C),
    surfaceRaised = Color(0xFF1A222C),
    surfaceFloating = Color(0xFF0C1118),
    ink = Color(0xFFE8F1F5),
    inkMuted = Color(0xFF9AABB8),
    accent = Color(0xFF2DD4BF),
    accentSoft = Color(0xFF5EEAD4),
    accentGlow = Color(0x402DD4BF),
    // Opaque enough for WCAG text on glass; menus use surfaceRaised separately.
    glassFill = Color(0xF21A222C),
    glassFillStrong = Color(0xF8252E3A),
    glassBorder = Color(0x662DD4BF),
    glassHighlight = Color(0x33FFFFFF),
    glassShadow = Color(0x66000000),
    danger = Color(0xFFFB7185),
    atelierCanvas = Color(0xFF05070A),
    atelierContainer = Color(0xFF121820),
    ivory = Color(0xFFE8F1F5),
    ivoryMuted = Color(0xFF94A3B8),
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
}

private fun VestraPalette.toScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF042F2E),
        primaryContainer = accentSoft.copy(alpha = 0.25f),
        onPrimaryContainer = ivory,
        secondary = Color(0xFFC4A484),
        onSecondary = Color(0xFF1A1208),
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceFloating,
        onSurfaceVariant = inkMuted,
        // Opaque containers — Material menus/dialogs use these; never translucent glass.
        surfaceContainerLowest = surface,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = Color(0xFF243040),
        outline = glassBorder,
        error = danger,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accentSoft,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFB8956C),
        onSecondary = Color.White,
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceFloating,
        onSurfaceVariant = inkMuted,
        // Opaque containers — Material menus/dialogs use these; never translucent glass.
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
