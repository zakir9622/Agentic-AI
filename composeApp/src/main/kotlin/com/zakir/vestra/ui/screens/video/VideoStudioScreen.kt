package com.zakir.vestra.ui.screens.video

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.ExamplePromptRow
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassOptionToggle
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.PromptComposer
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
    val bypassFilter by viewModel.bypassFilter.collectAsState()
    val qualityGuard by viewModel.qualityGuard.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(AiCapability.VIDEO)
    val estimate = viewModel.usage.estimateNext(provider)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing
    val assistCount = listOf(bypassFilter, fashionContext, detailBoost, qualityGuard).count { it }

    GlassScreen(title = LookbookCopy.STUDIO_VIDEO, subtitle = "Free cloud clips", onBack = onBack) {
        Text(
            estimate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        PromptComposer(
            prompt = prompt,
            onPromptChange = viewModel::setPrompt,
            modelLabel = provider.displayName,
            assistCount = assistCount,
            busy = busy,
            enabled = true,
            onSend = viewModel::generateVideo,
            onStop = { viewModel.forceStop() },
            placeholder = "Describe the clip… abaya walking through a Karachi night bazaar",
            assistToggles = {
                GlassOptionToggle(
                    text = LookbookCopy.ASSIST_EDITORIAL,
                    active = bypassFilter,
                    enabled = !busy,
                    onToggle = { viewModel.setBypassFilter(!bypassFilter) },
                )
                GlassOptionToggle(
                    text = LookbookCopy.ASSIST_FASHION,
                    active = fashionContext,
                    enabled = !busy,
                    onToggle = { viewModel.setFashionContext(!fashionContext) },
                )
                GlassOptionToggle(
                    text = LookbookCopy.ASSIST_DETAIL,
                    active = detailBoost,
                    enabled = !busy,
                    onToggle = { viewModel.setDetailBoost(!detailBoost) },
                )
                GlassOptionToggle(
                    text = LookbookCopy.ASSIST_QUALITY,
                    active = qualityGuard,
                    enabled = !busy,
                    onToggle = { viewModel.setQualityGuard(!qualityGuard) },
                )
            },
        )

        Spacer(Modifier.height(12.dp))
        Text("EXAMPLES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        ExamplePromptRow(
            examples = listOf(
                "Woman in black abaya walking through a Karachi night bazaar",
                "Slow pan across embroidered green shalwar kameez in soft daylight",
                "Hijabi model turning toward camera, linen texture detail",
            ),
            enabled = !busy,
            onPick = viewModel::setPrompt,
        )

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
