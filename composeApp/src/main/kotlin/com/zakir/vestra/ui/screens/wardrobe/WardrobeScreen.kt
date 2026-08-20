package com.zakir.vestra.ui.screens.wardrobe

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
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

    GlassScreen(
        title = "Wardrobe",
        subtitle = "Your looks",
        onBack = onBack,
        scrollable = false,
    ) {
        if (entries.isEmpty()) {
            GlassEmptyState(message = "Your generated looks live here.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    GlassCard {
                        AsyncImage(
                            model = File(entry.imagePath),
                            contentDescription = "Generated look ${entry.personLabel}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val file = File(entry.imagePath)
                                    if (!file.exists()) {
                                        Toast.makeText(context, "File missing", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file,
                                    )
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "image/jpeg"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            "Share look",
                                        ),
                                    )
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
                        GlassSecondaryButton(
                            text = "Delete",
                            onClick = { wardrobe.remove(entry.id) },
                        )
                    }
                }
            }
        }
    }
}
