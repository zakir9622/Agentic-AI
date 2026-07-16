package com.zakir.vestra.ui.screens.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.zakir.vestra.ui.TryOnViewModel
import java.io.File

/**
 * Garment intake: pick from the photo gallery (no permission needed) or shoot
 * with the system camera into app-private storage via FileProvider.
 */
@Composable
fun GarmentScreen(
    viewModel: TryOnViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val garment by viewModel.garment.collectAsState()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.selectGarment(it.toString()) }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        if (saved) pendingCaptureUri?.let { viewModel.selectGarment(it.toString()) }
        pendingCaptureUri = null
    }

    fun launchCamera() {
        val captures = File(context.filesDir, "captures").apply { mkdirs() }
        val file = File(captures, "garment_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCaptureUri = uri
        captureLauncher.launch(uri)
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
                    Text("The garment", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Viewfinder-style stage for the chosen garment.
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
                val selected = garment
                if (selected != null) {
                    AsyncImage(
                        model = selected.uri,
                        contentDescription = "Selected garment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "A flat-lay, a hanger shot,\nor a catalog page.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        pickLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Gallery")
                }
                OutlinedButton(onClick = ::launchCamera, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Camera")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onNext,
                enabled = garment != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text("Choose who wears it")
            }
        }
    }
}
