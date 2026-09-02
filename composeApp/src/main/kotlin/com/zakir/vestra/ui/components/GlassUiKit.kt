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

/**
 * Home's greeting row: avatar with presence, name, a status line, and a notifications button.
 *
 * Replaces a bare brand wordmark. The avatar and presence dot are what make the screen read as
 * *someone's* workspace rather than a generic tool.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    statusLine: String,
    online: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Trailing bell. Nullable because the header is also used inside a bar that already carries
     * its own actions — three 40dp buttons plus an avatar leave a 360dp row too little for the
     * name, which truncated it to "The Loo…". A host with its own actions passes null.
     */
    onNotifications: (() -> Unit)? = null,
    avatarColor: Color = VestraColors.Accent,
) {
    Row(
        modifier
            .fillMaxWidth()
            .testTag(TestTags.HOME_GREETING_HEADER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Box {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(avatarColor, VestraColors.ModalityAudio),
                        ),
                    )
                    .border(1.dp, VestraColors.GlassHighlight, CircleShape),
            )
            OnlineDot(
                online = online,
                size = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .border(2.dp, VestraColors.Canvas, CircleShape)
                    .padding(2.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                greeting,
                style = MaterialTheme.typography.titleLarge,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                statusLine,
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onNotifications != null) {
            GlassIconButton(
                icon = Icons.Outlined.NotificationsNone,
                contentDescription = "Notifications",
                onClick = onNotifications,
                modifier = Modifier.testTag(TestTags.HOME_NOTIFICATIONS_BUTTON),
            )
        }
    }
}

/**
 * The hero card: a question, a line of context, a primary action and one secondary icon action.
 *
 * This is the screen's thesis — it replaces an empty canvas (and, before that, a token counter)
 * with the single thing the app is for.
 */
@Composable
fun HeroPromptCard(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryIcon: ImageVector? = null,
    secondaryContentDescription: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(RadiusTokens.xl2)
    Column(
        modifier
            .fillMaxWidth()
            .testTag(TestTags.HOME_HERO_CARD)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        VestraColors.GlassFillStrong,
                        VestraColors.Accent.copy(alpha = 0.18f),
                    ),
                ),
            )
            .border(1.dp, VestraColors.GlassHighlight, shape)
            .padding(SpacingTokens.lg),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = VestraColors.Ink,
        )
        Spacer(Modifier.height(SpacingTokens.xs))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = VestraColors.InkMuted,
        )
        Spacer(Modifier.height(SpacingTokens.md))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .testTag(TestTags.HOME_HERO_PRIMARY)
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(VestraColors.Ink)
                    .clickable(onClick = onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    primaryLabel,
                    style = MaterialTheme.typography.titleMedium,
                    // Reads against `Ink`, which flips with the theme — so this must be `Canvas`,
                    // not a literal white that would vanish in light mode.
                    color = VestraColors.Canvas,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (secondaryIcon != null && onSecondary != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(RadiusTokens.md))
                        .background(VestraColors.GlassFill)
                        .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(RadiusTokens.md))
                        .clickable(onClick = onSecondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        secondaryIcon,
                        contentDescription = secondaryContentDescription,
                        tint = VestraColors.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
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

/** One capability tile: accent icon chip, title, one-line subtitle. Sized to share a row. */
@Composable
fun CapabilityTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Column(
        modifier
            .clip(shape)
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(SpacingTokens.md),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(RadiusTokens.sm))
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(RadiusTokens.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(SpacingTokens.sm))
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

/** Chat header: back, centred title with presence, overflow. */
@Composable
fun ChatStatusHeader(
    title: String,
    online: Boolean,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    statusLabel: String = if (online) "ONLINE" else "OFFLINE",
) {
    Row(
        modifier
            .fillMaxWidth()
            .testTag(TestTags.CHAT_STATUS_HEADER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        GlassIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OnlineDot(online, size = 6.dp)
                Spacer(Modifier.width(SpacingTokens.xxs))
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (online) VestraColors.SaffronDeep else VestraColors.InkMuted,
                    maxLines = 1,
                )
            }
        }
        GlassIconButton(Icons.Outlined.MoreHoriz, "More options", onMenu)
    }
}

/** Small "who is speaking" chip above an assistant message. */
@Composable
fun AuthorChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xxs + 2.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(VestraColors.Accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = VestraColors.Accent, modifier = Modifier.size(12.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            .background(VestraColors.Canvas.copy(alpha = 0.55f))
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
