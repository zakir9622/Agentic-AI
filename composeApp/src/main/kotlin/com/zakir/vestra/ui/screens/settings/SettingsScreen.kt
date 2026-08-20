package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelProvider
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.PackStatus
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.util.rememberPackDownloadStarter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    packManager: ModelPackManager,
    freeCloudDiscovery: FreeCloudDiscovery,
    onOpenPacks: () -> Unit,
    onOpenUsage: () -> Unit,
    onBack: () -> Unit,
) {
    val selectedTier by appSettings.engineTier.collectAsState()
    val packStates by packManager.states.collectAsState()
    val startDownload = rememberPackDownloadStarter(showToast = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { packManager.refresh() }

    val tryOnId by appSettings.cloudProviderId.collectAsState()
    val imageGenId by appSettings.imageGenProviderId.collectAsState()
    val imageEditId by appSettings.imageEditProviderId.collectAsState()
    val codeId by appSettings.codeProviderId.collectAsState()
    val videoId by appSettings.videoProviderId.collectAsState()

    val hfToken by appSettings.hfToken.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()

    var hfInput by remember(hfToken) { mutableStateOf(hfToken.orEmpty()) }
    var groqInput by remember(groqKey) { mutableStateOf(groqKey.orEmpty()) }
    var openRouterInput by remember(openRouterKey) { mutableStateOf(openRouterKey.orEmpty()) }
    var keysSavedFlash by remember { mutableStateOf(false) }

    val localPackChoices = remember { LocalModelCatalog.entries.filter { it.packId != null && it.runnable } }
    var selectedPackId by remember {
        mutableStateOf(localPackChoices.firstOrNull()?.packId.orEmpty())
    }

    SpatialBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item(key = "top") {
                GlassTopBar(
                    title = "Settings",
                    subtitle = "Keys · engines · models",
                    navigation = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            // —— Tokens first (this is where HF / Groq / OpenRouter go) ——
            item(key = "keys") {
                GlassCard {
                    GlassSectionLabel("API TOKENS")
                    Text(
                        "Paste free-tier tokens here. Cloud models below unlock automatically from these keys. Local Lite/Pro never need a token.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    KeyField("Hugging Face token", hfInput) { hfInput = it }
                    KeyField("Groq API key", groqInput) { groqInput = it }
                    KeyField("OpenRouter API key (:free models)", openRouterInput) { openRouterInput = it }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "huggingface.co/settings/tokens · console.groq.com · openrouter.ai/keys",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            appSettings.setHfToken(hfInput.trim().ifBlank { null })
                            appSettings.setGroqApiKey(groqInput.trim().ifBlank { null })
                            appSettings.setOpenRouterApiKey(openRouterInput.trim().ifBlank { null })
                            keysSavedFlash = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (keysSavedFlash) "Saved" else "Save tokens")
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "engine") {
                GlassCard {
                    GlassSectionLabel("LOCAL TRY-ON ENGINE")
                    Text(
                        "On-device engines. Cloud is never chosen by Auto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    EngineDropdown(
                        selected = selectedTier,
                        availability = { tier ->
                            if (tier == EngineTier.AUTO) Availability.Ready else engineRouter.availability(tier)
                        },
                        onSelect = appSettings::setEngineTier,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "local-pack") {
                GlassCard {
                    GlassSectionLabel("LOCAL MODEL PACK")
                    Text(
                        "Select a pack, then download. Transfers resume if interrupted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    PackDropdown(
                        choices = localPackChoices.map { it.packId!! to "${it.displayName} · ${it.approxSizeLabel}" },
                        selectedId = selectedPackId,
                        onSelect = { selectedPackId = it },
                    )
                    val status = packStates[selectedPackId]?.status
                    val progress = packStates[selectedPackId]?.progress ?: 0f
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (status) {
                            PackStatus.INSTALLED -> "Installed — ready offline"
                            PackStatus.DOWNLOADING -> "Downloading ${(progress * 100).toInt()}%…"
                            PackStatus.INCOMPATIBLE -> "This device doesn’t meet pack requirements"
                            PackStatus.UPDATE_AVAILABLE -> "Update available"
                            PackStatus.NOT_INSTALLED -> if (progress > 0f) "Partial download — can resume" else "Not installed"
                            null -> "Catalog loading…"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (selectedPackId.isNotBlank()) startDownload(selectedPackId) },
                            enabled = status != PackStatus.INCOMPATIBLE && selectedPackId.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                when (status) {
                                    PackStatus.INSTALLED -> "Re-download"
                                    PackStatus.DOWNLOADING -> "Downloading…"
                                    else -> if (progress > 0f) "Resume" else "Download"
                                },
                            )
                        }
                        OutlinedButton(onClick = onOpenPacks, modifier = Modifier.weight(1f)) {
                            Text("All packs")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "usage") {
                GlassCard(onClick = onOpenUsage) {
                    GlassSectionLabel("USAGE")
                    Text("Token & cost ledger", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cloud requests only — local packs use \$0 tokens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDropdown(
    selected: EngineTier,
    availability: (EngineTier) -> Availability,
    onSelect: (EngineTier) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { EngineTier.entries }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = {
                Text(selected.description(availability(selected)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { tier ->
                val avail = availability(tier)
                val enabled = avail == Availability.Ready || tier == EngineTier.CLOUD || tier == EngineTier.AUTO
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(tier.label())
                            Text(
                                tier.description(avail),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        if (enabled) {
                            onSelect(tier)
                            expanded = false
                        }
                    },
                    enabled = enabled,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackDropdown(
    choices: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = choices.firstOrNull { it.first == selectedId }?.second ?: "Select a pack"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudCapabilityDropdown(
    title: String,
    capability: AiCapability,
    selectedId: String,
    appSettings: AppSettings,
    discovery: FreeCloudDiscovery,
    onSelect: (String) -> Unit,
) {
    val hfToken by appSettings.hfToken.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()
    // Recompute when any token changes
    val tokenEpoch = "${hfToken.orEmpty()}|${groqKey.orEmpty()}|${openRouterKey.orEmpty()}"

    var discovered by remember(capability) { mutableStateOf<List<CloudModelProvider>>(emptyList()) }
    var discovering by remember { mutableStateOf(false) }
    var discoverError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val usable = remember(tokenEpoch, capability) { discovery.curatedUsable(appSettings, capability) }
    val locked = remember(tokenEpoch, capability) { discovery.curatedLocked(appSettings, capability) }
    val options = remember(usable, discovered) {
        (usable + discovered).distinctBy { it.id }
    }

    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
        ?: locked.firstOrNull { it.id == selectedId }
        ?: options.firstOrNull()

    Spacer(Modifier.height(14.dp))
    GlassCard {
        GlassSectionLabel(title)
        Text(
            when {
                options.isNotEmpty() -> "${options.size} free models ready with your tokens"
                locked.isNotEmpty() -> "Add the required token above to unlock ${locked.size} models"
                else -> "No free models for this capability yet"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.displayName ?: "Select model",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                supportingText = {
                    Text(selected?.usageNote?.ifBlank { selected.description } ?: "")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                enabled = options.isNotEmpty(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(provider.displayName)
                                Text(
                                    "${provider.platform.name} · ${provider.license}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onSelect(provider.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (locked.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Locked until token saved: " + locked.take(3).joinToString { it.displayName },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = {
                discovering = true
                discoverError = null
                scope.launch {
                    discovered = discovery.discoverHf(appSettings.hfToken.value, capability)
                    appSettings.rememberDiscovered(discovered)
                    discovering = false
                    if (discovered.isEmpty() && !appSettings.hfToken.value.isNullOrBlank()) {
                        discoverError = "No warm HF Inference models found for this capability"
                    } else if (appSettings.hfToken.value.isNullOrBlank()) {
                        discoverError = "Save an HF token above, then refresh"
                    }
                }
            },
            enabled = !discovering,
        ) {
            Text(if (discovering) "Refreshing HF free models…" else "Refresh free HF models from token")
        }
        discoverError?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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

private fun EngineTier.label(): String = when (this) {
    EngineTier.AUTO -> "Auto (on-device)"
    EngineTier.LITE -> "Lite — on device"
    EngineTier.PRO -> "Pro — on device"
    EngineTier.CLOUD -> "Cloud — free HF Spaces"
}

private fun EngineTier.description(availability: Availability): String {
    val base = when (this) {
        EngineTier.AUTO -> "Best on-device engine. Never uses cloud automatically."
        EngineTier.LITE -> "Fast compositor. Works offline on every phone."
        EngineTier.PRO -> "SD1.5 diffusion on-device. Needs Pro pack."
        EngineTier.CLOUD -> "Free Hugging Face Spaces only. Select model below."
    }
    return when (availability) {
        Availability.Ready -> base
        is Availability.Unavailable -> when (availability.reason) {
            UnavailableReason.PACK_NOT_INSTALLED -> "$base Model pack not installed."
            UnavailableReason.DEVICE_NOT_CAPABLE -> "$base Device doesn’t meet RAM requirements."
            UnavailableReason.OFFLINE -> "$base No internet connection."
            UnavailableReason.NOT_CONFIGURED -> "$base Add the required free API key above."
        }
    }
}
