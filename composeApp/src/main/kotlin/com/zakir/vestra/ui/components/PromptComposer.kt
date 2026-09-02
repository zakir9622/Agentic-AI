package com.zakir.vestra.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Prompt-first floating composer — model pill, assist count, send/stop.
 * Pattern adapted from modern generative shells; copy and brand are Lookbook.
 *
 * Two rules this component learned the hard way:
 *
 * 1. **[modelLabel] is a name, never a sentence.** It used to receive
 *    `GenerativeViewModel.preflightLabel()`, which returns the *blocked reason* when cloud is
 *    gated — a 140-character consent paragraph. Rendered in a one-line chip that came out as
 *    "Pick a cloud model in the model pi…", so the control that is supposed to name the
 *    selected model instead showed a truncated error. The reason now has its own slot,
 *    [blockedReason], rendered as a full-width hint row above the actions.
 * 2. **One rim per surface.** The container, the text field and each chip all used to draw
 *    their own 1dp border at three different radii inside 14dp of padding. The field is now
 *    borderless and recessed by fill instead ([VestraColors.Canvas] against the container's
 *    [VestraColors.GlassFillStrong]), so depth reads from tone, not from stacked outlines.
 */
@Composable
fun PromptComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
    modelLabel: String,
    onModelClick: (() -> Unit)? = null,
    assistCount: Int = 0,
    onAssistsClick: (() -> Unit)? = null,
    busy: Boolean,
    // The model is cold-loading — not generating yet, nothing to cancel. Shown as a spinner on
    // the send button itself rather than a separate status card, so loading/generating/idle are
    // all one place instead of three.
    loading: Boolean = false,
    enabled: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = VestraColors.Accent,
    placeholder: String = "Describe the look…",
    /**
     * Why generation is currently gated, if it is — e.g. "Add a free API key in Settings".
     * Shown as its own hint row; never folded into [modelLabel].
     */
    blockedReason: String? = null,
    referenceUri: String? = null,
    onAddReference: (() -> Unit)? = null,
    onClearReference: (() -> Unit)? = null,
    assistToggles: (@Composable () -> Unit)? = null,
    quickPrompts: List<QuickPromptItem> = emptyList(),
    onSelectQuickPrompt: ((String) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(RadiusTokens.xl2)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, shape)
            // The reference row only exists in Image mode, so the composer's height changes
            // when the modality chip changes. Animating it keeps that from reading as a jump.
            .animateContentSize()
            .padding(SpacingTokens.sm),
    ) {
        if (quickPrompts.isNotEmpty() && onSelectQuickPrompt != null && !busy) {
            QuickPromptCarousel(
                prompts = quickPrompts,
                onSelectPrompt = onSelectQuickPrompt,
                enabled = enabled,
            )
        }

        if (onAddReference != null || referenceUri != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                referenceUri?.let { uri ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(RadiusTokens.md))
                            .background(VestraColors.Canvas)
                            .padding(SpacingTokens.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Reference",
                            modifier = Modifier
                                .size(44.dp)
                                .testTag(TestTags.REFERENCE_IMAGE_THUMB)
                                .clip(RoundedCornerShape(RadiusTokens.sm)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(SpacingTokens.xs))
                        Text(
                            "Reference attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = VestraColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(SpacingTokens.xxs))
                        if (onClearReference != null) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(VestraColors.GlassFillStrong)
                                    .clickable(onClick = onClearReference),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Remove reference",
                                    tint = VestraColors.InkMuted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(SpacingTokens.xxs))
                        }
                    }
                }
                if (onAddReference != null && referenceUri == null) {
                    Row(
                        Modifier
                            .testTag(TestTags.ADD_REFERENCE_BUTTON)
                            .heightIn(min = ControlTokens.chip)
                            .clip(RoundedCornerShape(50))
                            .background(VestraColors.Canvas)
                            .clickable(enabled = !busy, onClick = onAddReference)
                            .padding(horizontal = SpacingTokens.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xxs + 2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Add reference image",
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Attach Reference",
                            style = MaterialTheme.typography.labelSmall,
                            color = VestraColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(SpacingTokens.xs))
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.PROMPT_INPUT),
            enabled = !busy,
            minLines = 2,
            maxLines = 5,
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VestraColors.InkMuted,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = if (prompt.isNotBlank() && !busy) {
                {
                    IconButton(onClick = { onPromptChange("") }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear prompt",
                            tint = VestraColors.InkMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else null,
            // Borderless by design — see the KDoc's "one rim per surface". The field reads as
            // recessed because `Canvas` sits a step below the container's `GlassFillStrong` in
            // both palettes, not because it is outlined.
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = VestraColors.Canvas,
                unfocusedContainerColor = VestraColors.Canvas,
                disabledContainerColor = VestraColors.Canvas,
                cursorColor = accent,
            ),
            shape = RoundedCornerShape(RadiusTokens.lg),
        )

        if (assistToggles != null) {
            Spacer(Modifier.height(SpacingTokens.xs))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
            ) {
                assistToggles()
            }
        }

        if (!blockedReason.isNullOrBlank()) {
            Spacer(Modifier.height(SpacingTokens.xs))
            BlockedReasonRow(reason = blockedReason, onClick = onModelClick)
        }

        Spacer(Modifier.height(SpacingTokens.sm))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        ) {
            // The model chip takes the free space directly. It used to share it 50/50 with a
            // weighted Spacer, which squeezed the label so hard that "Local tiny-SD (offline)"
            // clipped to "Local" — the chip named the wrong model. Dropping the spacer keeps
            // the send orb right-aligned anyway, since the chip now fills the gap.
            // Leading "+" — the reference design's attach affordance, and the one place a
            // reference image can be added once the composer is in a non-image mode.
            if (onAddReference != null) {
                Box(
                    Modifier
                        .size(ControlTokens.chip)
                        .testTag(TestTags.COMPOSER_ATTACH_BUTTON)
                        .clip(CircleShape)
                        .background(VestraColors.Canvas)
                        .clickable(enabled = !busy, onClick = onAddReference),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Attach a reference image",
                        tint = VestraColors.InkMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            ModelChip(
                label = modelLabel,
                onClick = onModelClick,
                accent = accent,
                modifier = Modifier.weight(1f).testTag(TestTags.MODEL_CHIP),
            )
            // A count of zero with no handler is not information — it was rendering a
            // permanently inert "Layers 0" between the model chip and the send button.
            if (assistCount > 0 || onAssistsClick != null) {
                AssistChip(
                    count = assistCount,
                    onClick = onAssistsClick,
                    modifier = Modifier.testTag(TestTags.ASSIST_CHIP),
                )
            }
            SendOrb(
                busy = busy,
                loading = loading,
                enabled = enabled && !loading && (busy || prompt.isNotBlank()),
                onSend = onSend,
                onStop = onStop,
                modifier = Modifier.testTag(TestTags.SEND_BUTTON),
            )
        }
    }
}

/**
 * Why generation is gated right now. Full width and up to two lines, because these strings are
 * sentences — the whole point of splitting them out of the one-line model chip.
 */
@Composable
private fun BlockedReasonRow(reason: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusTokens.md))
            .background(VestraColors.Canvas)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(TestTags.COMPOSER_BLOCKED_HINT)
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            reason,
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.InkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModelChip(
    label: String,
    onClick: (() -> Unit)?,
    accent: Color = VestraColors.Accent,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    val a11y = if (onClick != null) {
        "Selected model $label. Opens model picker."
    } else {
        "Selected model $label"
    }
    Row(
        modifier
            .heightIn(min = ControlTokens.chip)
            .clip(shape)
            .background(VestraColors.Canvas)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = a11y }
            .padding(horizontal = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(ControlTokens.dot)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(SpacingTokens.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.Ink,
            maxLines = 1,
            // Without this the default Clip cut "Local tiny-SD (offline)" down to "Local" with
            // no indication anything was missing, so the chip lied about which model was
            // selected. The full name stays in the chip's contentDescription for a11y.
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onClick != null) {
            Spacer(Modifier.width(SpacingTokens.xxs))
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = VestraColors.InkMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AssistChip(count: Int, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val a11y = when {
        count <= 0 -> "No assists active"
        count == 1 -> "1 assist active"
        else -> "$count assists active"
    }
    Row(
        modifier
            .heightIn(min = ControlTokens.chip)
            .clip(shape)
            .background(VestraColors.Canvas)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = a11y }
            .padding(horizontal = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Layers,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(SpacingTokens.xxs + 2.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.Ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun SendOrb(
    busy: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(ControlTokens.orb)
            .clip(CircleShape)
            .background(
                if (busy || loading) {
                    Brush.radialGradient(listOf(VestraColors.Danger, VestraColors.SaffronDeep))
                } else if (enabled) {
                    Brush.radialGradient(listOf(VestraColors.AccentSoft, VestraColors.Accent))
                } else {
                    Brush.radialGradient(
                        listOf(
                            VestraColors.InkMuted.copy(alpha = 0.35f),
                            VestraColors.InkMuted.copy(alpha = 0.2f),
                        ),
                    )
                },
            )
            // clickable(enabled = false) still consumes touches — loading disables the click
            // itself via enabled=false one level up, but this listener would still fire for
            // "busy" while loading is somehow also true; loading is checked first so it can
            // never route into onSend()/onStop() while there's nothing to start or cancel.
            .clickable(enabled = enabled) { if (!loading) { if (busy) onStop() else onSend() } },
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = VestraColors.Ivory,
            )
            else -> Icon(
                if (busy) Icons.Outlined.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (busy) "Cancel generation" else "Generate",
                tint = VestraColors.Ivory,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
