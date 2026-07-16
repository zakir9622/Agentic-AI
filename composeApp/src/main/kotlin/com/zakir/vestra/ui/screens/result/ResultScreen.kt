package com.zakir.vestra.ui.screens.result

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.zakir.vestra.shared.domain.GenerationState
import java.io.File

/**
 * The reveal. Before/after slider + film grain arrive with M6 polish;
 * save/share/report are fully functional now.
 */
@Composable
fun ResultScreen(
    viewModel: com.zakir.vestra.ui.TryOnViewModel,
    onNewLook: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.generation.collectAsState()
    val result = (state as? GenerationState.Complete)?.result
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        ReportDialog(onDismiss = { showReportDialog = false })
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "The reveal",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "AI-generated image",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (result != null) {
                    AsyncImage(
                        model = File(result.imagePath),
                        contentDescription = "Generated try-on result",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        "No result to show — start a new look.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (result != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { saveToGallery(context, File(result.imagePath)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = { share(context, File(result.imagePath)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Share")
                    }
                    OutlinedButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Flag, contentDescription = "Report this image")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onNewLook,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text("New look")
            }
        }
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit) {
    // Report intake queues to the `report` Edge Function in M5; acknowledging
    // locally now keeps the Play-required affordance present from day one.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this image") },
        text = {
            Text(
                "If this generation is offensive or misuses someone's likeness, " +
                    "report it and we'll review. Reports are anonymous.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Report") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun saveToGallery(context: Context, file: File) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Vestra")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri == null) {
        Toast.makeText(context, "Couldn't save image", Toast.LENGTH_SHORT).show()
        return
    }
    resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
    Toast.makeText(context, "Saved to Pictures/Vestra", Toast.LENGTH_SHORT).show()
}

private fun share(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share look"))
}
