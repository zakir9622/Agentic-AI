package com.zakir.vestra.ui.screens.settings

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.storage.TokenSidecar
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Master switch for cloud generation — off by default, local-only until enabled. */
internal fun LazyListScope.settingsCloudMasterToggleSection(appSettings: AppSettings) {
    item(key = "cloud-master-toggle") {
        val cloudEnabled by appSettings.cloudModelsEnabled.collectAsState()
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable cloud models", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (cloudEnabled) {
                            "Cloud generation is on — Groq / OpenRouter / HF models are available below."
                        } else {
                            "Off by default — generation runs local-only. Turn on to use free cloud models."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = cloudEnabled,
                    onCheckedChange = appSettings::setCloudModelsEnabled,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

/** API keys card + per-capability cloud model dropdowns. */
internal fun LazyListScope.settingsCloudKeysSection(
    appSettings: AppSettings,
    hfTokenSaved: Boolean,
    hfInput: String,
    groqInput: String,
    openRouterInput: String,
    onHfInput: (String) -> Unit,
    onGroqInput: (String) -> Unit,
    onOpenRouterInput: (String) -> Unit,
    keysSavedFlash: Boolean,
    clipboardHint: String?,
    durableReady: Boolean,
    onApplyClipboard: () -> Boolean,
    onOpenPortal: (String) -> Unit,
    onSaveTokens: () -> Unit,
    importTokensLauncher: ManagedActivityResultLauncher<Array<String>, android.net.Uri?>,
    onKeysLoadedFromDocuments: (count: Int) -> Unit,
) {
    item(key = "keys") {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        GlassCard {
            GlassSectionLabel("API KEYS")
            Text(
                "Create a free classic HF Write/Read key with Inference Providers (not fine-grained discussion-only), copy it, then Save — or import tokens.json / tokens.txt. Clipboard keys are detected automatically. Local Lite/Pro packs never need a key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.HF) },
                    modifier = Modifier.weight(1f),
                ) { Text("Hugging Face") }
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.GROQ) },
                    modifier = Modifier.weight(1f),
                ) { Text("Groq") }
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.OPENROUTER) },
                    modifier = Modifier.weight(1f),
                ) { Text("OpenRouter") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (!onApplyClipboard()) {
                        Toast.makeText(
                            context,
                            "No Hugging Face / Groq / OpenRouter key found on clipboard",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Paste key from clipboard")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    importTokensLauncher.launch(
                        arrayOf(
                            "application/json",
                            "text/plain",
                            "text/*",
                            "*/*",
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import tokens from JSON / TXT file")
            }
            if (durableReady) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val count = withContext(Dispatchers.IO) {
                                TokenSidecar.autoFetchFromDocuments(
                                    appSettings,
                                    overwriteExisting = true,
                                )
                            }
                            onKeysLoadedFromDocuments(count)
                            Toast.makeText(
                                context,
                                if (count > 0) {
                                    "Loaded $count key(s) from Documents/TheLookbook"
                                } else {
                                    "No tokens.json / tokens.txt found in Documents/TheLookbook"
                                },
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Auto-fetch from Documents/TheLookbook")
                }
            }
            clipboardHint?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.labelMedium, color = VestraColors.Accent)
            }
            Spacer(Modifier.height(10.dp))
            KeyField("Hugging Face API key", hfInput, onHfInput)
            KeyField("Groq API key", groqInput, onGroqInput)
            KeyField("OpenRouter API key (free models)", openRouterInput, onOpenRouterInput)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSaveTokens,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (keysSavedFlash) "Saved" else LookbookCopy.ACTION_SAVE_TOKENS)
            }
            if (hfTokenSaved) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "HF token saved · for Code use curated Qwen2.5-Coder / Groq (not random auto-listed models).",
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.Accent,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

internal fun LazyListScope.settingsCloudCapabilitiesSection(
    appSettings: AppSettings,
    freeCloudDiscovery: FreeCloudDiscovery,
    tryOnId: String,
    imageGenId: String,
    imageEditId: String,
    codeId: String,
    videoId: String,
    audioId: String,
) {
    item(key = "cap-tryon") {
        CloudCapabilityDropdown(
            title = "CLOUD TRY-ON",
            capability = AiCapability.TRY_ON,
            selectedId = tryOnId,
            appSettings = appSettings,
            discovery = freeCloudDiscovery,
            onSelect = appSettings::setCloudProvider,
        )
    }
    item(key = "cap-gen") {
        CloudCapabilityDropdown(
            title = "IMAGE GENERATION",
            capability = AiCapability.IMAGE_GEN,
            selectedId = imageGenId,
            appSettings = appSettings,
            discovery = freeCloudDiscovery,
            onSelect = appSettings::setImageGenProvider,
        )
    }
    item(key = "cap-edit") {
        CloudCapabilityDropdown(
            title = "IMAGE EDIT / RECREATE",
            capability = AiCapability.IMAGE_EDIT,
            selectedId = imageEditId,
            appSettings = appSettings,
            discovery = freeCloudDiscovery,
            onSelect = appSettings::setImageEditProvider,
        )
    }
    item(key = "cap-code") {
        CloudCapabilityDropdown(
            title = "CODING MODELS",
            capability = AiCapability.CODE,
            selectedId = codeId,
            appSettings = appSettings,
            discovery = freeCloudDiscovery,
            onSelect = appSettings::setCodeProvider,
        )
    }
    item(key = "cap-video") {
        CloudCapabilityDropdown(
            title = "VIDEO MODELS",
            capability = AiCapability.VIDEO,
            selectedId = videoId,
            appSettings = appSettings,
            discovery = freeCloudDiscovery,
            onSelect = appSettings::setVideoProvider,
        )
    }
    item(key = "cap-audio") {
        CloudCapabilityDropdown(
            title = "AUDIO / TTS MODELS",
            capability = AiCapability.AUDIO,
            selectedId = audioId,
            appSettings = appSettings,
            discovery = freeCloudDiscovery,
            onSelect = appSettings::setAudioProvider,
        )
    }
}
