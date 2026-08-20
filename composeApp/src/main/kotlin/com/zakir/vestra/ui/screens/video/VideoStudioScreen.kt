package com.zakir.vestra.ui.screens.video

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.screens.create.ResultPane

@Composable
fun VideoStudioScreen(
    viewModel: GenerativeViewModel,
    onBack: () -> Unit,
) {
    val prompt by viewModel.prompt.collectAsState()
    val state by viewModel.state.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(AiCapability.VIDEO)
    val estimate = viewModel.usage.estimateNext(provider)

    GlassScreen(title = "Video Studio", subtitle = "Open video models", onBack = onBack) {
        GlassCard {
            GlassSectionLabel("MODEL & USAGE")
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
                    Text("Describe the clip… e.g. woman in black abaya walking through a Karachi night bazaar, cinematic")
                },
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.clearResult()
                    viewModel.generateVideo()
                },
                enabled = prompt.isNotBlank() && state !is GenerativeState.Running && state !is GenerativeState.Preparing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Generate video") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Free HF Spaces can queue. Paid FAL backends are faster — set keys in Settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        ResultPane(state)
        Spacer(Modifier.height(24.dp))
    }
}
