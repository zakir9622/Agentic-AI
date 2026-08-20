package com.zakir.vestra.ui.screens.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.shared.domain.PackStatus
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground
import java.io.File

@Composable
fun StudioScreen(
    wardrobe: WardrobeRepository,
    packManager: ModelPackManager,
    onNewLook: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPacks: () -> Unit,
) {
    val recent by wardrobe.entries.collectAsState()
    val packStates by packManager.states.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { packManager.refresh() }
    val proReady = listOf("pro-v2-int8", "pro-v1").any { id ->
        packStates[id]?.status == PackStatus.INSTALLED
    }

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            GlassTopBar(
                title = "The Lookbook",
                subtitle = "Spatial studio",
                actions = {
                    IconButton(onClick = onOpenWardrobe) {
                        Icon(Icons.Outlined.Checkroom, contentDescription = "Wardrobe")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(text = if (proReady) "Pro ready" else "Pro pack needed", active = proReady)
                GlassPill(text = "Cloud models", active = true, accent = MaterialTheme.colorScheme.secondary)
            }

            Spacer(Modifier.height(24.dp))

            GlassCard {
                Text(
                    "Modest wear photoshoots on your phone",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Abaya, hijab, niqab, shalwar kameez — cast models with ethnicity, body type, and scene parameters. Local AI or free cloud models.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNewLook, modifier = Modifier.fillMaxWidth()) {
                    Text("Start a shoot")
                }
            }

            if (!proReady) {
                Spacer(Modifier.height(12.dp))
                GlassCard(onClick = onOpenPacks) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Download Pro AI model", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "One-time ~2–4 GB download. Fully offline after. Or use free cloud models in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("RECENT LOOKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(10.dp))

            if (recent.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent.take(10), key = { it.id }) { entry ->
                        AsyncImage(
                            model = File(entry.imagePath),
                            contentDescription = "Recent look",
                            modifier = Modifier
                                .width(130.dp)
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenWardrobe),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
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

        FloatingActionButton(
            onClick = onNewLook,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
