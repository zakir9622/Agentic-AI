package com.zakir.vestra.ui.screens.code

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.ExamplePromptRow
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassOptionToggle
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.screens.create.ResultPane

@Composable
fun CodeStudioScreen(
    viewModel: GenerativeViewModel,
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    val prompt by viewModel.prompt.collectAsState()
    val state by viewModel.state.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val creative by viewModel.creativeMode.collectAsState()
    val pragmatic by viewModel.pragmaticMode.collectAsState()
    val selectedId by viewModel.appSettings.codeProviderId.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(AiCapability.CODE)
    val estimate = viewModel.usage.estimateNext(provider)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing
    val assistCount = listOf(pragmatic, creative).count { it }
    var showModelPicker by remember { mutableStateOf(false) }
    val pickerModels = remember { CloudModelCatalog.forCapability(AiCapability.CODE) }

    fun leave() {
        if (busy) viewModel.forceStop(showStopped = false)
        onBack()
    }
    BackHandler { leave() }

    GlassScreen(title = LookbookCopy.STUDIO_CODE, subtitle = "Free open models", onBack = ::leave) {
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
            onModelClick = { showModelPicker = true },
            onSend = viewModel::generateCode,
            onStop = { viewModel.forceStop() },
            placeholder = "Ask for code… Kotlin Compose glass card with frosted border",
            assistToggles = {
                GlassOptionToggle(
                    text = LookbookCopy.ASSIST_PRAGMATIC,
                    active = pragmatic,
                    enabled = !busy,
                    onToggle = { viewModel.setPragmaticMode(!pragmatic) },
                )
                GlassOptionToggle(
                    text = LookbookCopy.ASSIST_CREATIVE,
                    active = creative,
                    enabled = !busy,
                    onToggle = { viewModel.setCreativeMode(!creative) },
                )
            },
        )

        Spacer(Modifier.height(12.dp))
        Text("EXAMPLES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        ExamplePromptRow(
            examples = listOf(
                "Write a Kotlin Compose frosted glass card with border highlight",
                "Explain how to resume an Android OkHttp download with Range headers",
                "Refactor this into a StateFlow ViewModel pattern (paste code)",
            ),
            enabled = !busy,
            onPick = viewModel::setPrompt,
        )

        if (preflight != null) {
            Spacer(Modifier.height(12.dp))
            GlassErrorBanner(
                message = preflight!!,
                onRetry = onOpenSettings ?: { showModelPicker = true },
                retryLabel = if (onOpenSettings != null) LookbookCopy.ACTION_OPEN_SETTINGS else "Choose model",
                onDismiss = { viewModel.clearResult() },
            )
        }

        Spacer(Modifier.height(12.dp))
        ResultPane(
            state = state,
            onCancel = { viewModel.forceStop() },
            onRetry = {
                viewModel.clearResult()
                viewModel.generateCode()
            },
            onDismiss = viewModel::clearResult,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showModelPicker) {
        ModelPickerSheet(
            title = "Coding models",
            models = pickerModels,
            selectedId = selectedId,
            onSelect = { viewModel.appSettings.setCodeProvider(it.id) },
            onDismiss = { showModelPicker = false },
        )
    }
}
