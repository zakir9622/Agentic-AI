package com.zakir.vestra.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.LocalVestraPalette
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.SpatialElevation
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.rememberReduceMotion

/**
 * Full-screen aurora mesh behind every screen's content.
 *
 * Five soft radial blobs on the violet→magenta→teal ramp, drifting on independent phases and
 * overlapping so their edges dissolve into each other. That overlap is the point: frosted glass
 * only reads as glass when there is something varied behind it to blur, and the previous version
 * — two accent orbs on a near-black ground under 95%-opaque cards — gave the blur nothing to work
 * with. The palette's glass fills are translucent now so this shows through them.
 *
 * Every blob's motion is gated on [rememberReduceMotion]; with it on, the mesh renders as a still
 * composition rather than freezing mid-drift.
 */
@Composable
fun SpatialBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val infinite = rememberInfiniteTransition(label = "aurora")

    /** One drift channel. Independent periods keep the blobs from pulsing in lockstep. */
    @Composable
    fun drift(periodMs: Int, range: Float, label: String): Float {
        val v by infinite.animateFloat(
            initialValue = if (reduceMotion) 0f else -range,
            targetValue = if (reduceMotion) 0f else range,
            animationSpec = infiniteRepeatable(
                animation = tween(if (reduceMotion) 1 else periodMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = label,
        )
        return v
    }

    val d1 = drift(11_000, 40f, "d1")
    val d2 = drift(14_500, 52f, "d2")
    val d3 = drift(9_500, 34f, "d3")
    val breathe by infinite.animateFloat(
        initialValue = if (reduceMotion) 1f else 0.92f,
        targetValue = if (reduceMotion) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 1 else 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val palette = LocalVestraPalette.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VestraColors.Canvas),
    ) {
        // Blob alpha is much lower in light mode: the same strength that reads as a rich aurora
        // on a deep indigo ground turns a pale lilac canvas muddy.
        val strength = if (palette.isDark) 1f else 0.42f
        AuroraBlob(Alignment.TopStart, (-70).dp + d1.dp, (-90).dp + d3.dp, 340.dp * breathe, palette.accent, strength)
        AuroraBlob(Alignment.TopEnd, 60.dp + d2.dp, (-40).dp + d1.dp, 300.dp * breathe, palette.modalityAudio, strength)
        AuroraBlob(Alignment.CenterStart, (-100).dp + d3.dp, (-30).dp + d2.dp, 380.dp * breathe, palette.accentSoft, strength)
        AuroraBlob(Alignment.CenterEnd, 90.dp + d1.dp, 60.dp + d3.dp, 320.dp * breathe, palette.saffronDeep, strength * 0.8f)
        AuroraBlob(Alignment.BottomStart, (-40).dp + d2.dp, 80.dp + d1.dp, 360.dp * breathe, palette.modalityAudio, strength * 0.9f)

        // A vertical scrim keeps text legible over whatever the mesh happens to be doing at the
        // top and bottom edges, where bars and composers sit.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VestraColors.Canvas.copy(alpha = if (palette.isDark) 0.40f else 0.30f),
                            Color.Transparent,
                            VestraColors.Canvas.copy(alpha = if (palette.isDark) 0.50f else 0.40f),
                        ),
                    ),
                ),
        )
        content()
    }
}

/** One soft radial blob of the aurora mesh. Edges fade fully to transparent so blobs blend. */
@Composable
private fun BoxScope.AuroraBlob(
    align: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    size: Dp,
    color: Color,
    strength: Float,
) {
    Box(
        Modifier
            .align(align)
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.78f * strength),
                        color.copy(alpha = 0.30f * strength),
                        Color.Transparent,
                    ),
                ),
                shape = CircleShape,
            ),
    )
}

/**
 * Frosted glass card — semi-transparent fill, highlight border, spatial shadow.
 *
 * A clickable card gets a subtle press-lift (scale down ~3%, spring back on release) —
 * lookbookweb's `press-3d`/`lift-3d` micro-interaction language, ported at Compose-native
 * cost rather than a full 3D perspective tilt. Skipped entirely when the user has reduced
 * motion enabled (`rememberReduceMotion()`), same as every other animation in this app.
 */
/**
 * Provides [VestraColors.Ink] as the content color for a card's children.
 *
 * Compose's default `LocalContentColor` is black, and none of these glass shells are Material
 * `Surface`s, so a `Text` that omits `color` renders black regardless of theme. Rather than
 * requiring every call site to remember, the shells supply it.
 */
@Composable
private fun InkContent(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides VestraColors.Ink,
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    val glassFill = VestraColors.GlassFill
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && pressed && !reduceMotion) 0.97f else 1f,
        label = "glassCardPressScale",
    )
    val base = modifier
        .fillMaxWidth()
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .then(
            if (elevation > 0.dp) {
                Modifier.graphicsLayer {
                    // Soft elevation; avoid ambient/spot shadow colors that blank on some GPU paths.
                    shadowElevation = elevation.toPx().coerceAtMost(12f)
                }
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(glassFill)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    VestraColors.GlassHighlight,
                    VestraColors.GlassBorder,
                ),
            ),
            shape = shape,
        )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = base,
            color = Color.Transparent,
            shape = shape,
            interactionSource = interactionSource,
        ) {
            InkContent { Column(Modifier.padding(SpacingTokens.section), content = content) }
        }
    } else {
        InkContent { Column(base.padding(SpacingTokens.section), content = content) }
    }
}

/**
 * Exact-match of lookbookweb's `solid-card` utility (`styles.css:259-263`): same border/shadow
 * treatment as [GlassCard] but an opaque fill (`--color-card`, not the translucent glass mix) —
 * for dense reading surfaces that still want the card rim + depth without content showing
 * through (chat bubbles, transcript boxes). See
 * docs/plans/lookbookweb-exact-ui-parity/PLAN.md A3.
 */
@Composable
fun SolidCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && pressed && !reduceMotion) 0.97f else 1f,
        label = "solidCardPressScale",
    )
    val base = modifier
        .fillMaxWidth()
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .clip(shape)
        .background(VestraColors.SurfaceRaised)
        .border(width = 1.dp, color = VestraColors.GlassBorder, shape = shape)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = base,
            color = Color.Transparent,
            shape = shape,
            interactionSource = interactionSource,
        ) {
            Column(Modifier.padding(SpacingTokens.section), content = content)
        }
    } else {
        Column(base.padding(SpacingTokens.section), content = content)
    }
}

@Composable
fun GlassSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Bottom, not Center. Centring aligned the back arrow to the midpoint of a two-line
        // block, which left the eyebrow hanging above it with empty space to its left on every
        // sub-page in the app. Anchoring both to the baseline of the title puts the arrow beside
        // the word it goes back from.
        verticalAlignment = Alignment.Bottom,
    ) {
        navigation()
        Column(Modifier.weight(1f).padding(bottom = 2.dp)) {
            if (subtitle != null) {
                // Bounded for the same reason the title is: an over-long subtitle wrapped to a
                // second line at 360dp and shoved the screen title down past the back arrow.
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            // Explicit, not inherited. Nothing in this component tree provides a content color,
            // so an unset `Text` falls back to `LocalContentColor`'s default of black — which
            // rendered every screen title black-on-black in dark mode. It went unnoticed because
            // the only screenshots that existed were of cards, never of a screen's own top bar.
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Row(content = actions)
    }
}

@Composable
fun GlassPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = VestraColors.Accent,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(if (active) VestraColors.GlassFillStrong else VestraColors.GlassFill)
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.5f) else VestraColors.GlassBorder,
                shape,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(Modifier.padding(end = 8.dp)) { leadingIcon() }
        } else {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (active) accent else VestraColors.InkMuted.copy(alpha = 0.4f)),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) VestraColors.Ink else VestraColors.InkMuted,
        )
    }
}

/** Saffron FilterChip — avoids Material3 purple secondaryContainer defaults. */
@Composable
fun AtelierFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        enabled = enabled,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = VestraColors.GlassFill,
            labelColor = VestraColors.Ink,
            iconColor = VestraColors.InkMuted,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = VestraColors.GlassBorder,
            selectedBorderColor = VestraColors.Accent.copy(alpha = 0.65f),
        ),
    )
}

/** Standard spatial screen shell: gradient canvas, glass top bar, scrollable body. */
@Composable
fun GlassScreen(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SpatialBackground(modifier) {
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier),
        ) {
            GlassTopBar(
                title = title,
                subtitle = subtitle,
                navigation = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = VestraColors.Ink,
                            )
                        }
                    }
                },
                actions = actions,
            )
            Spacer(Modifier.padding(top = 16.dp))
            content()
        }
    }
}

/** Elevated glass frame for image previews and model cards. */
@Composable
fun GlassImageFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(VestraColors.GlassHighlight, VestraColors.GlassBorder),
                ),
                shape,
            )
            .graphicsLayer {
                shadowElevation = 0f
            },
        content = content,
    )
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        contentColor = Color.White,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(
                            listOf(VestraColors.SaffronDeep, VestraColors.Accent, VestraColors.AccentSoft),
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                VestraColors.Accent.copy(alpha = 0.35f),
                                VestraColors.AccentSoft.copy(alpha = 0.35f),
                            ),
                        )
                    },
                    shape = shape,
                )
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

/**
 * Generate CTA that swaps to an in-button spinner + Force stop while work runs
 * (cloud jobs continue if you leave the screen; Stop cancels the ViewModel job).
 */
@Composable
fun GlassGenerateActions(
    busy: Boolean,
    generateLabel: String,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    statusText: String = "Working… you can leave this screen — tap Cancel generation to stop",
) {
    Column(modifier.fillMaxWidth()) {
        if (busy) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = VestraColors.Accent,
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            GlassSecondaryButton(text = LookbookCopy.ACTION_CANCEL_GENERATION, onClick = onStop)
        } else {
            GlassPrimaryButton(text = generateLabel, onClick = onGenerate, enabled = enabled)
        }
    }
}

/** Compact toggle chip for model assist options (pragmatic, fashion context, etc.). */
@Composable
fun GlassOptionToggle(
    text: String,
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier,
        shape = shape,
        color = if (active) VestraColors.Accent.copy(alpha = 0.18f) else VestraColors.GlassFill,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) VestraColors.Accent.copy(alpha = 0.75f) else VestraColors.GlassBorder,
        ),
        contentColor = if (active) VestraColors.Ink else VestraColors.InkMuted,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VestraColors.GlassBorder, shape),
        shape = shape,
        color = VestraColors.GlassFillStrong,
        contentColor = VestraColors.Ink,
    ) {
        Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun GlassEmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    GlassCard(modifier = modifier) {
        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.padding(top = 12.dp))
                    GlassSecondaryButton(text = actionLabel, onClick = onAction)
                }
            }
        }
    }
}

@Composable
fun GlassErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    retryLabel: String = LookbookCopy.ACTION_RETRY,
) {
    GlassCard(modifier = Modifier.testTag(TestTags.RESULT_FAILED)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = VestraColors.Danger)
        Spacer(Modifier.padding(top = 8.dp))
        Row {
            if (onRetry != null) {
                GlassSecondaryButton(
                    text = retryLabel,
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).testTag(TestTags.RESULT_RETRY_BUTTON),
                )
            }
            if (onDismiss != null) {
                if (onRetry != null) Spacer(Modifier.padding(horizontal = 6.dp))
                GlassSecondaryButton(text = "Dismiss", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GlassLoadingCard(
    message: String,
    progress: Float? = null,
    onCancel: (() -> Unit)? = null,
    accent: Color = VestraColors.Accent,
) {
    val progressLabel = progress?.let { "Generation progress ${(it.coerceIn(0f, 1f) * 100).toInt()} percent" }
        ?: "Generation in progress"
    GlassCard {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = progressLabel },
        )
        Spacer(Modifier.padding(top = 10.dp))
        if (progress != null) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = progressLabel },
                color = accent,
                trackColor = VestraColors.GlassBorder,
            )
        } else {
            androidx.compose.material3.CircularProgressIndicator(
                color = accent,
                modifier = Modifier.semantics { contentDescription = progressLabel },
            )
        }
        if (onCancel != null) {
            Spacer(Modifier.padding(top = 12.dp))
            GlassSecondaryButton(
                text = LookbookCopy.ACTION_CANCEL_GENERATION,
                onClick = onCancel,
                modifier = Modifier.testTag(TestTags.RESULT_CANCEL_BUTTON),
            )
        }
    }
}

@Composable
fun GlassTile(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RadiusTokens.md)
    Row(
        modifier
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .padding(SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
