package com.zakir.vestra.ui.screens.studio

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
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
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground
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
        animationSpec = tween(520),
        label = "studioFade",
    )

    SpatialBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .alpha(fade),
            contentPadding = PaddingValues(bottom = 36.dp),
        ) {
            item(key = "top") {
                GlassTopBar(
                    title = "The Lookbook",
                    subtitle = "Shop · sell · create",
                    actions = {
                        IconButton(onClick = onOpenWardrobe) {
                            Icon(Icons.Outlined.Checkroom, contentDescription = "Wardrobe")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPill(text = if (proReady) "Pro ready" else "Pro pack needed", active = proReady)
                    GlassPill(text = "On-device", active = true)
                    GlassPill(
                        text = if (online) "Online" else "Offline",
                        active = online,
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item(key = "hero") {
                GlassCard {
                    GlassSectionLabel("ESSENTIAL · ALL PERSONAS")
                    Text("Virtual try-on", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Shoppers preview modest wear. Sellers batch listing shots. Creators cast looks — on-device Lite/Pro or free cloud try-on.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewLook, modifier = Modifier.fillMaxWidth()) {
                        Text("Start a garment shoot")
                    }
                }
                Spacer(Modifier.height(14.dp))
                GlassSectionLabel("MEDIA LOOP")
            }

            item(key = "create") {
                StudioTile(
                    icon = Icons.Outlined.Image,
                    title = "Create Studio",
                    body = "Prompt or recreate product / lookbook stills. Save to gallery · share · wardrobe.",
                    onClick = onOpenCreate,
                )
                Spacer(Modifier.height(10.dp))
            }
            item(key = "video") {
                StudioTile(
                    icon = Icons.Outlined.Videocam,
                    title = "Video Studio",
                    body = "Free HF clips (LTX, CogVideoX). Save to Movies/The Lookbook.",
                    onClick = onOpenVideo,
                )
                Spacer(Modifier.height(10.dp))
            }
            item(key = "code") {
                StudioTile(
                    icon = Icons.Outlined.Code,
                    title = "Code Studio",
                    body = "Free coding models — Qwen, DeepSeek, Llama on Groq. Tokens in Usage.",
                    onClick = onOpenCode,
                )
            }

            if (!proReady) {
                item(key = "pro-cta") {
                    Spacer(Modifier.height(12.dp))
                    GlassCard(onClick = onOpenPacks) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Download Pro AI model", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "One-time ~2–4 GB. Fully offline after. Or use free cloud try-on in Settings.",
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
                        "See requests, tokens, and estimated cost per model and service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("RECENT LOOKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
            }

            item(key = "recent") {
                if (recent.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recent.take(10), key = { it.id }) { entry ->
                            val file = File(entry.imagePath)
                            AsyncImage(
                                model = file,
                                contentDescription = "Recent look",
                                modifier = Modifier
                                    .width(130.dp)
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        onClick = onOpenWardrobe,
                                        onLongClick = {
                                            if (file.exists()) {
                                                MediaExport.share(context, file, "Share look")
                                            }
                                        },
                                    ),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(24.dp)),
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
private fun StudioTile(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
