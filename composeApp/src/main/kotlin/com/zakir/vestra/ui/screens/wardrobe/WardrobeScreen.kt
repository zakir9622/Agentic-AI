package com.zakir.vestra.ui.screens.wardrobe

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.media.MediaExport
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassEmptyState
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import java.io.File

@Composable
fun WardrobeScreen(
    wardrobe: WardrobeRepository,
    onBack: () -> Unit,
) {
    val entries by wardrobe.entries.collectAsState()
    val context = LocalContext.current
    var favoritesOnly by remember { mutableStateOf(false) }
    val visible = remember(entries, favoritesOnly) {
        if (favoritesOnly) entries.filter { it.favorited } else entries
    }

    GlassScreen(
        title = LookbookCopy.STUDIO_WARDROBE,
        subtitle = "Looks · listings · assets",
        onBack = onBack,
        scrollable = false,
    ) {
        if (entries.isEmpty()) {
            GlassEmptyState(message = "Your try-on and Image studio looks appear here.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { favoritesOnly = false },
                    label = { Text("All (${entries.size})") },
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = true },
                    label = { Text("Favorites (${entries.count { it.favorited }})") },
                )
            }
            Spacer(Modifier.height(12.dp))
            if (visible.isEmpty()) {
                GlassEmptyState(message = "No favorites yet — tap ★ on a look.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { entry ->
                        val file = File(entry.imagePath)
                        GlassCard {
                            AsyncImage(
                                model = file,
                                contentDescription = "Generated look ${entry.personLabel}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (!file.exists()) {
                                            Toast.makeText(context, "File missing", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        MediaExport.share(context, file, "Share look")
                                    },
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${entry.personLabel} · ${entry.tier.name.lowercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassSecondaryButton(
                                    text = if (entry.favorited) "★ Fav" else "☆ Fav",
                                    onClick = { wardrobe.toggleFavorite(entry.id) },
                                    modifier = Modifier.weight(1f),
                                )
                                GlassSecondaryButton(
                                    text = "Share",
                                    onClick = {
                                        if (!file.exists()) {
                                            Toast.makeText(context, "File missing", Toast.LENGTH_SHORT).show()
                                        } else {
                                            MediaExport.share(context, file, "Share look")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassSecondaryButton(
                                    text = "Save to Photos",
                                    onClick = { MediaExport.saveImageToGallery(context, file) },
                                    modifier = Modifier.weight(1f),
                                )
                                GlassSecondaryButton(
                                    text = "Delete",
                                    onClick = {
                                        runCatching { if (file.exists()) file.delete() }
                                        wardrobe.remove(entry.id)
                                        Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
