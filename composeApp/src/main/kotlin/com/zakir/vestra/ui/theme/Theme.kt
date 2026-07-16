package com.zakir.vestra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Fashion editorial film" palette — near-black stage, champagne-gold accent, ivory type.
object VestraColors {
    val Stage = Color(0xFF0A0A0C)
    val StageElevated = Color(0xFF141418)
    val StageHighlight = Color(0xFF1E1E24)
    val Champagne = Color(0xFFD8B26E)
    val ChampagneDeep = Color(0xFFB08D4B)
    val Ivory = Color(0xFFF4EFE6)
    val IvoryMuted = Color(0xFFA8A29A)
    val Danger = Color(0xFFE5484D)

    // Light theme: gallery-white with the same gold thread.
    val Gallery = Color(0xFFFAF7F2)
    val GalleryElevated = Color(0xFFFFFFFF)
    val Ink = Color(0xFF17161A)
    val InkMuted = Color(0xFF6D675E)
}

private val DarkScheme = darkColorScheme(
    primary = VestraColors.Champagne,
    onPrimary = VestraColors.Stage,
    primaryContainer = VestraColors.ChampagneDeep,
    onPrimaryContainer = VestraColors.Ivory,
    secondary = VestraColors.IvoryMuted,
    onSecondary = VestraColors.Stage,
    background = VestraColors.Stage,
    onBackground = VestraColors.Ivory,
    surface = VestraColors.Stage,
    onSurface = VestraColors.Ivory,
    surfaceVariant = VestraColors.StageElevated,
    onSurfaceVariant = VestraColors.IvoryMuted,
    surfaceContainer = VestraColors.StageElevated,
    surfaceContainerHigh = VestraColors.StageHighlight,
    outline = VestraColors.IvoryMuted.copy(alpha = 0.4f),
    error = VestraColors.Danger,
)

private val LightScheme = lightColorScheme(
    primary = VestraColors.ChampagneDeep,
    onPrimary = VestraColors.Gallery,
    primaryContainer = VestraColors.Champagne,
    onPrimaryContainer = VestraColors.Ink,
    secondary = VestraColors.InkMuted,
    onSecondary = VestraColors.Gallery,
    background = VestraColors.Gallery,
    onBackground = VestraColors.Ink,
    surface = VestraColors.Gallery,
    onSurface = VestraColors.Ink,
    surfaceVariant = VestraColors.GalleryElevated,
    onSurfaceVariant = VestraColors.InkMuted,
    surfaceContainer = VestraColors.GalleryElevated,
    surfaceContainerHigh = VestraColors.GalleryElevated,
    outline = VestraColors.InkMuted.copy(alpha = 0.4f),
    error = VestraColors.Danger,
)

@Composable
fun VestraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = VestraTypography,
        content = content,
    )
}
