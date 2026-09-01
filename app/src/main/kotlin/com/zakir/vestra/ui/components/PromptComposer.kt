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
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Prompt-first floating composer — model pill, assist count, send/stop.
 * Pattern adapted from modern generative shells; copy and brand are Lookbook.
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
    referenceUri: String? = null,
    onAddReference: (() -> Unit)? = null,
    onClearReference: (() -> Unit)? = null,
    assistToggles: (@Composable () -> Unit)? = null,
    quickPrompts: List<QuickPromptItem> = emptyList(),
    onSelectQuickPrompt: ((String) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(VestraColors.GlassHighlight, accent.copy(alpha = 0.35f)),
                ),
                shape,
            )
            .padding(14.dp),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                referenceUri?.let { uri ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(VestraColors.GlassFill)
                            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Reference",
                            modifier = Modifier
                                .size(48.dp)
                                .testTag(TestTags.REFERENCE_IMAGE_THUMB)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reference attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = VestraColors.Ink,
                        )
                        Spacer(Modifier.width(4.dp))
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
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                }
                if (onAddReference != null && referenceUri == null) {
                    Row(
                        Modifier
                            .testTag(TestTags.ADD_REFERENCE_BUTTON)
                            .clip(RoundedCornerShape(14.dp))
                            .background(VestraColors.GlassFill)
                            .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(14.dp))
                            .clickable(enabled = !busy, onClick = onAddReference)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Add reference image",
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Attach Reference",
                            style = MaterialTheme.typography.labelMedium,
                            color = VestraColors.InkMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.PROMPT_INPUT),
            enabled = !busy,
            minLines = 2,
            maxLines = 5,
            placeholder = { Text(placeholder) },
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent.copy(alpha = 0.55f),
                unfocusedBorderColor = VestraColors.GlassBorder,
                focusedContainerColor = VestraColors.GlassFill,
                unfocusedContainerColor = VestraColors.GlassFill,
            ),
            shape = RoundedCornerShape(SpacingTokens.section),
        )

        if (assistToggles != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                assistToggles()
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The model chip takes the free space directly. It used to share it 50/50 with a
            // weighted Spacer, which squeezed the label so hard that "Local tiny-SD (offline)"
            // clipped to "Local" — the chip named the wrong model. Dropping the spacer keeps
            // the send orb right-aligned anyway, since the chip now fills the gap.
            ModelChip(
                label = modelLabel,
                onClick = onModelClick,
                accent = accent,
                modifier = Modifier.weight(1f).testTag(TestTags.MODEL_CHIP),
            )
            AssistChip(
                count = assistCount,
                onClick = onAssistsClick,
                modifier = Modifier.testTag(TestTags.ASSIST_CHIP),
            )
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
            .clip(shape)
            .background(VestraColors.GlassFill)
            .border(1.dp, accent.copy(alpha = 0.4f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = a11y }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = VestraColors.Ink,
            maxLines = 1,
            // Without this the default Clip cut "Local tiny-SD (offline)" down to "Local" with
            // no indication anything was missing, so the chip lied about which model was
            // selected. The full name stays in the chip's contentDescription for a11y.
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
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
            .clip(shape)
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = a11y }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Layers,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = VestraColors.Ink,
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
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (busy || loading) {
                    Brush.radialGradient(listOf(VestraColors.Danger, VestraColors.SaffronDeep))
                } else if (enabled) {
                    Brush.radialGradient(listOf(VestraColors.AccentSoft, VestraColors.SaffronDeep))
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
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
