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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability

/** Spatial Material 3 elevation tokens (dp). Prefer flat glass — shadows are GPU-risky. */
object SpatialElevation {
    const val Surface = 0f
    const val Raised = 0f
    const val Floating = 2f
    const val GlassOverlay = 0f
}

/**
 * Corner-radius scale, exact match of lookbookweb's `--radius: 1.5rem` (24px) base with its
 * `calc(var(--radius) ± N)` offsets (`styles.css:14-20`): sm = radius-12px, md = radius-8px,
 * lg = radius-4px, xl = radius, 2xl = radius+6px, 3xl = radius+12px, 4xl = radius+20px.
 * `xl2`/`xl3`/`xl4` (not `2xl`/`3xl`/`4xl` — Kotlin identifiers can't start with a digit) back
 * the larger rounding lookbookweb uses for cards (`rounded-3xl`) and the floating dock
 * (`rounded-4xl`). See docs/plans/lookbookweb-exact-ui-parity/PLAN.md A0.
 */
object RadiusTokens {
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xl2: Dp = 30.dp
    val xl3: Dp = 36.dp
    val xl4: Dp = 44.dp
}

object SpacingTokens {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val section: Dp = 18.dp
}

/**
 * Exact-match port of lookbookweb.lovable.app's design system (`zakir9622/lookbookweb`,
 * `src/styles.css`) — light airy canvas, white cards, near-black primary, one electric-blue
 * accent, per-modality brand colors. Values below are sRGB conversions of that file's OKLCH
 * tokens (CSS Color 4 OKLab->linear-sRGB matrices), not eyeballed — see
 * `docs/plans/lookbookweb-exact-ui-parity/PLAN.md` A0 for the full oklch->hex table.
 * Replaces the prior "Loom Ink" brass-on-deep-ink palette per that plan.
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
    /** Per-modality accent — Create/Image Studio. Brass-family, same as the base [accent]. */
    val modalityImage: Color,
    /** Per-modality accent — Video Studio. Warm copper shift off the brass family. */
    val modalityVideo: Color,
    /** Per-modality accent — Code Studio. Reuses the existing teal loom ([saffronDeep]). */
    val modalityCode: Color,
    /** Per-modality accent — Audio Studio. Muted dusty rose — warm, not the brand's avoided purple. */
    val modalityAudio: Color,
    val isDark: Boolean,
)

val LocalVestraPalette = staticCompositionLocalOf { LightPalette }

// lookbookweb light theme: background #F2F8FC · card #FFFFFF · foreground #111419 · accent #1F7DCF
private val LightPalette = VestraPalette(
    canvas = Color(0xFFF2F8FC),
    surface = Color(0xFFEBEDEF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceFloating = Color(0xFFEBEDEF),
    ink = Color(0xFF111419),
    inkMuted = Color(0xFF575B62),
    accent = Color(0xFF1F7DCF),
    accentSoft = Color(0xFF4E9BDB),
    accentGlow = Color(0x331F7DCF),
    // glass-border/highlight: white at 70%/85% alpha (styles.css --glass-border/--glass-highlight)
    glassFill = Color(0xF2FFFFFF),
    glassFillStrong = Color(0xFAFFFFFF),
    glassBorder = Color(0xB3FFFFFF),
    glassHighlight = Color(0xD9FFFFFF),
    glassShadow = Color(0x1A111419),
    danger = Color(0xFFD01C29),
    // atelierCanvas/atelierContainer/ivory/ivoryMuted are theme-independent by original design
    // (call sites like GenerationScreen.kt use AtelierCanvas as a fixed dark scrim behind
    // generation previews with Ivory text drawn on top, regardless of the app's light/dark
    // theme) — kept as fixed dark-canvas/light-text values in both palettes rather than tied to
    // `ink` (which flips with theme and collided with `ivory` in dark mode, making text
    // invisible — caught in code review before landing). See
    // docs/plans/lookbookweb-exact-ui-parity/PLAN.md A0.
    atelierCanvas = Color(0xFF111419),
    atelierContainer = Color(0xFF21242A),
    ivory = Color(0xFFF4F5F7),
    ivoryMuted = Color(0xFFA7ABB3),
    saffronDeep = Color(0xFF009C7B),
    silkMist = Color(0xFFEBEDEF),
    modalityImage = Color(0xFF1F7DCF),
    modalityVideo = Color(0xFFDD503F),
    modalityCode = Color(0xFF009C7B),
    modalityAudio = Color(0xFFE8179B),
    isDark = false,
)

// lookbookweb dark theme: background #0C0D11 · card #16181D · foreground #F4F5F7 · accent #6A99FF
private val DarkPalette = VestraPalette(
    canvas = Color(0xFF0C0D11),
    surface = Color(0xFF21242A),
    surfaceRaised = Color(0xFF16181D),
    surfaceFloating = Color(0xFF21242A),
    ink = Color(0xFFF4F5F7),
    inkMuted = Color(0xFFA7ABB3),
    accent = Color(0xFF6A99FF),
    accentSoft = Color(0xFF8FB2FF),
    accentGlow = Color(0x406A99FF),
    // glass-border/highlight: white at 18%/22% alpha (dark theme)
    glassFill = Color(0xF216181D),
    glassFillStrong = Color(0xF821242A),
    glassBorder = Color(0x2EFFFFFF),
    glassHighlight = Color(0x38FFFFFF),
    glassShadow = Color(0x66000000),
    danger = Color(0xFFF97066),
    // Same theme-independent fixed values as LightPalette — see the comment there.
    atelierCanvas = Color(0xFF111419),
    atelierContainer = Color(0xFF21242A),
    ivory = Color(0xFFF4F5F7),
    ivoryMuted = Color(0xFFA7ABB3),
    saffronDeep = Color(0xFF2DC5A6),
    silkMist = Color(0xFF21242A),
    modalityImage = Color(0xFF709FFF),
    modalityVideo = Color(0xFFFA8C58),
    modalityCode = Color(0xFF2DC5A6),
    modalityAudio = Color(0xFFFC65B6),
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

    /** Per-modality accents (Create/Video/Code/Audio Studio) — see [VestraPalette] docs. */
    val ModalityImage get() = active.modalityImage
    val ModalityVideo get() = active.modalityVideo
    val ModalityCode get() = active.modalityCode
    val ModalityAudio get() = active.modalityAudio

    fun modalityAccent(capability: AiCapability): Color = when (capability) {
        AiCapability.IMAGE_GEN, AiCapability.IMAGE_EDIT, AiCapability.TRY_ON -> active.modalityImage
        AiCapability.VIDEO -> active.modalityVideo
        AiCapability.CODE -> active.modalityCode
        AiCapability.AUDIO -> active.modalityAudio
    }
}

private fun VestraPalette.toScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF151F33),
        primaryContainer = Color(0xFF253659),
        onPrimaryContainer = ivory,
        secondary = inkMuted,
        onSecondary = Color(0xFF191A1B),
        secondaryContainer = Color(0xFF1E2B47),
        onSecondaryContainer = ivory,
        tertiary = saffronDeep,
        onTertiary = Color(0xFF04231C),
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
        surfaceContainerHighest = Color(0xFF2A2E36),
        outline = glassBorder,
        error = danger,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDECF8),
        onPrimaryContainer = Color(0xFF09263E),
        secondary = Color(0xFF066C59),
        onSecondary = Color.White,
        // FilterChip selected fill — matches brand-chat (--brand-chat, #009C7B family).
        secondaryContainer = Color(0xFFD9F0EB),
        onSecondaryContainer = Color(0xFF00271F),
        tertiary = saffronDeep,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFAE5E2),
        onTertiaryContainer = Color(0xFF111419),
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
