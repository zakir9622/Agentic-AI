package com.zakir.vestra.ui.screens.create

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.media.MediaExport
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.ExamplePromptRow
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassLoadingCard
import com.zakir.vestra.ui.components.GlassOptionToggle
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.PromptComposer
import java.io.File

@Composable
fun CreateStudioScreen(
    viewModel: GenerativeViewModel,
    onBack: () -> Unit,
) {
    val prompt by viewModel.prompt.collectAsState()
    val reference by viewModel.referenceUri.collectAsState()
    val state by viewModel.state.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val detailBoost by viewModel.detailBoost.collectAsState()
    val fashionContext by viewModel.fashionContext.collectAsState()
    val bypassFilter by viewModel.bypassFilter.collectAsState()
    val qualityGuard by viewModel.qualityGuard.collectAsState()
    val provider = viewModel.appSettings.selectedProvider(
        if (reference == null) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT,
    )
    val estimate = viewModel.usage.estimateNext(provider)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing
    val assistCount = listOf(bypassFilter, fashionContext, detailBoost, qualityGuard).count { it }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setReference(uri?.toString())
    }

    GlassScreen(title = LookbookCopy.STUDIO_IMAGE, subtitle = "Free cloud stills", onBack = onBack) {
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
            onSend = viewModel::generateImage,
            onStop = { viewModel.forceStop() },
            placeholder = if (reference == null) {
                "Describe the image… emerald abaya in a Lahore bazaar"
            } else {
                "Describe the edit… change to navy silk, soft studio light"
            },
            referenceUri = reference,
            onAddReference = {
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onClearReference = { viewModel.setReference(null) },
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
                "Emerald abaya in a Lahore bazaar at golden hour",
                "Soft studio light, navy silk hijab, ivory backdrop",
                "Black niqab portrait, shallow depth of field",
            ),
            enabled = !busy,
            onPick = viewModel::setPrompt,
        )

        if (preflight != null) {
            Spacer(Modifier.height(12.dp))
            GlassErrorBanner(message = preflight!!, onDismiss = { viewModel.clearResult() })
        }

        Spacer(Modifier.height(12.dp))
        ResultPane(state, onRetry = {
            viewModel.clearResult()
            viewModel.generateImage()
        }, onDismiss = viewModel::clearResult)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun ResultPane(
    state: GenerativeState?,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    when (state) {
        null -> Unit
        is GenerativeState.Preparing -> GlassLoadingCard(state.message)
        is GenerativeState.Running -> GlassLoadingCard(state.stage, progress = state.fraction)
        is GenerativeState.ImageReady -> GlassCard {
            GlassSectionLabel("RESULT")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(text = "AI-generated", active = true)
                GlassPill(text = "In looks gallery", active = true, accent = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = File(state.path),
                contentDescription = "Generated",
                modifier = Modifier.fillMaxWidth().height(320.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSecondaryButton(
                    text = "Save to Photos",
                    onClick = { MediaExport.saveImageToGallery(context, File(state.path)) },
                    modifier = Modifier.weight(1f),
                )
                GlassSecondaryButton(
                    text = "Share",
                    onClick = { MediaExport.share(context, File(state.path), "Share image") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        is GenerativeState.VideoReady -> GlassCard {
            GlassSectionLabel("VIDEO READY")
            GlassPill(text = "AI-generated", active = true)
            Spacer(Modifier.height(8.dp))
            Text(
                "Saved on device. Save to Movies/The Lookbook or open with a system player.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(state.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSecondaryButton(
                    text = "Save to Gallery",
                    onClick = { MediaExport.saveVideoToGallery(context, File(state.path)) },
                    modifier = Modifier.weight(1f),
                )
                GlassSecondaryButton(
                    text = "Open / share",
                    onClick = { MediaExport.openVideo(context, File(state.path)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        is GenerativeState.CodeReady -> GlassCard {
            GlassSectionLabel("CODE · ${state.tokensIn + state.tokensOut} free tokens")
            Text(
                "${state.tokensIn} in · ${state.tokensOut} out",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(state.text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = "Copy code",
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("lookbook-code", state.text))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                },
            )
        }
        is GenerativeState.Failed -> GlassErrorBanner(
            message = state.message,
            onRetry = onRetry,
            onDismiss = onDismiss,
        )
    }
}
