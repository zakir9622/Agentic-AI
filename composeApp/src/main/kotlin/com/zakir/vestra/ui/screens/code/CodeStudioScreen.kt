package com.zakir.vestra.ui.screens.code

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
fun CodeStudioScreen(
    viewModel: GenerativeViewModel,
    onBack: () -> Unit,
) {
    val prompt by viewModel.prompt.collectAsState()
    val state by viewModel.state.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(AiCapability.CODE)
    val estimate = viewModel.usage.estimateNext(provider)

    GlassScreen(title = "Code Studio", subtitle = "Open coding models", onBack = onBack) {
        GlassCard {
            GlassSectionLabel("MODEL & USAGE")
            Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
            Text(estimate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Change model and API keys in Settings. Token counts appear after each run.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        GlassCard {
            GlassSectionLabel("PROMPT")
            OutlinedTextField(
                value = prompt,
                onValueChange = viewModel::setPrompt,
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                placeholder = {
                    Text("Ask for code… e.g. Write a Kotlin Compose glass card with frosted border")
                },
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.clearResult()
                    viewModel.generateCode()
                },
                enabled = prompt.isNotBlank() && state !is GenerativeState.Running && state !is GenerativeState.Preparing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Generate code") }
        }

        Spacer(Modifier.height(12.dp))
        ResultPane(state)
        Spacer(Modifier.height(24.dp))
    }
}
