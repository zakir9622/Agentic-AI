package com.zakir.vestra.ui.screens.code

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
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
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassGenerateActions
import com.zakir.vestra.ui.components.GlassOptionToggle
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
    val preflight by viewModel.preflightMessage.collectAsState()
    val creative by viewModel.creativeMode.collectAsState()
    val pragmatic by viewModel.pragmaticMode.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(AiCapability.CODE)
    val estimate = viewModel.usage.estimateNext(provider)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing

    GlassScreen(title = "Code Studio", subtitle = "Free open coding models", onBack = onBack) {
        GlassCard {
            GlassSectionLabel("FREE MODEL & USAGE")
            Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
            Text(estimate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Groq / HF Inference / OpenRouter :free only. Token counts appear after each run. No paid APIs.",
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
                enabled = !busy,
                placeholder = {
                    Text("Ask for code… e.g. Write a Kotlin Compose glass card with frosted border")
                },
            )
            Spacer(Modifier.height(10.dp))
            GlassSectionLabel("MODEL OPTIONS")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassOptionToggle(
                    text = "Pragmatic (fewer refusals)",
                    active = pragmatic,
                    enabled = !busy,
                    onToggle = { viewModel.setPragmaticMode(!pragmatic) },
                )
                GlassOptionToggle(
                    text = "Creative",
                    active = creative,
                    enabled = !busy,
                    onToggle = { viewModel.setCreativeMode(!creative) },
                )
            }
            Text(
                "Pragmatic reduces soft refusals. Creative raises temperature. Failed calls auto-retry with a clearer prompt.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text("EXAMPLES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            com.zakir.vestra.ui.components.ExamplePromptRow(
                examples = listOf(
                    "Write a Kotlin Compose frosted glass card with border highlight",
                    "Explain how to resume an Android OkHttp download with Range headers",
                    "Refactor this into a StateFlow ViewModel pattern (paste code)",
                ),
                enabled = !busy,
                onPick = viewModel::setPrompt,
            )
            Spacer(Modifier.height(12.dp))
            GlassGenerateActions(
                busy = busy,
                generateLabel = "Generate code (free)",
                onGenerate = viewModel::generateCode,
                onStop = { viewModel.forceStop() },
                enabled = prompt.isNotBlank(),
            )
        }

        if (preflight != null) {
            Spacer(Modifier.height(12.dp))
            GlassErrorBanner(message = preflight!!, onDismiss = { viewModel.clearResult() })
        }

        Spacer(Modifier.height(12.dp))
        ResultPane(
            state = state,
            onRetry = {
                viewModel.clearResult()
                viewModel.generateCode()
            },
            onDismiss = viewModel::clearResult,
        )
        Spacer(Modifier.height(24.dp))
    }
}
