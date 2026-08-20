package com.zakir.vestra.ui.screens.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import java.io.File

/** Prompt-driven image create / recreate studio. */
@Composable
fun CreateStudioScreen(
    viewModel: GenerativeViewModel,
    onBack: () -> Unit,
) {
    val prompt by viewModel.prompt.collectAsState()
    val reference by viewModel.referenceUri.collectAsState()
    val state by viewModel.state.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(
        if (reference == null) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT,
    )
    val estimate = viewModel.usage.estimateNext(provider)

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setReference(uri?.toString())
    }

    GlassScreen(title = "Create Studio", subtitle = "Prompt image", onBack = onBack) {
        GlassCard {
            GlassSectionLabel("MODEL")
            Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
            Text(estimate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))

        GlassCard {
            GlassSectionLabel("PROMPT")
            OutlinedTextField(
                value = prompt,
                onValueChange = viewModel::setPrompt,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = {
                    Text(
                        if (reference == null) {
                            "Describe the image you want… e.g. emerald abaya in a Lahore bazaar at golden hour"
                        } else {
                            "Describe how to recreate / edit the reference… e.g. change to navy silk, soft studio light"
                        },
                    )
                },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (reference == null) "Add reference image (recreate)" else "Change reference image")
            }
            if (reference != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = reference,
                    contentDescription = "Reference",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop,
                )
                OutlinedButton(onClick = { viewModel.setReference(null) }) { Text("Clear reference") }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.clearResult()
                    viewModel.generateImage()
                },
                enabled = prompt.isNotBlank() && state !is GenerativeState.Running && state !is GenerativeState.Preparing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (reference == null) "Generate image" else "Recreate from prompt")
            }
        }

        Spacer(Modifier.height(12.dp))
        ResultPane(state)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun ResultPane(state: GenerativeState?) {
    when (state) {
        null -> Unit
        is GenerativeState.Preparing -> GlassCard {
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        is GenerativeState.Running -> GlassCard {
            Text(state.stage, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { state.fraction }, modifier = Modifier.fillMaxWidth())
        }
        is GenerativeState.ImageReady -> GlassCard {
            GlassSectionLabel("RESULT")
            AsyncImage(
                model = File(state.path),
                contentDescription = "Generated",
                modifier = Modifier.fillMaxWidth().height(320.dp),
                contentScale = ContentScale.Fit,
            )
        }
        is GenerativeState.VideoReady -> GlassCard {
            GlassSectionLabel("VIDEO READY")
            Text(state.path, style = MaterialTheme.typography.bodySmall)
            Text("Saved under app generations. Open with a video player.", style = MaterialTheme.typography.bodyMedium)
        }
        is GenerativeState.CodeReady -> GlassCard {
            GlassSectionLabel("CODE · ${state.tokensIn + state.tokensOut} tokens")
            Text(
                "${state.tokensIn} in · ${state.tokensOut} out",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(state.text, style = MaterialTheme.typography.bodyMedium)
        }
        is GenerativeState.Failed -> GlassCard {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
    }
}
