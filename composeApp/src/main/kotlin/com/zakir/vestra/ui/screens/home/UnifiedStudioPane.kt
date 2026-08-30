package com.zakir.vestra.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.safety.SafetyPresets
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.DockedLiveLog
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassOptionToggle
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.ModelQuickSwitcher
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.ResultPane
import com.zakir.vestra.ui.components.SafetyConfirmDialog
import com.zakir.vestra.ui.components.StudioTurnBubble
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a pack-readiness probe off the UI thread.
 *
 * The probes stat files on disk, so calling them directly from a composable body put
 * file-system work on the main thread on every recomposition. [keys] re-runs the probe when the
 * installed packs change or a generation starts/finishes, which is the only time the answer can
 * actually differ.
 */
@Composable
private fun <T> produceLocalProbe(
    vararg keys: Any?,
    initial: T,
    probe: () -> T,
): State<T> = produceState(initialValue = initial, keys = keys) {
    value = withContext(Dispatchers.IO) { runCatching(probe).getOrDefault(initial) }
}

@Composable
private fun produceLocalReadiness(
    vararg keys: Any?,
    probe: () -> Boolean,
): State<Boolean> = produceLocalProbe(*keys, initial = false, probe = probe)

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
    val turns by viewModel.turns.collectAsState()
    val liveLog by viewModel.liveLog.collectAsState()
    val generationStartedAtMs by viewModel.generationStartedAtMs.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val creative by viewModel.creativeMode.collectAsState()
    val pragmatic by viewModel.pragmaticMode.collectAsState()
    val fashionContext by viewModel.fashionContext.collectAsState()
    val inferenceSteps by viewModel.inferenceSteps.collectAsState()
    val guidanceScale by viewModel.guidanceScale.collectAsState()
    val seed by viewModel.seed.collectAsState()
    val strength by viewModel.strength.collectAsState()
    val candidateCount by viewModel.candidateCount.collectAsState()
    val packStates by packManager?.states?.collectAsState()
        ?: remember { mutableStateOf(emptyMap()) }

    val warmup by viewModel.warmup.collectAsState()
    val cloudModelsEnabled by viewModel.appSettings.cloudModelsEnabled.collectAsState()
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
    val preflightChip = viewModel.preflightLabel(effectiveCapability)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing

    // These each stat pack files on disk. Called straight from the composable body they ran on
    // the main thread on every recomposition — and ResultPane ticks once a second while a
    // generation is running, so that was five file-system probes per second on the UI thread.
    // Hoist them onto Dispatchers.IO and recompute only when the installed packs change.
    val localImageReady by produceLocalReadiness(packStates, busy) { viewModel.localImageOfflineReady() }
    val localImageEditReady by produceLocalReadiness(packStates, busy) { viewModel.localImageEditOfflineReady() }
    val localCodeReady by produceLocalReadiness(packStates, busy) { viewModel.localCodeOfflineReady() }
    val localVideoReady by produceLocalReadiness(packStates, busy) { viewModel.localVideoOfflineReady() }

    // Mirrors ImageParameterSection's own showGuidance/isEdit gating exactly — otherwise the
    // "Advanced" badge counts an override on a slider the current mode doesn't even display
    // (e.g. edit strength left non-default after removing the reference photo), with no way
    // to see or clear whatever the badge is counting.
    val imageEditActive = reference != null
    val imageShowsGuidance = imageGenId != "local-bonsai-image-v1" || imageEditActive
    val assistCount = when (capability) {
        AiCapability.CODE -> listOf(pragmatic, creative).count { it }
        AiCapability.AUDIO -> listOf(fashionContext).count { it }
        AiCapability.IMAGE_GEN -> listOfNotNull(
            inferenceSteps != 22,
            (guidanceScale != 7.0f).takeIf { imageShowsGuidance },
            seed != null,
            (strength != 0.65f).takeIf { imageEditActive },
            candidateCount > 1,
        ).count { it }
        else -> 0
    }

    // The Candidates control only renders while a local pack is ready (batching isn't reachable
    // for cloud through this UI). If the local pack becomes unavailable after the user set a
    // count > 1 — uninstalled, or "prefer local" turned off — a stale count would otherwise keep
    // multiplying real cloud API calls on the next generation with no visible control left to
    // explain or reset it.
    LaunchedEffect(localImageReady, localImageEditReady) {
        if (!localImageReady && !localImageEditReady) {
            viewModel.setCandidateCount(1)
        }
    }

    var showModelPicker by remember { mutableStateOf(false) }
    // Separate from showModelPicker (the full sheet, still used for search/error-recovery flows
    // that need the sheet's richer metadata — license, quota status, "needs API key") — the
    // composer's model chip opens this compact popup instead, so picking among a handful of
    // already-known-usable models doesn't require leaving the composer.
    var showQuickSwitcher by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var showSafetyConfirm by remember { mutableStateOf(false) }
    val safetyPresetId by viewModel.appSettings.safetyPresetId.collectAsState()
    // Cloud rows must disappear entirely when the master toggle is off — otherwise the picker
    // offers models that preflight and the runtime gate will refuse to run.
    val pickerModels = remember(effectiveCapability, freeCloudDiscovery, cloudModelsEnabled) {
        if (!cloudModelsEnabled) {
            emptyList()
        } else {
            freeCloudDiscovery?.selectable(viewModel.appSettings, effectiveCapability)
                ?: CloudModelCatalog.forCapability(effectiveCapability)
        }
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
                "local-gemma-v1" -> packStates["local-gemma-v1"]?.isReady() == true
                "local-gemma-4-e2b-v1" -> packStates["local-gemma-4-e2b-v1"]?.isReady() == true
                "local-functiongemma-v1" -> packStates["local-functiongemma-v1"]?.isReady() == true
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

    // Picking a model should load it, not defer the cost to the first prompt.
    LaunchedEffect(selectedId, effectiveCapability) {
        viewModel.warmUpLocal(effectiveCapability)
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setReference(uri?.toString())
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

    fun dispatchGenerate() {
        when (capability) {
            AiCapability.IMAGE_GEN -> viewModel.generateImage()
            AiCapability.VIDEO -> viewModel.generateVideo()
            AiCapability.CODE -> viewModel.generateCode()
            AiCapability.AUDIO -> viewModel.generateAudio()
            else -> Unit
        }
    }

    // The active safety preset's confirm flag (Blur identities / Redact details) requires a
    // real confirmation step before generation runs — Image (covers Image Edit too, same
    // generateImage() call) and Video, the two capabilities GenerativeViewModel applies the
    // preset's guard clause to. Audio/Code don't produce visual content, so the guard doesn't
    // apply there.
    fun onGenerate() {
        val requiresConfirm = capability in setOf(AiCapability.IMAGE_GEN, AiCapability.VIDEO) &&
            SafetyPresets.byId(safetyPresetId).confirm
        if (requiresConfirm) {
            showSafetyConfirm = true
        } else {
            dispatchGenerate()
        }
    }

    val accent = VestraColors.modalityAccent(effectiveCapability)
    val failedMsg = (state as? GenerativeState.Failed)?.message.orEmpty()
    val quotaOrCredits = failedMsg.contains("ZeroGPU", ignoreCase = true) ||
        failedMsg.contains("monthly credits", ignoreCase = true) ||
        failedMsg.contains("Inference Providers", ignoreCase = true)
    val listState = rememberLazyListState()
    // Auto-scroll to the newest turn as it's appended or its result comes in — the header item
    // (index 0) means the last turn always sits at `turns.lastIndex + 1`.
    LaunchedEffect(turns) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex + 1)
    }

    // Two regions, not one long scroll: the conversation timeline scrolls in the top region
    // while the composer stays docked at the bottom of the screen. Previously everything lived
    // in a single verticalScroll column, so the composer drifted mid-scroll and results pushed
    // it off-screen; now a single LazyColumn (not a verticalScroll Column) handles the header
    // content and the turn history together, since nesting a scrollable list inside an already-
    // scrollable Column is a measurement bug waiting to happen (unbounded height).
    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.section, vertical = 8.dp),
        ) {
            item(key = "studio-header") {
                Column {
                    // Model/readiness status used to duplicate here (a section label, a status
                    // sentence, and a FlowRow with a cloud-fallback estimate that could show the
                    // wrong model's name entirely — see 3.1.6 CHANGELOG) on top of the composer's
                    // own model chip below. That's gone; the chip is the single source of truth
                    // for which model is selected/loading/ready. Only a real failure still gets
                    // its own banner here, since the send button alone can't carry a retry action.
                    when (val w = warmup) {
                        is GenerativeViewModel.Warmup.Failed -> {
                            GlassErrorBanner(
                                message = "${w.label} could not load: ${w.reason}",
                                onRetry = { viewModel.warmUpLocal(effectiveCapability) },
                                retryLabel = "Retry load",
                                onDismiss = null,
                            )
                        }
                        // Loading/Ready surface only via the composer's send-button spinner and
                        // model chip.
                        GenerativeViewModel.Warmup.Idle,
                        is GenerativeViewModel.Warmup.Loading,
                        is GenerativeViewModel.Warmup.Ready,
                        -> Unit
                    }

                    // Editorial/fashion/detail/quality/vision-assist toggles and the in-studio
                    // Safety row were deleted here (see 3.1.6 CHANGELOG) — Code's Pragmatic/
                    // Creative toggles are the only ones left with a studio-side UI, so this only
                    // renders for Code; an empty "Advanced" card that expands to nothing for
                    // every other capability would be its own dangling-affordance bug.
                    if (capability == AiCapability.CODE) {
                        Spacer(Modifier.height(8.dp))
                        AdvancedAssistSection(
                            expanded = advancedExpanded,
                            onToggle = { advancedExpanded = !advancedExpanded },
                            busy = busy,
                            pragmatic = pragmatic,
                            creative = creative,
                            onPragmatic = { viewModel.setPragmaticMode(!pragmatic) },
                            onCreative = { viewModel.setCreativeMode(!creative) },
                        )
                    } else if (capability == AiCapability.IMAGE_GEN &&
                        (localImageReady || localImageEditReady)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        ImageParameterSection(
                            expanded = advancedExpanded,
                            onToggle = { advancedExpanded = !advancedExpanded },
                            busy = busy,
                            isEdit = imageEditActive,
                            showGuidance = imageShowsGuidance,
                            steps = inferenceSteps,
                            guidance = guidanceScale,
                            seed = seed,
                            strength = strength,
                            candidateCount = candidateCount,
                            onSteps = viewModel::setInferenceSteps,
                            onGuidance = viewModel::setGuidanceScale,
                            onSeed = viewModel::setSeed,
                            onStrength = viewModel::setStrength,
                            onCandidateCount = viewModel::setCandidateCount,
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
                }
            }
            itemsIndexed(turns, key = { _, turn -> turn.id }) { index, turn ->
                StudioTurnBubble(
                    turn = turn,
                    index = index,
                    isLatest = index == turns.lastIndex,
                    accent = accent,
                    generationStartedAtMs = generationStartedAtMs,
                    onRetry = {
                        viewModel.clearResult()
                        if (quotaOrCredits) {
                            showModelPicker = true
                        } else {
                            onGenerate()
                        }
                    },
                    onDismiss = {
                        viewModel.clearResult()
                        viewModel.dismissLastTurn()
                    },
                    retryLabel = if (quotaOrCredits) "Choose model" else LookbookCopy.ACTION_RETRY,
                    modifier = Modifier
                        .animateItem()
                        .padding(vertical = 6.dp),
                )
            }
        }

        // Docked composer — outside the scroll region so prompt, model pill, reference
        // picker and send stay reachable no matter how long the conversation gets.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.section)
                .padding(bottom = 10.dp, top = 4.dp),
        ) {
            // CODE is the one Studio capability that's actually LLM-context-window-shaped
            // (Image/Video/Audio are diffusion/TTS, not chat-context-bounded) — A4.3.
            if (capability == AiCapability.CODE) {
                // The effective model id (which local pack, if any) requires resolving
                // RoutingLocalCodeGenerator's delegate, which stats pack files on disk when no
                // explicit pick exists — hoisted to Dispatchers.IO and keyed on the same
                // pack/readiness signals as produceLocalReadiness above it, NOT on `prompt`, so
                // typing doesn't re-trigger disk I/O on every keystroke.
                val codeModelId by produceState(
                    initialValue = codeId,
                    keys = arrayOf(packStates, localCodeReady, codeId),
                ) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { viewModel.currentCodeModelId(localCodeReady) }.getOrDefault(codeId)
                    }
                }
                val codeBudget = remember(prompt, codeModelId) {
                    com.zakir.vestra.shared.chat.ContextBudget.evaluate(
                        usedTokens = com.zakir.vestra.shared.chat.ContextBudget.estimateTokens(prompt),
                        modelId = codeModelId,
                    )
                }
                com.zakir.vestra.ui.screens.news.ContextBudgetBar(
                    budget = codeBudget,
                    hasDraft = prompt.isNotBlank(),
                    testTag = com.zakir.vestra.ui.TestTags.STUDIO_TOKEN_BUDGET_BAR,
                )
            }
            // Docked next to the composer instead of scrolling past with the results, so the
            // live log stays visible ("always visible like a in the docked prompt box") without
            // eating the space above it.
            DockedLiveLog(
                lines = liveLog,
                generationStartedAtMs = generationStartedAtMs,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Box {
            PromptComposer(
                prompt = prompt,
                onPromptChange = viewModel::setPrompt,
                accent = accent,
                modelLabel = when {
                    // Reflects whichever engine the user actually selected — was hardcoded to
                    // "Local tiny-SD" regardless of pick, mislabeling every Bonsai-selected run.
                    effectiveCapability == AiCapability.IMAGE_GEN && localImageReady && reference == null ->
                        if (imageGenId == "local-bonsai-image-v1") {
                            "Bonsai Image 4B (offline)"
                        } else {
                            "Local tiny-SD (offline)"
                        }
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
                loading = warmup is GenerativeViewModel.Warmup.Loading,
                enabled = true,
                onModelClick = { showQuickSwitcher = true },
                // Advanced renders for Code (Pragmatic/Creative) and for Image when a local
                // model is selected (sampler controls) — wiring this unconditionally left the
                // Assists chip a clickable no-op on every other capability, toggling a flag
                // nothing was listening to anymore.
                onAssistsClick = if (capability == AiCapability.CODE ||
                    (capability == AiCapability.IMAGE_GEN && (localImageReady || localImageEditReady))
                ) {
                    { advancedExpanded = !advancedExpanded }
                } else {
                    null
                },
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
            // Anchored to a marker at the composer's bottom-start corner (where the model chip
            // actually renders — PromptComposer.kt's last Row) rather than the whole multi-row
            // Box above: the composer is docked at the screen's bottom edge with nothing below
            // it, so anchoring to the Box's top would force Compose's flip-to-fit to jump the
            // popup above the entire composer (quick-prompt carousel, reference row, text
            // field) instead of appearing near the tapped chip.
            Box(Modifier.align(Alignment.BottomStart)) {
                ModelQuickSwitcher(
                    expanded = showQuickSwitcher,
                    onDismiss = { showQuickSwitcher = false },
                    models = pickerModels,
                    selectedId = selectedId,
                    onSelect = { chosen ->
                        when (effectiveCapability) {
                            AiCapability.IMAGE_EDIT -> viewModel.appSettings.setImageEditProvider(chosen.id)
                            AiCapability.IMAGE_GEN -> viewModel.appSettings.setImageGenProvider(chosen.id)
                            AiCapability.VIDEO -> viewModel.appSettings.setVideoProvider(chosen.id)
                            AiCapability.CODE -> viewModel.appSettings.setCodeProvider(chosen.id)
                            else -> Unit
                        }
                    },
                    onDeviceEntries = onDeviceEntries,
                    onSelectDevice = { entry ->
                        if (!entry.ready) return@ModelQuickSwitcher
                        when (effectiveCapability) {
                            AiCapability.IMAGE_EDIT,
                            AiCapability.IMAGE_GEN,
                            AiCapability.VIDEO,
                            AiCapability.CODE,
                            -> viewModel.appSettings.setLocalGenerator(effectiveCapability, entry.id)
                            else -> Unit
                        }
                    },
                    onBrowseAll = { showModelPicker = true },
                    health = viewModel.appSettings.modelHealth,
                    accent = accent,
                    cloudGenerationEnabled = cloudModelsEnabled,
                    hasCredential = { model ->
                        !model.requiresApiKey || !viewModel.appSettings.apiKeyFor(model).isNullOrBlank()
                    },
                )
            }
        }
        }
    }

    if (showModelPicker) {
        ModelPickerSheet(
            title = when (effectiveCapability) {
                AiCapability.IMAGE_EDIT -> "Image edit models"
                AiCapability.IMAGE_GEN -> "Image models"
                AiCapability.VIDEO -> "Video models"
                AiCapability.CODE -> "Coding models"
                else -> "Models"
            } + if (cloudModelsEnabled) "" else " · on-device",
            models = pickerModels,
            selectedId = selectedId,
            onDeviceEntries = onDeviceEntries,
            health = viewModel.appSettings.modelHealth,
            accent = accent,
            cloudGenerationEnabled = cloudModelsEnabled,
            hasCredential = { model ->
                !model.requiresApiKey || !viewModel.appSettings.apiKeyFor(model).isNullOrBlank()
            },
            onSelect = { chosen ->
                when (effectiveCapability) {
                    AiCapability.IMAGE_EDIT -> viewModel.appSettings.setImageEditProvider(chosen.id)
                    AiCapability.IMAGE_GEN -> viewModel.appSettings.setImageGenProvider(chosen.id)
                    AiCapability.VIDEO -> viewModel.appSettings.setVideoProvider(chosen.id)
                    AiCapability.CODE -> viewModel.appSettings.setCodeProvider(chosen.id)
                    else -> Unit
                }
            },
            onSelectDevice = { entry ->
                if (!entry.ready) return@ModelPickerSheet
                when (effectiveCapability) {
                    AiCapability.IMAGE_EDIT,
                    AiCapability.IMAGE_GEN,
                    AiCapability.VIDEO,
                    AiCapability.CODE,
                    -> viewModel.appSettings.setLocalGenerator(effectiveCapability, entry.id)
                    else -> Unit
                }
            },
            onDismiss = { showModelPicker = false },
        )
    }

    if (showSafetyConfirm) {
        SafetyConfirmDialog(
            preset = SafetyPresets.byId(safetyPresetId),
            onConfirm = {
                showSafetyConfirm = false
                dispatchGenerate()
            },
            onCancel = { showSafetyConfirm = false },
        )
    }
}

@Composable
// Code's Pragmatic/Creative toggles are the only Advanced controls left with a studio-side UI —
// Editorial/Modest fashion/Detail enhance/Quality check/Analyze reference and the in-studio
// Safety row were deleted here (see 3.1.6 CHANGELOG). Safety is still configurable from Settings
// (SettingsSafetySection.kt, which already duplicated this row's exact behavior); Analyze
// reference moved to a persisted AppSettings toggle next to it, since — unlike the others, which
// all default to on — it defaulted off and had no other access point once its studio UI was gone.
private fun AdvancedAssistSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    busy: Boolean,
    pragmatic: Boolean,
    creative: Boolean,
    onPragmatic: () -> Unit,
    onCreative: () -> Unit,
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
        }
    }
}

/**
 * Local-generation sampler controls (steps / guidance / seed / img2img strength / candidate
 * count) — the engines already accept these per call, but nothing surfaced them (see the audit
 * that added this section). Only meaningful for on-device generation: cloud Spaces/HF Inference
 * payloads don't accept sampler overrides (see [ModelAssistCatalog]'s own note), so this only
 * renders while a local model is selected.
 */
@Composable
private fun ImageParameterSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    busy: Boolean,
    isEdit: Boolean,
    showGuidance: Boolean,
    steps: Int,
    guidance: Float,
    seed: Long?,
    strength: Float,
    candidateCount: Int,
    onSteps: (Int) -> Unit,
    onGuidance: (Float) -> Unit,
    onSeed: (Long?) -> Unit,
    onStrength: (Float) -> Unit,
    onCandidateCount: (Int) -> Unit,
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
                ParamSlider(
                    label = "Steps",
                    valueLabel = steps.toString(),
                    value = steps.toFloat(),
                    range = 4f..50f,
                    enabled = !busy,
                    onChange = { onSteps(it.toInt()) },
                )
                Text(
                    "Distilled local packs round this to their own supported step count " +
                        "(typically 4–8) — this sets an upper bound, not an exact count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                )
                if (showGuidance) {
                    Spacer(Modifier.height(10.dp))
                    ParamSlider(
                        label = "Guidance",
                        valueLabel = "%.1f".format(guidance),
                        value = guidance,
                        range = 1f..15f,
                        enabled = !busy,
                        onChange = onGuidance,
                    )
                }
                if (isEdit) {
                    Spacer(Modifier.height(10.dp))
                    ParamSlider(
                        label = "Edit strength",
                        valueLabel = "%.2f".format(strength),
                        value = strength,
                        range = 0.15f..0.95f,
                        enabled = !busy,
                        onChange = onStrength,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Seed" + if (seed != null) ": $seed" else ": Random",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VestraColors.Ink,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassPill(
                            text = "Reroll",
                            active = false,
                            modifier = Modifier.clickable(enabled = !busy) {
                                onSeed(kotlin.random.Random.nextLong(0L, Long.MAX_VALUE))
                            },
                        )
                        if (seed != null) {
                            GlassPill(
                                text = "Random",
                                active = false,
                                modifier = Modifier.clickable(enabled = !busy) { onSeed(null) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Candidates",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VestraColors.Ink,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (count in 1..4) {
                        GlassPill(
                            text = "${count}×",
                            active = candidateCount == count,
                            modifier = Modifier.clickable(enabled = !busy) { onCandidateCount(count) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VestraColors.Ink)
        Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = VestraColors.InkMuted)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = VestraColors.Accent,
            activeTrackColor = VestraColors.Accent,
        ),
    )
}
