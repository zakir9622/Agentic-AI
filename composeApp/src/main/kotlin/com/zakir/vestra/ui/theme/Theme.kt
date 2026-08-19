package com.zakir.vestra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Spatial Material 3 elevation tokens (dp). */
object SpatialElevation {
    const val Surface = 1f
    const val Raised = 6f
    const val Floating = 12f
    const val GlassOverlay = 3f
}

object VestraColors {
    val Canvas = Color(0xFFF5F3F0)
    val Surface = Color(0xFFFAFAF8)
    val SurfaceRaised = Color(0xFFFFFFFF)
    val SurfaceFloating = Color(0xFFF0EDE8)
    val Ink = Color(0xFF1A1816)
    val InkMuted = Color(0xFF6B6560)
    val Accent = Color(0xFF2D6A4F)
    val AccentSoft = Color(0xFF40916C)
    val GlassBorder = Color(0x1A000000)
    val Danger = Color(0xFFE5484D)

    // Generation stage (dark monitor)
    val AtelierCanvas = Color(0xFF0E0E10)
    val AtelierContainer = Color(0xFF1A1A1E)
    val Ivory = Color(0xFFF4EFE6)
    val IvoryMuted = Color(0xFFA8A29A)
}

private val SpatialScheme = lightColorScheme(
    primary = VestraColors.Accent,
    onPrimary = Color.White,
    primaryContainer = VestraColors.AccentSoft,
    onPrimaryContainer = Color.White,
    secondary = VestraColors.InkMuted,
    onSecondary = VestraColors.Surface,
    background = VestraColors.Canvas,
    onBackground = VestraColors.Ink,
    surface = VestraColors.Surface,
    onSurface = VestraColors.Ink,
    surfaceVariant = VestraColors.SurfaceFloating,
    onSurfaceVariant = VestraColors.InkMuted,
    surfaceContainer = VestraColors.SurfaceRaised,
    surfaceContainerHigh = VestraColors.SurfaceFloating,
    surfaceContainerHighest = VestraColors.SurfaceFloating,
    outline = VestraColors.GlassBorder,
    error = VestraColors.Danger,
)

@Composable
fun VestraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SpatialScheme,
        typography = VestraTypography,
        content = content,
    )
}
