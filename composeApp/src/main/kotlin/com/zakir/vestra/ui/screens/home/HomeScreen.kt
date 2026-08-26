package com.zakir.vestra.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.jobs.LocalJobStore
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.InterruptedJobsBanner
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.rememberReduceMotion

/**
 * Home *is* the tool picker — not a popup behind a "+" FAB, and not a tabbed pager sharing one
 * screen between Image/Video/Audio/Code. Each card below opens a fully isolated, standalone
 * screen for that modality (only one local model is ever loaded at a time, so the studios must
 * never share a swipeable pager or tab row that could keep more than one alive).
 */
@Composable
fun HomeScreen(
    localJobStore: LocalJobStore? = null,
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onSelectAudio: () -> Unit,
    onSelectCode: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenPacks: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(Unit) { appeared = true }
    val fade by animateFloatAsState(
        targetValue = if (appeared || reduceMotion) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(640),
        label = "homeFade",
    )

    val tools = remember {
        listOf(
            HomeTool(
                id = "image",
                title = "Image Studio",
                description = "Garment renders and lookbook photography — on-device or cloud",
                icon = Icons.Outlined.Image,
                accentColor = VestraColors.ModalityImage,
                badge = "Local + Cloud",
                onSelect = onSelectImage,
            ),
            HomeTool(
                id = "video",
                title = "Video Studio",
                description = "Local still-clip motion loops, or cloud generation",
                icon = Icons.Outlined.Videocam,
                accentColor = VestraColors.ModalityVideo,
                badge = "Local + Cloud",
                onSelect = onSelectVideo,
            ),
            HomeTool(
                id = "code",
                title = "Code Studio",
                description = "On-device Gemma / Qwen reasoning and code generation",
                icon = Icons.Outlined.Code,
                accentColor = VestraColors.ModalityCode,
                badge = "On-Device",
                onSelect = onSelectCode,
            ),
            HomeTool(
                id = "audio",
                title = "Audio Studio",
                description = "Device TTS, voice-changer DSP knobs, and offline transcription",
                icon = Icons.Outlined.GraphicEq,
                accentColor = VestraColors.ModalityAudio,
                badge = "On-Device",
                onSelect = onSelectAudio,
            ),
            HomeTool(
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

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .alpha(fade)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.padding(horizontal = SpacingTokens.section)) {
                InterruptedJobsBanner(localJobStore)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.section),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    LookbookCopy.PRODUCT_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    color = VestraColors.Ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(VestraColors.SaffronDeep, VestraColors.Accent),
                                ),
                            )
                            .clickable(onClick = onOpenPacks)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Packs",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                    IconButton(onClick = onOpenHelp) {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = LookbookCopy.STUDIO_HELP,
                            tint = VestraColors.Ink,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(Modifier.padding(horizontal = SpacingTokens.section)) {
                GlassSectionLabel("CREATE")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeToolCard(tools[0], Modifier.weight(1f))
                    HomeToolCard(tools[1], Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeToolCard(tools[2], Modifier.weight(1f))
                    HomeToolCard(tools[3], Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                HomeToolCard(tools[4], Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private data class HomeTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String,
    val onSelect: () -> Unit,
)

@Composable
private fun HomeToolCard(tool: HomeTool, modifier: Modifier = Modifier) {
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
                onClick = tool.onSelect,
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
