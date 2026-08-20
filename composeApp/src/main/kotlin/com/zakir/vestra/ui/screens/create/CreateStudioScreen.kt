package com.zakir.vestra.ui.screens.create

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassLoadingCard
import com.zakir.vestra.ui.components.GlassPrimaryButton
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
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
    val provider = viewModel.appSettings.selectedProvider(
        if (reference == null) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT,
    )
    val estimate = viewModel.usage.estimateNext(provider)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setReference(uri?.toString())
    }

    GlassScreen(title = "Create Studio", subtitle = "Free prompt image", onBack = onBack) {
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
                    Text(
                        if (reference == null) {
                            "Describe the image… e.g. emerald abaya in a Lahore bazaar at golden hour"
                        } else {
                            "Describe how to recreate / edit… e.g. change to navy silk, soft studio light"
                        },
                    )
                },
            )
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = if (reference == null) "Add reference image (recreate)" else "Change reference image",
                onClick = {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !busy,
            )
            if (reference != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = reference,
                    contentDescription = "Reference",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop,
                )
                GlassSecondaryButton(text = "Clear reference", onClick = { viewModel.setReference(null) }, enabled = !busy)
            }
            Spacer(Modifier.height(12.dp))
            if (busy) {
                GlassSecondaryButton(text = "Cancel", onClick = viewModel::cancel)
            } else {
                GlassPrimaryButton(
                    text = if (reference == null) "Generate image (free)" else "Recreate from prompt (free)",
                    onClick = viewModel::generateImage,
                    enabled = prompt.isNotBlank(),
                )
            }
        }

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
    val clipboard = LocalClipboardManager.current
    when (state) {
        null -> Unit
        is GenerativeState.Preparing -> GlassLoadingCard(state.message)
        is GenerativeState.Running -> GlassLoadingCard(state.stage, progress = state.fraction)
        is GenerativeState.ImageReady -> GlassCard {
            GlassSectionLabel("RESULT")
            AsyncImage(
                model = File(state.path),
                contentDescription = "Generated",
                modifier = Modifier.fillMaxWidth().height(320.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = "Share image",
                onClick = {
                    val file = File(state.path)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            "Share",
                        ),
                    )
                },
            )
        }
        is GenerativeState.VideoReady -> GlassCard {
            GlassSectionLabel("VIDEO READY")
            Text("Saved on device. Open with a free system video player.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(state.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = "Open / share video",
                onClick = {
                    val file = File(state.path)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val view = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(view) }.onFailure {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Share video",
                            ),
                        )
                    }
                },
            )
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
                    clipboard.setText(AnnotatedString(state.text))
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
