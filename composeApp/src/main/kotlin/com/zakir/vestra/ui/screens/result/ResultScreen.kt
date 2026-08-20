package com.zakir.vestra.ui.screens.result

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.zakir.vestra.data.LocalReportStore
import com.zakir.vestra.data.ReportReason
import com.zakir.vestra.ui.components.GlassEmptyState
import com.zakir.vestra.ui.components.GlassImageFrame
import com.zakir.vestra.ui.components.GlassPrimaryButton
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground
import java.io.File

@Composable
fun ResultScreen(
    viewModel: com.zakir.vestra.ui.TryOnViewModel,
    onNewLook: () -> Unit,
    onBackToStudio: () -> Unit,
) {
    val context = LocalContext.current
    val reportStore = remember { LocalReportStore(context) }
    val shoot by viewModel.shoot.collectAsState()
    val results = shoot?.completed.orEmpty()
    var selectedShot by remember { mutableStateOf(0) }
    var showReport by remember { mutableStateOf(false) }
    val result = results.getOrNull(selectedShot.coerceIn(0, (results.size - 1).coerceAtLeast(0)))

    if (showReport && result != null) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report content") },
            text = {
                Column {
                    Text("Reports are stored on this device only (no paid services). Why are you reporting?")
                    Spacer(Modifier.height(8.dp))
                    ReportReason.entries.forEach { reason ->
                        TextButton(
                            onClick = {
                                reportStore.submit(result.imagePath, reason)
                                showReport = false
                                Toast.makeText(context, "Report saved locally", Toast.LENGTH_SHORT).show()
                            },
                        ) { Text(reason.label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReport = false }) { Text("Cancel") }
            },
        )
    }

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            GlassTopBar(
                title = "Your look",
                subtitle = if (results.size > 1) "${results.size} shots" else "Result",
                navigation = {
                    IconButton(onClick = onBackToStudio) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to studio")
                    }
                },
            )
            Spacer(Modifier.height(16.dp))

            val shotSources by viewModel.shots.collectAsState()
            val userPhoto = shotSources.filterIsInstance<com.zakir.vestra.shared.domain.PersonSource.UserPhoto>()
                .firstOrNull()

            if (result == null) {
                GlassEmptyState(
                    message = "No shots to show — start a new shoot.",
                    actionLabel = "New shoot",
                    onAction = onNewLook,
                    modifier = Modifier.weight(1f),
                )
            } else {
                GlassImageFrame(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (userPhoto != null && results.size == 1) {
                            BeforeAfter(beforeModel = userPhoto.uri, afterModel = File(result.imagePath))
                        } else {
                            AsyncImage(
                                model = File(result.imagePath),
                                contentDescription = "Photoshoot shot ${selectedShot + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }

                if (results.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(results) { index, shot ->
                            AsyncImage(
                                model = File(shot.imagePath),
                                contentDescription = "Shot ${index + 1}",
                                modifier = Modifier
                                    .height(72.dp)
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (index == selectedShot) 2.dp else 1.dp,
                                        color = if (index == selectedShot) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable { selectedShot = index },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassSecondaryButton(
                        text = "Save",
                        onClick = {
                            results.forEach { shot -> saveToGallery(context, File(shot.imagePath), quiet = true) }
                            Toast.makeText(
                                context,
                                if (results.size > 1) "${results.size} shots saved" else "Saved to Pictures/The Lookbook",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    GlassSecondaryButton(
                        text = "Share",
                        onClick = { share(context, File(result.imagePath)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                GlassSecondaryButton(text = "Report content", onClick = { showReport = true })
                Spacer(Modifier.height(10.dp))
            }

            GlassPrimaryButton(
                text = "New shoot",
                onClick = onNewLook,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun BeforeAfter(beforeModel: Any, afterModel: Any) {
    var fraction by remember { mutableStateOf(0.5f) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Before and after comparison. Drag horizontally to adjust." }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    fraction = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                }
            },
    ) {
        val widthPx = constraints.maxWidth
        AsyncImage(
            model = afterModel,
            contentDescription = "Generated try-on result",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        AsyncImage(
            model = beforeModel,
            contentDescription = "Original photo",
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
                },
            contentScale = ContentScale.Fit,
        )
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset { IntOffset((fraction * widthPx).toInt() - 1, 0) }
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
        )
    }
}

private fun saveToGallery(context: Context, file: File, quiet: Boolean = false) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/The Lookbook")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri == null) {
        if (!quiet) Toast.makeText(context, "Couldn't save image", Toast.LENGTH_SHORT).show()
        return
    }
    resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
    if (!quiet) Toast.makeText(context, "Saved to Pictures/The Lookbook", Toast.LENGTH_SHORT).show()
}

private fun share(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share look"))
}
