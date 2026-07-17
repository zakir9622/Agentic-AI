package com.zakir.vestra.ui.screens.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.Backdrop
import com.zakir.vestra.ui.TryOnViewModel
import java.io.File

/**
 * The outfit: add one garment for a single piece, or several for a full suit
 * (trousers + kurta + dupatta). Each piece is captured from the gallery or
 * camera and tagged with a category (Auto by default); the shoot layers them
 * onto the model in the right order.
 */
@Composable
fun GarmentScreen(
    viewModel: TryOnViewModel,
    guard: com.zakir.vestra.data.GarmentInputGuard,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val outfit by viewModel.outfit.collectAsState()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPiece by remember { mutableIntStateOf(0) }
    var wornWarning by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.addGarment(it.toString())
            selectedPiece = outfit.size // the newly added piece
        }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        if (saved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            pendingCaptureUri?.let {
                viewModel.addGarment(it.toString())
                selectedPiece = outfit.size
            }
        }
        pendingCaptureUri = null
    }

    fun launchCamera() {
        val captures = File(context.filesDir, "captures").apply { mkdirs() }
        val file = File(captures, "garment_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCaptureUri = uri
        captureLauncher.launch(uri)
    }

    fun pickFromGallery() =
        pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val active = outfit.getOrNull(selectedPiece.coerceIn(0, (outfit.size - 1).coerceAtLeast(0)))

    // Warn when the selected piece looks like a photo of a person wearing the
    // outfit rather than a flat garment — the Lite compositor can't isolate a
    // garment from a full model shot.
    androidx.compose.runtime.LaunchedEffect(active?.uri) {
        wornWarning = active?.uri?.let { guard.looksLikeWornPhoto(it) } ?: false
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Act I", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (outfit.size > 1) "The outfit" else "The garment",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stage for the selected piece.
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (active != null) {
                    AsyncImage(
                        model = active.uri,
                        contentDescription = "Selected garment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    // Manual rotate for photos with no EXIF orientation tag.
                    IconButton(
                        onClick = {
                            com.zakir.vestra.data.ImageRotator.rotate90(context, active.uri)?.let {
                                viewModel.setGarmentUri(selectedPiece, it)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Rotate90DegreesCw,
                            contentDescription = "Rotate 90°",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        text = "Add each piece of the outfit —\nkurta, trousers, dupatta.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (wornWarning) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "This looks like a photo of someone already wearing the outfit. " +
                            "For a clean result use a flat-lay or hanger shot of just the garment — " +
                            "or switch to the Cloud engine in Settings, which can restyle a full look.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Outfit strip: each added piece + an "add" tile.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(outfit) { index, piece ->
                    Box {
                        AsyncImage(
                            model = piece.uri,
                            contentDescription = "Piece ${index + 1}",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (index == selectedPiece) 2.dp else 1.dp,
                                    color = if (index == selectedPiece) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedPiece = index },
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = {
                                viewModel.removeGarment(index)
                                selectedPiece = 0
                            },
                            modifier = Modifier.size(22.dp).align(Alignment.TopEnd),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Remove piece",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                item {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(onClick = ::pickFromGallery),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Add a piece",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (active != null) {
                Spacer(Modifier.height(10.dp))
                // Category of the selected piece (Auto detects abaya/hijab/dress/…).
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = active.category == null,
                            onClick = { viewModel.setGarmentCategory(selectedPiece, null) },
                            label = { Text("Auto") },
                        )
                    }
                    items(categoryChips.size) { i ->
                        val (category, label) = categoryChips[i]
                        FilterChip(
                            selected = active.category == category,
                            onClick = { viewModel.setGarmentCategory(selectedPiece, category) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("BACKDROP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            val selectedBackdrop by viewModel.backdrop.collectAsState()
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Backdrop.entries.size) { index ->
                    val backdrop = Backdrop.entries[index]
                    FilterChip(
                        selected = selectedBackdrop == backdrop,
                        onClick = { viewModel.setBackdrop(backdrop) },
                        label = { Text(backdrop.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = ::pickFromGallery, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (outfit.isEmpty()) "Gallery" else "Add piece")
                }
                OutlinedButton(onClick = ::launchCamera, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Camera")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onNext,
                enabled = outfit.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text("Cast the models")
            }
        }
    }
}

private val categoryChips = listOf(
    GarmentCategory.DRESS to "Dress",
    GarmentCategory.FULL_COVERAGE to "Abaya / Kaftan",
    GarmentCategory.HEADSCARF to "Hijab / Dupatta",
    GarmentCategory.UPPER_BODY to "Kurta / Top",
    GarmentCategory.LOWER_BODY to "Trousers",
)
