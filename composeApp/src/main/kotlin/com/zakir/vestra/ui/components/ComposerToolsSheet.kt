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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * One attachment source in the sheet's top row — a photo, a camera capture, a file.
 * These *add* something to the next message without changing which generator runs it.
 */
data class ComposerSource(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * One tool in the sheet's list — the thing that decides which generator the next message routes
 * to. [selected] marks the tool the composer is currently in, so the sheet doubles as the mode
 * indicator rather than needing a separate always-visible chip row.
 */
data class ComposerTool(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * The composer's `+` sheet: attachment sources across the top, generators listed below.
 *
 * This replaces the always-visible five-chip modality row. That row cost a permanent 40dp band
 * above the composer to surface a choice most messages never change, and it forced the composer
 * to carry a *second* attach affordance beside it — the app shipped with an "Attach Reference"
 * chip and a leading "+" button both visible in Image mode, the chip sitting on top of the
 * placeholder text.
 *
 * Folding both into one `+` gives the thread that space back and leaves exactly one way to
 * attach and exactly one way to switch generator. The active tool still shows in the composer,
 * as a small dismissible chip, so nothing about the current mode is hidden — it is just not
 * occupying a row of its own when it is the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerToolsSheet(
    sources: List<ComposerSource>,
    tools: List<ComposerTool>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VestraColors.SurfaceRaised,
            // A ModalBottomSheet is its own window with its own composition root, so it does NOT
        // inherit the `testTagsAsResourceId = true` set on the content root in MainActivity.
        // Without this, every testTag inside the sheet is invisible to UiAutomator/Appium: a
        // live probe showed the page source collapse from fifteen resource-ids on the main
        // screen to a single `android:id/content` the moment the sheet opened. Each sheet has
        // to opt in for itself.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(com.zakir.vestra.ui.TestTags.COMPOSER_TOOLS_SHEET)
                .padding(horizontal = SpacingTokens.lg)
                .padding(bottom = 28.dp),
        ) {
            if (sources.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                ) {
                    sources.forEach { source ->
                        SourceButton(source, onDismiss)
                    }
                }
                Spacer(Modifier.height(SpacingTokens.lg))
            }

            tools.forEach { tool ->
                ToolRow(tool, onDismiss)
                Spacer(Modifier.height(SpacingTokens.xxs))
            }
        }
    }
}

/** A source reads as a wide pill with the icon over the label, matching the reference layout. */
@Composable
private fun SourceButton(source: ComposerSource, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.xl)
    Column(
        Modifier
            .testTag(com.zakir.vestra.ui.TestTags.composerSource(source.id))
            .width(96.dp)
            .heightIn(min = 82.dp)
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .clickable {
                source.onClick()
                onDismiss()
            }
            .padding(vertical = SpacingTokens.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(source.icon, contentDescription = null, tint = VestraColors.Ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(SpacingTokens.xxs + 2.dp))
        Text(
            source.label,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToolRow(tool: ComposerTool, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(com.zakir.vestra.ui.TestTags.composerTool(tool.id))
            .clip(shape)
            .background(if (tool.selected) tool.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable {
                tool.onClick()
                onDismiss()
            }
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tool.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tool.icon, contentDescription = null, tint = tool.accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                tool.label,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (tool.selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Currently selected",
                tint = tool.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
