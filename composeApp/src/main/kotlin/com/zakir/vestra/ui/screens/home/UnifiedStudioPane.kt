package com.zakir.vestra.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.ExamplePromptRow
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassOptionToggle
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.ResultPane
import com.zakir.vestra.ui.theme.VestraColors

@Composable
fun UnifiedStudioPane(
    capability: AiCapability,
    viewModel: GenerativeViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
    freeCloudDiscovery: FreeCloudDiscovery? = null,
    packManager: ModelPackManager? = null,
) {
    LaunchedEffect(capability) {
        viewModel.bindStudio(capability)
    }

    val prompt by viewModel.prompt.collectAsState()
    val reference by viewModel.referenceUri.collectAsState()
    val state by viewModel.state.collectAsState()
    val liveLog by viewModel.liveLog.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val creative by viewModel.creativeMode.collectAsState()
    val pragmatic by viewModel.pragmaticMode.collectAsState()
    val detailBoost by viewModel.detailBoost.collectAsState()
    val fashionContext by viewModel.fashionContext.collectAsState()
    val bypassFilter by viewModel.bypassFilter.collectAsState()
    val qualityGuard by viewModel.qualityGuard.collectAsState()
    val inferenceSteps by viewModel.inferenceSteps.collectAsState()
    val guidanceScale by viewModel.guidanceScale.collectAsState()
    val seed by viewModel.seed.collectAsState()
    val lastUsedId by viewModel.lastUsedProviderId.collectAsState()
    val packStates by packManager?.states?.collectAsState()
        ?: remember { mutableStateOf(emptyMap()) }

    val imageGenId by viewModel.appSettings.imageGenProviderId.collectAsState()
    val imageEditId by viewModel.appSettings.imageEditProviderId.collectAsState()
    val codeId by viewModel.appSettings.codeProviderId.collectAsState()
    val videoId by viewModel.appSettings.videoProviderId.collectAsState()

    val effectiveCapability = when (capability) {
        AiCapability.IMAGE_GEN -> if (reference == null) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT
        else -> capability
    }
    val provider = viewModel.appSettings.selectedProvider(effectiveCapability)
    val selectedId = when (effectiveCapability) {
        AiCapability.IMAGE_GEN -> imageGenId
        AiCapability.IMAGE_EDIT -> imageEditId
        AiCapability.CODE -> codeId
        AiCapability.VIDEO -> videoId
        AiCapability.AUDIO -> provider.id
        else -> provider.id
    }
    val estimate = viewModel.usage.estimateNext(provider)
    val preflightChip = viewModel.preflightLabel(effectiveCapability)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing
    val localImageReady = viewModel.localImageOfflineReady()
    val localImageEditReady = viewModel.localImageEditOfflineReady()
    val localCodeReady = viewModel.localCodeOfflineReady()
    val localVideoReady = viewModel.localVideoOfflineReady()

    val assistCount = when (capability) {
        AiCapability.CODE -> listOf(pragmatic, creative).count { it }
        else -> listOf(bypassFilter, fashionContext, detailBoost, qualityGuard).count { it }
    }

    var showModelPicker by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    val pickerModels = remember(effectiveCapability, freeCloudDiscovery) {
        freeCloudDiscovery?.selectable(viewModel.appSettings, effectiveCapability)
            ?: CloudModelCatalog.forCapability(effectiveCapability)
    }
    val onDeviceEntries = remember(
        packStates,
        effectiveCapability,
        localImageReady,
        localImageEditReady,
        localCodeReady,
        localVideoReady,
    ) {
        LocalModelCatalog.forStudioPicker(effectiveCapability).map { entry ->
            val packReady = when (entry.id) {
                "local-sdturbo-v1" -> localImageReady
                "local-sdturbo-edit" -> localImageEditReady
                "local-stillclip-v1" -> localVideoReady
                "local-gemma-v1" -> localCodeReady
                else -> entry.packId?.let { packStates[it]?.isReady() == true } == true
            }
            OnDevicePickerEntry(
                id = entry.id,
                displayName = entry.displayName,
                detail = entry.testingNote,
                ready = LocalModelCatalog.studioEntryReady(entry, packReady),
                statusLabel = LocalModelCatalog.studioStatusLabel(entry, packReady),
            )
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setReference(uri?.toString())
    }

    val subtitle = when (capability) {
        AiCapability.IMAGE_GEN -> LookbookCopy.STUDIO_IMAGE
        AiCapability.VIDEO -> LookbookCopy.STUDIO_VIDEO
        AiCapability.CODE -> LookbookCopy.STUDIO_CODE
        else -> "Studio"
    }

    val placeholder = when (capability) {
        AiCapability.IMAGE_GEN -> if (reference == null) {
            "Describe the image… emerald abaya in a Lahore bazaar"
        } else {
            "Describe the edit… change to navy silk, soft studio light"
        }
        AiCapability.VIDEO -> "Describe the clip… abaya walking through a Karachi night bazaar"
        AiCapability.CODE -> "Ask for code… Kotlin Compose glass card with frosted border"
        else -> "Enter a prompt…"
    }

    val examples = when (capability) {
        AiCapability.IMAGE_GEN -> listOf(
            "Emerald abaya in a Lahore bazaar, soft afternoon light",
            "Navy silk hijab portrait, studio softbox, editorial",
            "Cream linen shalwar kameez, courtyard architecture",
        )
        AiCapability.VIDEO -> listOf(
            "Woman in black abaya walking through a Karachi night bazaar",
            "Slow pan across embroidered green shalwar kameez in soft daylight",
            "Hijabi model turning toward camera, linen texture detail",
        )
        AiCapability.CODE -> listOf(
            "Write a Kotlin Compose frosted glass card with border highlight",
            "Explain how to resume an Android OkHttp download with Range headers",
            "Refactor this into a StateFlow ViewModel pattern (paste code)",
        )
        else -> emptyList()
    }

    fun onGenerate() = when (capability) {
        AiCapability.IMAGE_GEN -> viewModel.generateImage()
        AiCapability.VIDEO -> viewModel.generateVideo()
        AiCapability.CODE -> viewModel.generateCode()
        AiCapability.AUDIO -> viewModel.generateAudio()
        else -> Unit
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        GlassSectionLabel(subtitle.uppercase())
        Text(
            when (capability) {
                AiCapability.IMAGE_GEN, AiCapability.IMAGE_EDIT ->
                    when {
                        reference != null && localImageEditReady ->
                            "Local img2img ready offline — Edit runs on-device."
                        reference == null && localImageReady ->
                            "Local tiny-SD ready offline — Create Studio runs on-device."
                        else ->
                            "Cloud by default. For offline Create/Edit: Settings → Model packs → download local-sdturbo-v1 (~1.06 GB)."
                    }
                AiCapability.VIDEO ->
                    if (localVideoReady) {
                        "Local still-clip ready — short on-device MP4 from tiny-SD (not diffusion video)."
                    } else {
                        "Cloud HF Spaces by default. Offline still-clips: download local-sdturbo-v1."
                    }
                AiCapability.AUDIO ->
                    "Device TTS works offline + voice-changer knobs. Cloud TTS optional."
                AiCapability.CODE ->
                    if (localCodeReady) {
                        "Local Gemma ready offline — Code Studio runs on-device."
                    } else {
                        "Cloud LLMs by default. Offline Code: download local-gemma-v1 (~530 MB)."
                    }
                else -> estimate
            },
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.InkMuted,
        )
        Text(
            estimate,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
        )
        if (preflightChip != null && preflight == null) {
            Spacer(Modifier.height(6.dp))
            GlassPill(text = preflightChip, active = true)
        }
        lastUsedId?.let { id ->
            CloudModelCatalog.byId(id)?.let { used ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Last run: ${used.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.Accent,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        PromptComposer(
            prompt = prompt,
            onPromptChange = viewModel::setPrompt,
            modelLabel = when {
                effectiveCapability == AiCapability.IMAGE_GEN && localImageReady && reference == null ->
                    "Local tiny-SD (offline)"
                effectiveCapability == AiCapability.IMAGE_EDIT && localImageEditReady ->
                    "Local img2img (offline)"
                effectiveCapability == AiCapability.CODE && localCodeReady ->
                    "Local Gemma (offline)"
                effectiveCapability == AiCapability.VIDEO && localVideoReady ->
                    "Local still-clip (offline)"
                else -> provider.displayName
            },
            assistCount = assistCount,
            busy = busy,
            enabled = true,
            onModelClick = { showModelPicker = true },
            onAssistsClick = { advancedExpanded = !advancedExpanded },
            onSend = ::onGenerate,
            onStop = { viewModel.forceStop() },
            placeholder = placeholder,
            referenceUri = if (capability == AiCapability.IMAGE_GEN) reference else null,
            onAddReference = if (capability == AiCapability.IMAGE_GEN) {
                {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            } else {
                null
            },
            onClearReference = if (capability == AiCapability.IMAGE_GEN) {
                { viewModel.setReference(null) }
            } else {
                null
            },
        )

        Spacer(Modifier.height(8.dp))
        AdvancedAssistSection(
            expanded = advancedExpanded,
            onToggle = { advancedExpanded = !advancedExpanded },
            busy = busy,
            capability = capability,
            bypassFilter = bypassFilter,
            fashionContext = fashionContext,
            detailBoost = detailBoost,
            qualityGuard = qualityGuard,
            pragmatic = pragmatic,
            creative = creative,
            onBypassFilter = { viewModel.setBypassFilter(!bypassFilter) },
            onFashionContext = { viewModel.setFashionContext(!fashionContext) },
            onDetailBoost = { viewModel.setDetailBoost(!detailBoost) },
            onQualityGuard = { viewModel.setQualityGuard(!qualityGuard) },
            onPragmatic = { viewModel.setPragmaticMode(!pragmatic) },
            onCreative = { viewModel.setCreativeMode(!creative) },
            inferenceSteps = inferenceSteps,
            guidanceScale = guidanceScale,
            seed = seed?.toString().orEmpty(),
            onInferenceSteps = viewModel::setInferenceSteps,
            onGuidanceScale = viewModel::setGuidanceScale,
            onSeed = { raw ->
                viewModel.setSeed(raw.trim().toLongOrNull())
            },
        )

        if (examples.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "EXAMPLES",
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.Accent,
            )
            Spacer(Modifier.height(6.dp))
            ExamplePromptRow(
                examples = examples,
                enabled = !busy,
                onPick = viewModel::setPrompt,
            )
        }

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
        if (viewModel.resultBelongsTo(effectiveCapability) || viewModel.resultBelongsTo(capability)) {
            val failedMsg = (state as? GenerativeState.Failed)?.message.orEmpty()
            val quotaOrCredits = failedMsg.contains("ZeroGPU", ignoreCase = true) ||
                failedMsg.contains("monthly credits", ignoreCase = true) ||
                failedMsg.contains("Inference Providers", ignoreCase = true)
            ResultPane(
                state = state,
                liveLog = liveLog,
                onCancel = { viewModel.forceStop() },
                onRetry = {
                    viewModel.clearResult()
                    if (quotaOrCredits) {
                        showModelPicker = true
                    } else {
                        onGenerate()
                    }
                },
                retryLabel = if (quotaOrCredits) "Choose model" else LookbookCopy.ACTION_RETRY,
                onDismiss = viewModel::clearResult,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showModelPicker) {
        ModelPickerSheet(
            title = when (effectiveCapability) {
                AiCapability.IMAGE_EDIT -> "Image edit models"
                AiCapability.IMAGE_GEN -> "Image models"
                AiCapability.VIDEO -> "Video models"
                AiCapability.CODE -> "Coding models"
                else -> "Models"
            },
            models = pickerModels,
            selectedId = selectedId,
            onDeviceEntries = onDeviceEntries,
            health = viewModel.appSettings.modelHealth,
            onSelect = { chosen ->
                when (effectiveCapability) {
                    AiCapability.IMAGE_EDIT -> viewModel.appSettings.setImageEditProvider(chosen.id)
                    AiCapability.IMAGE_GEN -> viewModel.appSettings.setImageGenProvider(chosen.id)
                    AiCapability.VIDEO -> viewModel.appSettings.setVideoProvider(chosen.id)
                    AiCapability.CODE -> viewModel.appSettings.setCodeProvider(chosen.id)
                    else -> Unit
                }
            },
            onDismiss = { showModelPicker = false },
        )
    }
}

@Composable
private fun AdvancedAssistSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    busy: Boolean,
    capability: AiCapability,
    bypassFilter: Boolean,
    fashionContext: Boolean,
    detailBoost: Boolean,
    qualityGuard: Boolean,
    pragmatic: Boolean,
    creative: Boolean,
    onBypassFilter: () -> Unit,
    onFashionContext: () -> Unit,
    onDetailBoost: () -> Unit,
    onQualityGuard: () -> Unit,
    onPragmatic: () -> Unit,
    onCreative: () -> Unit,
    inferenceSteps: Int = 22,
    guidanceScale: Float = 7f,
    seed: String = "",
    onInferenceSteps: (Int) -> Unit = {},
    onGuidanceScale: (Float) -> Unit = {},
    onSeed: (String) -> Unit = {},
) {
    GlassCard(onClick = onToggle) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Advanced",
                style = MaterialTheme.typography.titleMedium,
                color = VestraColors.Ink,
            )
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse advanced options" else "Expand advanced options",
                tint = VestraColors.Accent,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                when (capability) {
                    AiCapability.CODE -> {
                        GlassOptionToggle(
                            text = LookbookCopy.ASSIST_PRAGMATIC,
                            active = pragmatic,
                            enabled = !busy,
                            onToggle = onPragmatic,
                        )
                        Spacer(Modifier.height(8.dp))
                        GlassOptionToggle(
                            text = LookbookCopy.ASSIST_CREATIVE,
                            active = creative,
                            enabled = !busy,
                            onToggle = onCreative,
                        )
                    }
                    else -> {
                        GlassOptionToggle(
                            text = LookbookCopy.ASSIST_EDITORIAL,
                            active = bypassFilter,
                            enabled = !busy,
                            onToggle = onBypassFilter,
                        )
                        Spacer(Modifier.height(8.dp))
                        GlassOptionToggle(
                            text = LookbookCopy.ASSIST_FASHION,
                            active = fashionContext,
                            enabled = !busy,
                            onToggle = onFashionContext,
                        )
                        Spacer(Modifier.height(8.dp))
                        GlassOptionToggle(
                            text = LookbookCopy.ASSIST_DETAIL,
                            active = detailBoost,
                            enabled = !busy,
                            onToggle = onDetailBoost,
                        )
                        Spacer(Modifier.height(8.dp))
                        GlassOptionToggle(
                            text = LookbookCopy.ASSIST_QUALITY,
                            active = qualityGuard,
                            enabled = !busy,
                            onToggle = onQualityGuard,
                        )
                        if (capability == AiCapability.IMAGE_GEN || capability == AiCapability.IMAGE_EDIT) {
                            Spacer(Modifier.height(12.dp))
                            Text("Sampler (HF Inference when supported)", style = MaterialTheme.typography.labelSmall, color = VestraColors.Accent)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = inferenceSteps.toString(),
                                onValueChange = { onInferenceSteps(it.toIntOrNull() ?: inferenceSteps) },
                                label = { Text("Steps") },
                                singleLine = true,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VestraColors.Accent.copy(alpha = 0.55f),
                                    unfocusedBorderColor = VestraColors.GlassBorder,
                                ),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = guidanceScale.toString(),
                                onValueChange = { onGuidanceScale(it.toFloatOrNull() ?: guidanceScale) },
                                label = { Text("CFG scale") },
                                singleLine = true,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VestraColors.Accent.copy(alpha = 0.55f),
                                    unfocusedBorderColor = VestraColors.GlassBorder,
                                ),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = seed,
                                onValueChange = onSeed,
                                label = { Text("Seed (optional)") },
                                singleLine = true,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VestraColors.Accent.copy(alpha = 0.55f),
                                    unfocusedBorderColor = VestraColors.GlassBorder,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
