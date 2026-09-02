package com.zakir.vestra.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
 * The app's one chatbox.
 *
 * Everything the user can send goes through this single control, the way the Gemini app works:
 * a `+` that opens sources and tools, one text field, one send button. What it deliberately no
 * longer has:
 *
 * - **A modality chip row above it.** Five chips took a permanent 40dp band to expose a choice
 *   most messages never change. The generator now lives in the `+` sheet
 *   ([ComposerToolsSheet]), and the *active* one — when it isn't plain Chat — shows as a small
 *   dismissible chip on the row above the field, where it costs nothing when unused.
 * - **Two ways to attach.** The composer used to render an "Attach Reference" chip row *and* a
 *   leading attach button whenever a reference was possible. Both were visible at once in Image
 *   mode, with the chip overlapping the placeholder. There is one `+` now.
 * - **A model chip.** It read "FLUX.1 Schnell · Ready · verified 6m ago" — three facts crammed
 *   into a one-line control that then ellipsised. The model selector moved to the top bar, which
 *   has the width for a name and is where the reference app puts it.
 *
 * Two rules that survived from the previous design, because both were learned from real defects:
 *
 * 1. **[blockedReason] is never folded into a label.** It is a sentence and gets its own row.
 * 2. **One rim per surface.** The field is borderless and recessed by fill
 *    ([VestraColors.Canvas] against the container's [VestraColors.GlassFillStrong]).
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun PromptComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
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
    placeholder: String = "Ask Lookbook",
    /** Opens the `+` sheet. The only attach affordance and the only generator switch. */
    onOpenTools: (() -> Unit)? = null,
    /** Dictation. Absent when no speech recogniser is installed, rather than shown inert. */
    onDictate: (() -> Unit)? = null,
    /**
     * Why generation is currently gated, if it is — e.g. "Add a free API key in Settings".
     * Shown as its own hint row; never folded into another label.
     */
    blockedReason: String? = null,
    /** The non-default generator the next message will use, if any. Null means plain Chat. */
    activeTool: ActiveTool? = null,
    referenceUri: String? = null,
    onClearReference: (() -> Unit)? = null,
    quickPrompts: List<QuickPromptItem> = emptyList(),
    onSelectQuickPrompt: ((String) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(RadiusTokens.xl2)
    Column(
        modifier
            .fillMaxWidth()
            .testTag(TestTags.PROMPT_COMPOSER)
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, shape)
            // The context row appears and disappears with the active tool and any attachment, so
            // the composer's height changes. Animating it keeps that from reading as a jump.
            .animateContentSize()
            .padding(horizontal = SpacingTokens.xxs + 2.dp, vertical = SpacingTokens.xxs),
    ) {
        if (quickPrompts.isNotEmpty() && onSelectQuickPrompt != null && !busy) {
            QuickPromptCarousel(
                prompts = quickPrompts,
                onSelectPrompt = onSelectQuickPrompt,
                enabled = enabled,
            )
        }

        // Context row: what this message carries beyond its text. Renders nothing at all in the
        // common case, which is what makes a single chatbox feel like one control instead of a
        // panel.
        if (activeTool != null || referenceUri != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.COMPOSER_CONTEXT_ROW)
                    .padding(start = SpacingTokens.xs, top = SpacingTokens.xxs),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                activeTool?.let { tool ->
                    ActiveToolChip(tool)
                }
                referenceUri?.let { uri ->
                    ReferenceThumb(uri = uri, onClear = onClearReference)
                }
            }
            Spacer(Modifier.height(SpacingTokens.xs))
        }

        // One row: `+`, the field, and send — the shape the reference app uses and the reason
        // this reads as a single control rather than a panel. It was briefly a field stacked
        // over an action row, which cost ~56dp of height on every screen to say the same thing.
        Row(
            Modifier.fillMaxWidth(),
            // Bottom, not Center: the field grows upward as the prompt wraps, and the buttons
            // must stay on the last line rather than drifting to the middle of a 6-line prompt.
            verticalAlignment = Alignment.Bottom,
        ) {
            if (onOpenTools != null) {
                ComposerIconButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "Add photos, files, or switch tool",
                    testTag = TestTags.COMPOSER_ATTACH_BUTTON,
                    enabled = !busy,
                    onClick = onOpenTools,
                )
            }
            BasicTextField(
                value = prompt,
                onValueChange = onPromptChange,
                enabled = !busy,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = VestraColors.Ink),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.PROMPT_INPUT)
                    .padding(horizontal = SpacingTokens.xs)
                    .padding(vertical = 12.dp),
                decorationBox = { inner ->
                    // The placeholder is drawn behind the field's own content rather than in a
                    // sibling row. That is the fix for the reported defect, where a chip laid out
                    // beside the field ended up on top of this text.
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (prompt.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VestraColors.InkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            if (prompt.isNotBlank() && !busy) {
                ComposerIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Clear prompt",
                    testTag = TestTags.COMPOSER_CLEAR_BUTTON,
                    enabled = true,
                    onClick = { onPromptChange("") },
                )
            }
            if (onDictate != null && prompt.isBlank() && !busy) {
                ComposerIconButton(
                    icon = Icons.Outlined.Mic,
                    contentDescription = "Dictate a prompt",
                    testTag = TestTags.COMPOSER_MIC_BUTTON,
                    enabled = true,
                    onClick = onDictate,
                )
            }
            // Send appears with the first character, exactly as in the reference: an empty box
            // offers dictation, not a permanently greyed-out send orb whose only message is
            // "you cannot do this yet".
            if (busy || loading || prompt.isNotBlank()) {
                SendOrb(
                    busy = busy,
                    loading = loading,
                    enabled = enabled && !loading,
                    onSend = onSend,
                    onStop = onStop,
                    modifier = Modifier.testTag(TestTags.SEND_BUTTON),
                )
            }
        }

        if (!blockedReason.isNullOrBlank()) {
            Spacer(Modifier.height(SpacingTokens.xs))
            BlockedReasonRow(reason = blockedReason)
        }
    }
}

/** The generator the next message routes to, when it is not plain Chat. */
data class ActiveTool(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    /** Returns to plain Chat. */
    val onClear: () -> Unit,
)

@Composable
private fun ActiveToolChip(tool: ActiveTool) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .testTag(TestTags.COMPOSER_ACTIVE_TOOL)
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(tool.accent.copy(alpha = 0.18f))
            .padding(start = SpacingTokens.xs + 2.dp, end = SpacingTokens.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tool.icon, contentDescription = null, tint = tool.accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(SpacingTokens.xxs + 2.dp))
        Text(
            tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.Ink,
            maxLines = 1,
        )
        Spacer(Modifier.width(SpacingTokens.xxs))
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(onClick = tool.onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Switch back to Chat",
                tint = VestraColors.InkMuted,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/** The attached reference image, as a thumbnail on the context row. */
@Composable
private fun ReferenceThumb(uri: String, onClear: (() -> Unit)?) {
    Row(
        Modifier
            .clip(RoundedCornerShape(RadiusTokens.md))
            .background(VestraColors.Canvas)
            .padding(SpacingTokens.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Attached reference image",
            modifier = Modifier
                .size(32.dp)
                .testTag(TestTags.REFERENCE_IMAGE_THUMB)
                .clip(RoundedCornerShape(RadiusTokens.sm)),
            contentScale = ContentScale.Crop,
        )
        if (onClear != null) {
            Spacer(Modifier.width(SpacingTokens.xxs))
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove reference",
                    tint = VestraColors.InkMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Why generation is gated right now. Full width and up to two lines, because these strings are
 * sentences — the whole point of keeping them out of one-line controls.
 */
@Composable
private fun BlockedReasonRow(reason: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusTokens.md))
            .background(VestraColors.Canvas)
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
            // Three, not two: the cloud-consent sentence is ~140 characters and rendered as
            // "…to use Hugging Face — n…" at two lines, cutting the clause that says nothing is
            // sent to the network. Truncating a consent notice mid-promise is the worst place to
            // save a line.
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ComposerIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(ControlTokens.chip)
            .testTag(testTag)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) VestraColors.Ink else VestraColors.InkMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(21.dp),
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
                contentDescription = if (busy) "Cancel generation" else "Send",
                tint = VestraColors.Ivory,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
