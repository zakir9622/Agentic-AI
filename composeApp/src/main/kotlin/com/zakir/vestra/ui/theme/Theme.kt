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

/**
 * Interactive-control heights. Before these existed the composer's model chip measured ~30dp
 * against a 48dp send orb in the same centered row, leaving ~9dp of dead air on either side
 * of every chip; the home top bar mixed a 26dp brand mark, a ~29dp chip and 36dp icon buttons.
 * Controls that sit on one row must share one height.
 */
object ControlTokens {
    /** Pills, chips and dropdown anchors. */
    val chip: Dp = 40.dp
    /** Circular icon buttons in bars. */
    val iconButton: Dp = 40.dp
    /** The composer's send/stop orb — one step up from a chip so it reads as primary. */
    val orb: Dp = 44.dp
    /** Status/selection dot used inside chips and list rows. */
    val dot: Dp = 8.dp
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
    /**
     * Status green — a state that is fine.
     *
     * Deliberately not an alias of [modalityCode] or [saffronDeep]. Those two are the same
     * teal, so binding "good" to either made a good/warning pair render as one colour, and
     * binding a *status* to a *modality* colour would have meant "Ready" and "this is the
     * Code studio" sharing a swatch.
     */
    val success: Color,
    /** Status amber — degraded but usable. The palette had no warm mid-tone before this. */
    val warning: Color,
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

/**
 * Violet-aurora light theme — the same hue family as [DarkPalette], lifted onto a pale lilac
 * ground so the two read as one system rather than two unrelated designs.
 */
private val LightPalette = VestraPalette(
    canvas = Color(0xFFF6F3FE),
    surface = Color(0xFFEDE8FB),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceFloating = Color(0xFFEDE8FB),
    ink = Color(0xFF1B1430),
    inkMuted = Color(0xFF5F5680),
    accent = Color(0xFF7C3AED),
    accentSoft = Color(0xFF9B6BF2),
    accentGlow = Color(0x337C3AED),
    // These were a literal port of lookbookweb's `--glass-*` tokens: white at 95%/98% fill and
    // white at 70%/85% for the border and highlight. That works on the *web* app, whose glass
    // sits over a tinted canvas — but this app draws glass on white cards
    // (`surfaceContainer = Color.White` below), where white-on-white is nothing at all.
    // Screenshot renders showed the consequence plainly: `GlassSecondaryButton` rendered as bare
    // centred text with no button, and unconfigured service tiles floated with neither fill nor
    // rim beside bordered ones. The fill is now a step off white and the border a low-alpha ink,
    // so every glass surface has an edge in both palettes. Dark mode is untouched — its
    // white-alpha values were always visible against a dark ground.
    glassFill = Color(0xCCFFFFFF),
    glassFillStrong = Color(0xE6FFFFFF),
    glassBorder = Color(0x1F1B1430),
    glassHighlight = Color(0x0D1B1430),
    glassShadow = Color(0x1A111419),
    danger = Color(0xFFD01C29),
    success = Color(0xFF0F7A52),
    warning = Color(0xFFB45309),
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
    saffronDeep = Color(0xFF0D9488),
    silkMist = Color(0xFFEDE8FB),
    modalityImage = Color(0xFF7C3AED),
    modalityVideo = Color(0xFFEA580C),
    modalityCode = Color(0xFF0D9488),
    modalityAudio = Color(0xFFDB2777),
    isDark = false,
)

/**
 * Violet-aurora dark theme. The canvas is a deep indigo rather than near-black so the mesh
 * gradient behind the glass has something to sit on, and the glass fills are **translucent**
 * (0x8C / 0x66 alpha, not the old 0xF2 near-opaque) — frosted glass only reads as glass when the
 * colour behind it comes through. The old dark palette was a near-black ground under 95%-opaque
 * cards, which is a dark theme, not glassmorphism.
 */
private val DarkPalette = VestraPalette(
    canvas = Color(0xFF130C26),
    surface = Color(0xFF1A1430),
    surfaceRaised = Color(0xFF171129),
    surfaceFloating = Color(0xFF221A3D),
    ink = Color(0xFFF3EEFF),
    inkMuted = Color(0xFFB4A9D4),
    accent = Color(0xFFA78BFA),
    accentSoft = Color(0xFFC4B5FD),
    accentGlow = Color(0x4DA78BFA),
    // Translucent by design — these sit over the aurora mesh and must let it through.
    glassFill = Color(0x8C2A2150),
    glassFillStrong = Color(0xA6332764),
    glassBorder = Color(0x33FFFFFF),
    glassHighlight = Color(0x45FFFFFF),
    glassShadow = Color(0x73000000),
    danger = Color(0xFFFF8A93),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    // Same theme-independent fixed values as LightPalette — see the comment there.
    atelierCanvas = Color(0xFF111419),
    atelierContainer = Color(0xFF21242A),
    ivory = Color(0xFFF4F5F7),
    ivoryMuted = Color(0xFFA7ABB3),
    saffronDeep = Color(0xFF2DD4BF),
    silkMist = Color(0xFF221A3D),
    modalityImage = Color(0xFFA78BFA),
    modalityVideo = Color(0xFFFB923C),
    modalityCode = Color(0xFF2DD4BF),
    modalityAudio = Color(0xFFF472B6),
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
    val Success get() = active.success
    val Warning get() = active.warning
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
