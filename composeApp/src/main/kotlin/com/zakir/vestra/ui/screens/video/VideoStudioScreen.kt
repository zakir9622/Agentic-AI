package com.zakir.vestra.ui.screens.video

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
fun VideoStudioScreen(
    viewModel: GenerativeViewModel,
    onBack: () -> Unit,
) {
    val prompt by viewModel.prompt.collectAsState()
    val state by viewModel.state.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val detailBoost by viewModel.detailBoost.collectAsState()
    val fashionContext by viewModel.fashionContext.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(AiCapability.VIDEO)
    val estimate = viewModel.usage.estimateNext(provider)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing

    GlassScreen(title = "Video Studio", subtitle = "Creators · free HF only", onBack = onBack) {
        GlassCard {
            GlassSectionLabel("FREE MODEL")
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
                enabled = !busy,
                placeholder = {
                    Text("Describe the clip… e.g. woman in black abaya walking through a Karachi night bazaar")
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
                    text = "Fashion context",
                    active = fashionContext,
                    enabled = !busy,
                    onToggle = { viewModel.setFashionContext(!fashionContext) },
                )
                GlassOptionToggle(
                    text = "Detail boost",
                    active = detailBoost,
                    enabled = !busy,
                    onToggle = { viewModel.setDetailBoost(!detailBoost) },
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("EXAMPLES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            com.zakir.vestra.ui.components.ExamplePromptRow(
                examples = listOf(
                    "Woman in black abaya walking through a Karachi night bazaar",
                    "Slow pan across embroidered green shalwar kameez in soft daylight",
                    "Hijabi model turning toward camera, linen texture detail",
                ),
                enabled = !busy,
                onPick = viewModel::setPrompt,
            )
            Spacer(Modifier.height(12.dp))
            GlassGenerateActions(
                busy = busy,
                generateLabel = "Generate video (free)",
                onGenerate = viewModel::generateVideo,
                onStop = { viewModel.forceStop() },
                enabled = prompt.isNotBlank(),
                statusText = "Rendering… leave anytime — Force stop cancels",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Free HF Spaces can queue at peak hours. No paid video APIs are used.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                viewModel.generateVideo()
            },
            onDismiss = viewModel::clearResult,
        )
        Spacer(Modifier.height(24.dp))
    }
}
