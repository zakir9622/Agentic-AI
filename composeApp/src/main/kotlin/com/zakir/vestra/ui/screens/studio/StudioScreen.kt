package com.zakir.vestra.ui.screens.studio

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.media.MediaExport
import com.zakir.vestra.shared.domain.PackStatus
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.components.AtelierHero
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.VestraColors
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudioScreen(
    appSettings: AppSettings,
    wardrobe: WardrobeRepository,
    packManager: ModelPackManager,
    onNewLook: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenCode: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPacks: () -> Unit,
    onOpenUsage: () -> Unit,
) {
    val context = LocalContext.current
    val recent by wardrobe.entries.collectAsState()
    val packStates by packManager.states.collectAsState()
    LaunchedEffect(Unit) { packManager.refresh() }
    val proReady = listOf("pro-v2-int8", "pro-v1").any { id ->
        packStates[id]?.status == PackStatus.INSTALLED
    }

    var online by remember { mutableStateOf(appSettings.networkLikelyAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            online = appSettings.networkLikelyAvailable()
            delay(2_500)
        }
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val fade by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(640),
        label = "studioFade",
    )
    val heroLift by animateFloatAsState(
        targetValue = if (appeared) 0f else 18f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroLift",
    )

    val statusLine = buildString {
        append(if (proReady) "Pro on-device" else "Lite · cloud ready")
        append("  ·  ")
        append(if (online) "Signal live" else "Offline")
    }

    SpatialBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 18.dp)
                .alpha(fade),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item(key = "chrome") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onOpenWardrobe) {
                        Icon(
                            Icons.Outlined.Checkroom,
                            contentDescription = "Wardrobe",
                            tint = VestraColors.Ink,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VestraColors.GlassFillStrong)
                                .border(1.5.dp, VestraColors.Accent.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = VestraColors.Accent,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            item(key = "hero") {
                Box(Modifier.padding(bottom = heroLift.dp)) {
                    AtelierHero(
                        brand = "The Lookbook",
                        headline = "Modest wear · local AI",
                        support = "Cast abaya, hijab, and shalwar looks on-device — or spin free cloud studios for stills, video, and code.",
                        cta = "Start a garment shoot",
                        onCta = onNewLook,
                        statusLine = statusLine,
                    )
                }
                Spacer(Modifier.height(22.dp))
            }

            item(key = "studios-label") {
                GlassSectionLabel("STUDIOS")
            }

            item(key = "create") {
                StudioTile(
                    icon = Icons.Outlined.Image,
                    title = "Create",
                    body = "Prompt stills · recreate · Photos",
                    onClick = onOpenCreate,
                )
                Spacer(Modifier.height(10.dp))
            }
            item(key = "video") {
                StudioTile(
                    icon = Icons.Outlined.Videocam,
                    title = "Video",
                    body = "Free HF clips · Movies gallery",
                    onClick = onOpenVideo,
                )
                Spacer(Modifier.height(10.dp))
            }
            item(key = "code") {
                StudioTile(
                    icon = Icons.Outlined.Code,
                    title = "Code",
                    body = "Groq · HF · OpenRouter free",
                    onClick = onOpenCode,
                )
            }

            if (!proReady) {
                item(key = "pro-cta") {
                    Spacer(Modifier.height(14.dp))
                    GlassCard(onClick = onOpenPacks) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconWell(Icons.Outlined.Cloud)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Install Pro pack", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "One download. Fully offline after. Free cloud try-on stays in Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "usage") {
                Spacer(Modifier.height(12.dp))
                GlassCard(onClick = onOpenUsage) {
                    Text("Token & usage", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Requests, tokens, and free-tier spend per model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(22.dp))
                GlassSectionLabel("RECENT LOOKS")
            }

            item(key = "recent") {
                if (recent.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recent.take(10), key = { it.id }) { entry ->
                            val file = File(entry.imagePath)
                            Box(
                                Modifier
                                    .width(138.dp)
                                    .aspectRatio(0.72f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                VestraColors.GlassHighlight,
                                                VestraColors.Accent.copy(alpha = 0.35f),
                                            ),
                                        ),
                                        RoundedCornerShape(22.dp),
                                    )
                                    .combinedClickable(
                                        onClick = onOpenWardrobe,
                                        onLongClick = {
                                            if (file.exists()) {
                                                MediaExport.share(context, file, "Share look")
                                            }
                                        },
                                    ),
                            ) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Recent look",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    androidx.compose.ui.graphics.Color.Transparent,
                                                    VestraColors.AtelierCanvas.copy(alpha = 0.75f),
                                                ),
                                            ),
                                        ),
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(VestraColors.GlassFill)
                            .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Your generated looks appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconWell(icon: ImageVector) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = VestraColors.Accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StudioTile(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tileScale",
    )
    GlassCard(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier.scale(scale),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconWell(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
