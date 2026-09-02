package com.zakir.vestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * The glassmorphism component set the redesign is built from.
 *
 * These are the pieces the previous UI had no equivalent for — a greeting header with presence,
 * a hero prompt card, capability tiles, history rows, a chat header with status, author chips,
 * and syntax-highlighted code inside a message bubble. They live together because they share one
 * surface language (translucent fill, hairline highlight border, generous radius) that only holds
 * together if it is defined in one place; scattering it across screens is how the old UI ended up
 * with three different card treatments.
 */

/** Small uppercase outlined pill — the "NEXT-GEN INTELLIGENCE" eyebrow above a hero. */
@Composable
fun GlassBadgePill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, CircleShape)
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.xs),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Large rounded-square glass tile holding a brand or feature glyph. */
@Composable
fun GlassAppMark(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 108.dp,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(RadiusTokens.xl3)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        VestraColors.Accent.copy(alpha = 0.38f),
                        VestraColors.GlassFill,
                    ),
                ),
            )
            .border(1.dp, VestraColors.GlassHighlight, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = VestraColors.Ink,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

/**
 * Overlapping avatar stack plus a trust line.
 *
 * [avatarColors] rather than image URLs: this renders below a first-run CTA, where there is no
 * network fetch worth blocking the screen on and no real user photos to show.
 */
@Composable
fun SocialProofRow(
    avatarColors: List<Color>,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row {
            avatarColors.forEachIndexed { index, color ->
                Box(
                    Modifier
                        // Negative offset on all but the first produces the overlap.
                        .offset(x = if (index == 0) 0.dp else (-10 * index).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, VestraColors.Canvas, CircleShape),
                )
            }
        }
        Spacer(Modifier.width(SpacingTokens.xs))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Presence dot. Green when online, muted otherwise. */
@Composable
fun OnlineDot(online: Boolean, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (online) VestraColors.SaffronDeep else VestraColors.InkMuted),
    )
}

/** Circular glass icon button — top-bar actions, composer affordances. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = VestraColors.Ink,
) {
    Box(
        modifier
            .size(ControlTokens.iconButton)
            .clip(CircleShape)
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Section label with an optional trailing action, e.g. "CAPABILITIES … Explore All". */
@Composable
fun SectionHeaderRow(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.Accent,
                maxLines = 1,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

/** One row of recent activity: glyph, title, subtitle, relative time. */
@Composable
fun HistoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    timeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(RadiusTokens.md)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VestraColors.GlassFillStrong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = VestraColors.InkMuted, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
            maxLines = 1,
        )
    }
}

/**
 * Syntax-highlighted code inside a message bubble.
 *
 * Scrolls horizontally in its own container rather than wrapping: a wrapped code line reads as a
 * syntax error to anyone skimming, and the bubble is far narrower than most snippets.
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: String? = null,
) {
    val shape = RoundedCornerShape(RadiusTokens.sm)
    val theme = CodeTheme(
        plain = VestraColors.Ink,
        keyword = VestraColors.ModalityAudio,
        string = VestraColors.SaffronDeep,
        number = VestraColors.AccentSoft,
        comment = VestraColors.InkMuted,
        property = VestraColors.Accent,
        punctuation = VestraColors.InkMuted,
    )
    Column(
        modifier
            .fillMaxWidth()
            .testTag(TestTags.CHAT_CODE_BLOCK)
            .clip(shape)
            // `Surface`, not `Canvas`: in light mode the canvas is a pale lilac within a few
            // percent of the white bubble behind it, so a canvas-tinted block was delineated
            // only by its border. `Surface` is a real step down from the bubble in both palettes.
            .background(VestraColors.Surface.copy(alpha = 0.85f))
            .border(1.dp, VestraColors.GlassBorder, shape)
            .padding(SpacingTokens.sm),
    ) {
        if (language != null) {
            Text(
                language.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
            )
            Spacer(Modifier.height(SpacingTokens.xxs))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                CodeHighlighter.highlight(code.trimEnd(), theme),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                softWrap = false,
            )
        }
    }
}
