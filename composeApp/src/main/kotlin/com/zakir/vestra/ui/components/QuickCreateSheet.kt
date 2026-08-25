package com.zakir.vestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zakir.vestra.ui.screens.home.HomeTab
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.VestraColors

private data class QuickToolItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String,
    val onSelect: () -> Unit,
)

/**
 * Tool-picker dialog opened from the bottom dock's center Create action — exact-match of
 * lookbookweb's Create dialog (`AppShell.tsx`'s tool-picker `Dialog`, title "Create",
 * description "Pick a tool to start something new."): a centered dialog, not a bottom sheet.
 * One grid of every local generation surface, each with a short description and a real
 * capability badge, so "Create" has a single obvious entry point instead of always landing on
 * the last-used tab.
 */
@Composable
internal fun QuickCreateSheet(
    onSelectTab: (HomeTab) -> Unit,
    onOpenChat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tools = remember {
        buildList {
            add(
                QuickToolItem(
                    id = "image",
                    title = "Image Studio",
                    description = "Modest silhouettes, textiles & lookbook renders — on-device or cloud",
                    icon = Icons.Outlined.Image,
                    accentColor = VestraColors.ModalityImage,
                    badge = "Local + Cloud",
                    onSelect = { onSelectTab(HomeTab.IMAGE) },
                ),
            )
            add(
                QuickToolItem(
                    id = "video",
                    title = "Video Studio",
                    description = "Local still-clip motion loops, or cloud generation",
                    icon = Icons.Outlined.Videocam,
                    accentColor = VestraColors.ModalityVideo,
                    badge = "Local + Cloud",
                    onSelect = { onSelectTab(HomeTab.VIDEO) },
                ),
            )
            add(
                QuickToolItem(
                    id = "code",
                    title = "Code Studio",
                    description = "On-device Gemma / Qwen reasoning and code generation",
                    icon = Icons.Outlined.Code,
                    accentColor = VestraColors.ModalityCode,
                    badge = "On-Device",
                    onSelect = { onSelectTab(HomeTab.CODE) },
                ),
            )
            add(
                QuickToolItem(
                    id = "audio",
                    title = "Audio Studio",
                    description = "Device TTS, voice-changer DSP knobs, and offline transcription",
                    icon = Icons.Outlined.GraphicEq,
                    accentColor = VestraColors.ModalityAudio,
                    badge = "On-Device",
                    onSelect = { onSelectTab(HomeTab.AUDIO) },
                ),
            )
            if (HomeTab.TRY_ON_TAB_ENABLED) {
                add(
                    QuickToolItem(
                        id = "try_on",
                        title = "Virtual Try-On",
                        description = "Drape garments onto casting models",
                        icon = Icons.Outlined.Checkroom,
                        accentColor = VestraColors.Accent,
                        badge = "Pro / Lite",
                        onSelect = { onSelectTab(HomeTab.TRY_ON) },
                    ),
                )
            }
            add(
                QuickToolItem(
                    id = "chat",
                    title = "News & Chat",
                    description = "Fashion + AI headlines, discuss any story with a local or cloud model",
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    accentColor = VestraColors.ModalityCode,
                    badge = "Local + Cloud",
                    onSelect = onOpenChat,
                ),
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(RadiusTokens.xl3))
                .background(VestraColors.Canvas)
                .border(
                    width = 1.dp,
                    color = VestraColors.GlassBorder,
                    shape = RoundedCornerShape(RadiusTokens.xl3),
                )
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Create",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = VestraColors.Ink,
                    )
                    Text(
                        text = "Pick a tool to start something new.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VestraColors.InkMuted,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VestraColors.GlassFill),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = VestraColors.InkMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().height(360.dp),
            ) {
                items(tools, key = { it.id }) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = {
                            onDismiss()
                            tool.onSelect()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: QuickToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val cardBackground = Brush.verticalGradient(
        colors = listOf(
            VestraColors.GlassFillStrong,
            tool.accentColor.copy(alpha = 0.08f),
        ),
    )

    Column(
        modifier = modifier
            .testTag("tool_card_${tool.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusTokens.lg))
            .background(cardBackground)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(tool.accentColor.copy(alpha = 0.35f), Color.Transparent),
                ),
                shape = RoundedCornerShape(RadiusTokens.lg),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, tool.accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tool.accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = tool.badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                    color = tool.accentColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tool.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = VestraColors.Ink,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tool.description,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
            color = VestraColors.InkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
