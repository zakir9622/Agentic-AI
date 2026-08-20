package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudModelProvider
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.displayLabel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    onOpenPacks: () -> Unit,
    onOpenUsage: () -> Unit,
    onBack: () -> Unit,
) {
    val selectedTier by appSettings.engineTier.collectAsState()
    val tryOnId by appSettings.cloudProviderId.collectAsState()
    val imageGenId by appSettings.imageGenProviderId.collectAsState()
    val imageEditId by appSettings.imageEditProviderId.collectAsState()
    val codeId by appSettings.codeProviderId.collectAsState()
    val videoId by appSettings.videoProviderId.collectAsState()

    val hfToken by appSettings.hfToken.collectAsState()
    val replicateToken by appSettings.replicateToken.collectAsState()
    val falKey by appSettings.falApiKey.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()

    var hfInput by remember(hfToken) { mutableStateOf(hfToken.orEmpty()) }
    var replicateInput by remember(replicateToken) { mutableStateOf(replicateToken.orEmpty()) }
    var falInput by remember(falKey) { mutableStateOf(falKey.orEmpty()) }
    var groqInput by remember(groqKey) { mutableStateOf(groqKey.orEmpty()) }
    var openRouterInput by remember(openRouterKey) { mutableStateOf(openRouterKey.orEmpty()) }

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            GlassTopBar(
                title = "Settings",
                navigation = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )

            Spacer(Modifier.height(20.dp))

            GlassCard {
                GlassSectionLabel("TRY-ON ENGINE")
                EngineTier.entries.forEach { tier ->
                    val availability = if (tier == EngineTier.AUTO) Availability.Ready else engineRouter.availability(tier)
                    val enabled = availability == Availability.Ready
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = tier == selectedTier, enabled = enabled) {
                                appSettings.setEngineTier(tier)
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tier == selectedTier, enabled = enabled, onClick = { appSettings.setEngineTier(tier) })
                        Column {
                            Text(tier.label(), style = MaterialTheme.typography.titleMedium)
                            Text(tier.description(availability), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            GlassCard(onClick = onOpenPacks) {
                GlassSectionLabel("ON-DEVICE MODELS")
                Text("Model packs", style = MaterialTheme.typography.titleMedium)
                Text("Download Pro/Lite for offline garment try-on.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(14.dp))
            GlassCard(onClick = onOpenUsage) {
                GlassSectionLabel("USAGE")
                Text("Token & cost ledger", style = MaterialTheme.typography.titleMedium)
                Text("See requests, tokens, and estimated spend per model.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            CapabilityPicker(
                title = "CLOUD TRY-ON MODELS",
                capability = AiCapability.TRY_ON,
                selectedId = tryOnId,
                onSelect = appSettings::setCloudProvider,
            )
            CapabilityPicker(
                title = "IMAGE GENERATION",
                capability = AiCapability.IMAGE_GEN,
                selectedId = imageGenId,
                onSelect = appSettings::setImageGenProvider,
            )
            CapabilityPicker(
                title = "IMAGE RECREATE / EDIT",
                capability = AiCapability.IMAGE_EDIT,
                selectedId = imageEditId,
                onSelect = appSettings::setImageEditProvider,
            )
            CapabilityPicker(
                title = "CODING MODELS",
                capability = AiCapability.CODE,
                selectedId = codeId,
                onSelect = appSettings::setCodeProvider,
            )
            CapabilityPicker(
                title = "VIDEO MODELS",
                capability = AiCapability.VIDEO,
                selectedId = videoId,
                onSelect = appSettings::setVideoProvider,
            )

            Spacer(Modifier.height(14.dp))
            GlassCard {
                GlassSectionLabel("API KEYS")
                Text(
                    "HF token improves free Spaces + unlocks Inference. Groq/OpenRouter for coding. Replicate/FAL for paid media.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                KeyField("Hugging Face token", hfInput) { hfInput = it }
                KeyField("Groq API key", groqInput) { groqInput = it }
                KeyField("OpenRouter API key", openRouterInput) { openRouterInput = it }
                KeyField("Replicate API token", replicateInput) { replicateInput = it }
                KeyField("FAL API key", falInput) { falInput = it }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Keys: huggingface.co/settings/tokens · console.groq.com · openrouter.ai/keys · replicate.com/account · fal.ai/dashboard",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        appSettings.setHfToken(hfInput)
                        appSettings.setGroqApiKey(groqInput)
                        appSettings.setOpenRouterApiKey(openRouterInput)
                        appSettings.setReplicateToken(replicateInput)
                        appSettings.setFalApiKey(falInput)
                    },
                ) { Text("Save API keys") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CapabilityPicker(
    title: String,
    capability: AiCapability,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Spacer(Modifier.height(14.dp))
    GlassCard {
        GlassSectionLabel(title)
        Text(capability.displayLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        CloudModelCatalog.forCapability(capability)
            .groupBy { it.platform }
            .forEach { (platform, providers) ->
                Text(
                    platform.sectionTitle(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                providers.forEach { provider ->
                    CloudProviderRow(
                        provider = provider,
                        selected = provider.id == selectedId,
                        onSelect = { onSelect(provider.id) },
                    )
                }
            }
    }
}

@Composable
private fun KeyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
    )
}

@Composable
private fun CloudProviderRow(
    provider: CloudModelProvider,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(provider.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        provider.freeTier && provider.estCostUsd <= 0.0 -> "Free"
                        provider.estCostUsd > 0.0 -> "~$${"%.3f".format(provider.estCostUsd)}"
                        else -> "Paid"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(provider.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val meta = buildString {
                append(provider.license)
                append(" · Q")
                append(provider.qualityScore)
                if (provider.estTokensPerRequest > 0) {
                    append(" · ~")
                    append(provider.estTokensPerRequest)
                    append(" tok")
                }
                if (provider.usageNote.isNotBlank()) {
                    append(" · ")
                    append(provider.usageNote)
                }
            }
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun CloudPlatform.sectionTitle(): String = when (this) {
    CloudPlatform.HF_SPACE -> "Hugging Face Spaces (free)"
    CloudPlatform.HF_INFERENCE -> "Hugging Face Inference"
    CloudPlatform.REPLICATE -> "Replicate (API key)"
    CloudPlatform.FAL -> "fal.ai (API key)"
    CloudPlatform.GROQ -> "Groq (free tier)"
    CloudPlatform.OPENROUTER -> "OpenRouter"
}

private fun EngineTier.label(): String = when (this) {
    EngineTier.AUTO -> "Auto (on-device)"
    EngineTier.LITE -> "Lite — on device"
    EngineTier.PRO -> "Pro — on device"
    EngineTier.CLOUD -> "Cloud — open-source try-on"
}

private fun EngineTier.description(availability: Availability): String {
    val base = when (this) {
        EngineTier.AUTO -> "Best on-device engine. Never uses cloud automatically."
        EngineTier.LITE -> "Fast compositor. Works offline on every phone."
        EngineTier.PRO -> "SD1.5 diffusion on-device. Needs Pro pack."
        EngineTier.CLOUD -> "HF / Replicate / FAL try-on. Select model below."
    }
    return when (availability) {
        Availability.Ready -> base
        is Availability.Unavailable -> when (availability.reason) {
            UnavailableReason.PACK_NOT_INSTALLED -> "$base\nModel pack not installed."
            UnavailableReason.DEVICE_NOT_CAPABLE -> "$base\nDevice doesn't meet RAM requirements."
            UnavailableReason.OFFLINE -> "$base\nNo internet connection."
            UnavailableReason.NOT_CONFIGURED -> "$base\nAdd the required API key above."
        }
    }
}
