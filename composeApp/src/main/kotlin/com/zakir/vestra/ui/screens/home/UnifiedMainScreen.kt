package com.zakir.vestra.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.zakir.vestra.VestraApp
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.chat.ChatMessage
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.jobs.LocalJobStore
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.storage.ApiKeyDataStore
import com.zakir.vestra.ui.ChatViewModel
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.ApiUsageDashboardCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.InterruptedJobsBanner
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.components.StudioTurnBubble
import com.zakir.vestra.ui.screens.news.ChatMessageBubble
import com.zakir.vestra.ui.screens.news.ChatTypingIndicator
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.launch

/**
 * The whole app is one screen: a single scrolling conversation that mixes Chat replies with
 * Image/Video/Code/Audio results, per the approved Gemini-style redesign. There is no dashboard
 * to browse and no bottom dock — Library and Settings are reached from the two icons at top
 * right. A composer mode chip (Chat/Image/Video/Code/Audio) picks which generator the next
 * message routes to; everything it produces lands in the same thread.
 */
private enum class ComposerMode(val label: String, val capability: AiCapability?) {
    CHAT("Chat", null),
    IMAGE("Image", AiCapability.IMAGE_GEN),
    VIDEO("Video", AiCapability.VIDEO),
    CODE("Code", AiCapability.CODE),
    AUDIO("Audio", AiCapability.AUDIO);

    val accent: Color
        get() = capability?.let(VestraColors::modalityAccent) ?: VestraColors.Accent
}

private sealed interface ThreadEntry {
    val timestampMs: Long
    data class Chat(val message: ChatMessage) : ThreadEntry {
        override val timestampMs: Long get() = message.timestampMs
    }
    data class Generative(val turn: GenerativeViewModel.StudioTurn) : ThreadEntry {
        override val timestampMs: Long get() = turn.timestampMs
    }
}

@Composable
fun UnifiedMainScreen(
    generativeViewModel: GenerativeViewModel,
    chatViewModel: ChatViewModel,
    appSettings: AppSettings,
    localJobStore: LocalJobStore?,
    freeCloudDiscovery: FreeCloudDiscovery? = null,
    packManager: ModelPackManager? = null,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var mode by remember {
        mutableStateOf(
            ComposerMode.entries.firstOrNull { it.capability == generativeViewModel.currentStudio }
                ?: ComposerMode.CHAT,
        )
    }
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val allTurns by generativeViewModel.allTurns.collectAsState()
    val chatMessages by chatViewModel.messages.collectAsState()
    val chatBusy by chatViewModel.busy.collectAsState()
    val chatError by chatViewModel.error.collectAsState()
    val genState by generativeViewModel.state.collectAsState()
    val genPrompt by generativeViewModel.prompt.collectAsState()
    val genPreflight by generativeViewModel.preflightMessage.collectAsState()
    val genReference by generativeViewModel.referenceUri.collectAsState()
    val generationStartedAtMs by generativeViewModel.generationStartedAtMs.collectAsState()
    val safetyPresetId by appSettings.safetyPresetId.collectAsState()
    var showSafetyConfirm by remember { mutableStateOf<AiCapability?>(null) }
    val genBusy = genState is GenerativeState.Preparing ||
        genState is GenerativeState.Running ||
        genState is GenerativeState.CodeStreaming

    // Which provider (cloud or on-device) the active mode's next generation would use, and the
    // picker to change it — Chat routes through the Code provider (matching ChatViewModel), and
    // Image with a reference photo attached routes through the Edit provider, not Create.
    var showModelPicker by remember { mutableStateOf(false) }
    val pickerCapability = when {
        mode == ComposerMode.CHAT -> AiCapability.CODE
        mode == ComposerMode.IMAGE && genReference != null -> AiCapability.IMAGE_EDIT
        else -> mode.capability ?: AiCapability.CODE
    }
    val imageGenId by appSettings.imageGenProviderId.collectAsState()
    val imageEditId by appSettings.imageEditProviderId.collectAsState()
    val codeId by appSettings.codeProviderId.collectAsState()
    val videoId by appSettings.videoProviderId.collectAsState()
    val selectedModelId = when (pickerCapability) {
        AiCapability.IMAGE_GEN -> imageGenId
        AiCapability.IMAGE_EDIT -> imageEditId
        AiCapability.CODE -> codeId
        AiCapability.VIDEO -> videoId
        else -> appSettings.selectedProvider(pickerCapability).id
    }
    val packStates by packManager?.states?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }
    val localImageReady = generativeViewModel.localImageOfflineReady()
    val localImageEditReady = generativeViewModel.localImageEditOfflineReady()
    val localCodeReady = generativeViewModel.localCodeOfflineReady()
    val localVideoReady = generativeViewModel.localVideoOfflineReady()
    val pickerModels = remember(pickerCapability, freeCloudDiscovery) {
        freeCloudDiscovery?.selectable(appSettings, pickerCapability)
            ?: CloudModelCatalog.forCapability(pickerCapability)
    }
    val onDeviceEntries = remember(packStates, pickerCapability, localImageReady, localImageEditReady, localCodeReady, localVideoReady) {
        LocalModelCatalog.forStudioPicker(pickerCapability).map { entry ->
            val packReady = when (entry.id) {
                "local-sdturbo-v1" -> localImageReady
                "local-sdturbo-edit" -> localImageEditReady
                "local-stillclip-v1" -> localVideoReady
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
    fun selectModel(id: String) {
        when (pickerCapability) {
            AiCapability.IMAGE_EDIT -> appSettings.setImageEditProvider(id)
            AiCapability.IMAGE_GEN -> appSettings.setImageGenProvider(id)
            AiCapability.VIDEO -> appSettings.setVideoProvider(id)
            AiCapability.CODE -> appSettings.setCodeProvider(id)
            else -> Unit
        }
    }

    val entries = remember(chatMessages, allTurns) {
        (chatMessages.map { ThreadEntry.Chat(it) } + allTurns.map { ThreadEntry.Generative(it) })
            .sortedBy { it.timestampMs }
    }
    // Per-capability, not a single global "latest" — a generation keeps running in a
    // backgrounded studio after the user switches modes (GenerativeViewModel's StudioBag design
    // never cancels it), so its own most-recent turn must stay retry/dismiss-able even after a
    // newer turn lands in a different studio.
    val latestIdByCapability = remember(allTurns) {
        allTurns.groupBy { it.capability }.mapValues { (_, turns) -> turns.maxBy { it.timestampMs }.id }
    }
    val showChatTyping = chatBusy &&
        chatMessages.lastOrNull()?.role?.equals("assistant", ignoreCase = true) != true

    LaunchedEffect(entries.size, showChatTyping) {
        val lastIndex = entries.size - 1 + if (showChatTyping) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    fun dispatchGenerate(capability: AiCapability) {
        when (capability) {
            AiCapability.IMAGE_GEN, AiCapability.IMAGE_EDIT -> generativeViewModel.generateImage()
            AiCapability.VIDEO -> generativeViewModel.generateVideo()
            AiCapability.CODE -> generativeViewModel.generateCode()
            AiCapability.AUDIO -> generativeViewModel.generateAudio()
            else -> Unit
        }
    }

    // Image/Video prompts route through the active safety preset's guard clause either way —
    // this only additionally asks the user to confirm first when that preset opts into it
    // (SafetyPresets.byId(id).confirm), same gate UnifiedStudioPane used to apply per-studio.
    fun onGenerate(capability: AiCapability) {
        val requiresConfirm = capability in setOf(AiCapability.IMAGE_GEN, AiCapability.VIDEO) &&
            com.zakir.vestra.shared.safety.SafetyPresets.byId(safetyPresetId).confirm
        if (requiresConfirm) showSafetyConfirm = capability else dispatchGenerate(capability)
    }

    fun retryFor(capability: AiCapability) {
        generativeViewModel.bindStudio(capability)
        val normalized = if (capability == AiCapability.IMAGE_EDIT) AiCapability.IMAGE_GEN else capability
        mode = ComposerMode.entries.firstOrNull { it.capability == normalized } ?: mode
        generativeViewModel.clearResult()
        onGenerate(capability)
    }

    fun dismiss(capability: AiCapability) {
        generativeViewModel.bindStudio(capability)
        generativeViewModel.dismissLastTurn()
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        generativeViewModel.setReference(uri?.toString())
    }

    val currentActiveModelLabel = if (mode == ComposerMode.CHAT) {
        chatModelLabel(chatViewModel, appSettings)
    } else {
        generativeViewModel.preflightLabel(mode.capability!!) ?: "Select model"
    }
    val activeProvider = pickerModels.firstOrNull { it.id == selectedModelId }
    val currentServiceLabel = when {
        selectedModelId.startsWith("local-") -> "On-Device"
        activeProvider?.platform == CloudPlatform.GEMINI -> "Gemini"
        activeProvider?.platform == CloudPlatform.HF_SPACE || activeProvider?.platform == CloudPlatform.HF_INFERENCE -> "HuggingFace"
        activeProvider?.platform == CloudPlatform.GROQ -> "Groq"
        activeProvider?.platform == CloudPlatform.OPENROUTER -> "OpenRouter"
        else -> if (selectedModelId.startsWith("local")) "On-Device" else "Cloud"
    }
    val isCloud = !selectedModelId.startsWith("local-")

    fun onSelectService(serviceKey: String) {
        when (serviceKey) {
            "GEMINI" -> {
                val geminiModel = pickerModels.firstOrNull { it.platform == CloudPlatform.GEMINI }
                if (geminiModel != null) selectModel(geminiModel.id) else showModelPicker = true
            }
            "HF" -> {
                val hfModel = pickerModels.firstOrNull {
                    it.platform == CloudPlatform.HF_SPACE || it.platform == CloudPlatform.HF_INFERENCE
                }
                if (hfModel != null) selectModel(hfModel.id) else showModelPicker = true
            }
            "GROQ" -> {
                val groqModel = pickerModels.firstOrNull { it.platform == CloudPlatform.GROQ }
                if (groqModel != null) selectModel(groqModel.id) else showModelPicker = true
            }
            "OPENROUTER" -> {
                val openRouterModel = pickerModels.firstOrNull { it.platform == CloudPlatform.OPENROUTER }
                if (openRouterModel != null) selectModel(openRouterModel.id) else showModelPicker = true
            }
            "ON_DEVICE" -> {
                val readyLocal = onDeviceEntries.firstOrNull { it.ready }
                if (readyLocal != null) {
                    when (pickerCapability) {
                        AiCapability.IMAGE_EDIT, AiCapability.IMAGE_GEN, AiCapability.VIDEO, AiCapability.CODE ->
                            appSettings.setLocalGenerator(pickerCapability, readyLocal.id)
                        else -> Unit
                    }
                } else {
                    showModelPicker = true
                }
            }
        }
    }

    val context = LocalContext.current
    val vestraApp = remember(context) { context.applicationContext as? VestraApp }
    val apiKeyDataStore = vestraApp?.apiKeyDataStore
    val usageData by apiKeyDataStore?.usageDashboardFlow?.collectAsState(initial = ApiKeyDataStore.ApiUsageDashboardData())
        ?: remember { mutableStateOf(ApiKeyDataStore.ApiUsageDashboardData()) }
    var showUsageDashboard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SpatialBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            UnifiedTopBar(
                currentModelLabel = currentActiveModelLabel,
                currentServiceLabel = currentServiceLabel,
                isCloud = isCloud,
                isUsageActive = showUsageDashboard,
                onToggleUsage = { showUsageDashboard = !showUsageDashboard },
                onModelSelectorClick = { showModelPicker = true },
                onSelectService = ::onSelectService,
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = onOpenSettings,
            )

            Box(Modifier.padding(horizontal = SpacingTokens.section)) {
                InterruptedJobsBanner(localJobStore)
            }

            if (showUsageDashboard && entries.isNotEmpty()) {
                Box(Modifier.padding(horizontal = SpacingTokens.section, vertical = 4.dp)) {
                    ApiUsageDashboardCard(
                        data = usageData,
                        onOpenSettings = onOpenSettings,
                        onClearHistory = {
                            scope.launch { apiKeyDataStore?.clearSessionUsageHistory() }
                        },
                        initiallyExpanded = true,
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = SpacingTokens.section, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (entries.isEmpty()) {
                    item(key = "usage_dashboard_empty_card") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ApiUsageDashboardCard(
                                data = usageData,
                                onOpenSettings = onOpenSettings,
                                onClearHistory = {
                                    scope.launch { apiKeyDataStore?.clearSessionUsageHistory() }
                                },
                                initiallyExpanded = true,
                            )
                        }
                    }
                }

                itemsIndexed(
                    entries,
                    key = { _, entry ->
                        when (entry) {
                            is ThreadEntry.Chat -> "chat_${entry.message.id}"
                            is ThreadEntry.Generative -> "gen_${entry.turn.id}"
                        }
                    },
                ) { index, entry ->
                    when (entry) {
                        is ThreadEntry.Chat -> ChatMessageBubble(message = entry.message, index = index)
                        is ThreadEntry.Generative -> {
                            val isLatest = entry.turn.id == latestIdByCapability[entry.turn.capability]
                            StudioTurnBubble(
                                turn = entry.turn,
                                index = index,
                                isLatest = isLatest,
                                accent = VestraColors.modalityAccent(entry.turn.capability),
                                generationStartedAtMs = generationStartedAtMs,
                                onRetry = if (isLatest) {
                                    { retryFor(entry.turn.capability) }
                                } else {
                                    null
                                },
                                onDismiss = if (isLatest) {
                                    { dismiss(entry.turn.capability) }
                                } else {
                                    null
                                },
                                retryLabel = "Retry",
                            )
                        }
                    }
                }
                if (showChatTyping) {
                    item(key = "chat_typing") {
                        ChatTypingIndicator(modelLabel = chatModelLabel(chatViewModel, appSettings))
                    }
                }
            }

            val error = if (mode == ComposerMode.CHAT) chatError else genPreflight
            if (error != null) {
                Box(Modifier.padding(horizontal = SpacingTokens.section, vertical = 4.dp)) {
                    GlassErrorBanner(
                        message = error,
                        onDismiss = {
                            if (mode == ComposerMode.CHAT) chatViewModel.clearError() else generativeViewModel.clearResult()
                        },
                    )
                }
            }

            Column(Modifier.padding(horizontal = SpacingTokens.section, vertical = 10.dp)) {
                ModalityChipRow(
                    selected = mode,
                    onSelect = { next ->
                        mode = next
                        next.capability?.let(generativeViewModel::bindStudio)
                    },
                )
                Spacer(Modifier.height(10.dp))
                PromptComposer(
                    prompt = if (mode == ComposerMode.CHAT) chatInput else genPrompt,
                    onPromptChange = {
                        if (mode == ComposerMode.CHAT) chatInput = it else generativeViewModel.setPrompt(it)
                    },
                    modelLabel = if (mode == ComposerMode.CHAT) {
                        chatModelLabel(chatViewModel, appSettings)
                    } else {
                        generativeViewModel.preflightLabel(mode.capability!!) ?: "Select a model in Settings"
                    },
                    busy = if (mode == ComposerMode.CHAT) chatBusy else genBusy,
                    onModelClick = { showModelPicker = true },
                    enabled = true,
                    onSend = {
                        when (mode) {
                            ComposerMode.CHAT -> {
                                val text = chatInput
                                chatInput = ""
                                chatViewModel.send(text)
                            }
                            ComposerMode.IMAGE -> onGenerate(AiCapability.IMAGE_GEN)
                            ComposerMode.VIDEO -> onGenerate(AiCapability.VIDEO)
                            ComposerMode.CODE -> onGenerate(AiCapability.CODE)
                            ComposerMode.AUDIO -> onGenerate(AiCapability.AUDIO)
                        }
                    },
                    onStop = { if (mode == ComposerMode.CHAT) chatViewModel.cancel() else generativeViewModel.forceStop() },
                    accent = mode.accent,
                    placeholder = "Ask Lookbook to create anything…",
                    referenceUri = if (mode == ComposerMode.IMAGE) genReference else null,
                    onAddReference = if (mode == ComposerMode.IMAGE) {
                        { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    } else {
                        null
                    },
                    onClearReference = if (mode == ComposerMode.IMAGE) {
                        { generativeViewModel.setReference(null) }
                    } else {
                        null
                    },
                )
            }

            if (showModelPicker) {
                ModelPickerSheet(
                    title = when (pickerCapability) {
                        AiCapability.IMAGE_EDIT -> "Image edit models"
                        AiCapability.IMAGE_GEN -> "Image models"
                        AiCapability.VIDEO -> "Video models"
                        AiCapability.CODE -> if (mode == ComposerMode.CHAT) "Chat models" else "Coding models"
                        else -> "Models"
                    },
                    models = pickerModels,
                    selectedId = selectedModelId,
                    onDeviceEntries = onDeviceEntries,
                    health = appSettings.modelHealth,
                    accent = mode.accent,
                    hasCredential = { model -> appSettings.cloudUsable(model) },
                    onSelect = { chosen -> selectModel(chosen.id) },
                    onSelectDevice = { entry ->
                        if (!entry.ready) return@ModelPickerSheet
                        when (pickerCapability) {
                            AiCapability.IMAGE_EDIT, AiCapability.IMAGE_GEN, AiCapability.VIDEO, AiCapability.CODE ->
                                appSettings.setLocalGenerator(pickerCapability, entry.id)
                            else -> Unit
                        }
                    },
                    onDismiss = { showModelPicker = false },
                )
            }

            showSafetyConfirm?.let { pendingCapability ->
                com.zakir.vestra.ui.components.SafetyConfirmDialog(
                    preset = com.zakir.vestra.shared.safety.SafetyPresets.byId(safetyPresetId),
                    onConfirm = {
                        showSafetyConfirm = null
                        dispatchGenerate(pendingCapability)
                    },
                    onCancel = { showSafetyConfirm = null },
                )
            }
        }
    }
}

@Composable
private fun UnifiedTopBar(
    currentModelLabel: String,
    currentServiceLabel: String,
    isCloud: Boolean,
    isUsageActive: Boolean = false,
    onToggleUsage: () -> Unit = {},
    onModelSelectorClick: () -> Unit,
    onSelectService: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.section, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(VestraColors.Accent),
            )
            Spacer(Modifier.width(9.dp))
            Text(LookbookCopy.PRODUCT_NAME, style = MaterialTheme.typography.titleMedium, color = VestraColors.Ink)
        }

        Box(contentAlignment = Alignment.Center) {
            var dropdownExpanded by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .testTag(TestTags.TOP_MODEL_SELECTOR)
                    .clip(RoundedCornerShape(50))
                    .background(VestraColors.GlassFillStrong)
                    .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(50))
                    .clickable { dropdownExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isCloud) VestraColors.Accent else VestraColors.SaffronDeep),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$currentServiceLabel · $currentModelLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = VestraColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 130.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = "Switch model or service",
                    tint = VestraColors.InkMuted,
                    modifier = Modifier.size(16.dp),
                )
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier
                    .background(VestraColors.SurfaceRaised)
                    .widthIn(min = 220.dp, max = 280.dp),
            ) {
                DropdownMenuItem(
                    text = { Text("⚡ Google Gemini", color = VestraColors.Ink) },
                    onClick = {
                        dropdownExpanded = false
                        onSelectService("GEMINI")
                    },
                    leadingIcon = {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(VestraColors.Accent))
                    },
                )
                DropdownMenuItem(
                    text = { Text("🤗 Hugging Face", color = VestraColors.Ink) },
                    onClick = {
                        dropdownExpanded = false
                        onSelectService("HF")
                    },
                    leadingIcon = {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(VestraColors.SaffronDeep))
                    },
                )
                DropdownMenuItem(
                    text = { Text("⚡ Groq", color = VestraColors.Ink) },
                    onClick = {
                        dropdownExpanded = false
                        onSelectService("GROQ")
                    },
                    leadingIcon = {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(VestraColors.AccentSoft))
                    },
                )
                DropdownMenuItem(
                    text = { Text("🌐 OpenRouter", color = VestraColors.Ink) },
                    onClick = {
                        dropdownExpanded = false
                        onSelectService("OPENROUTER")
                    },
                    leadingIcon = {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(VestraColors.ModalityCode))
                    },
                )
                DropdownMenuItem(
                    text = { Text("💻 On-Device (Offline)", color = VestraColors.Ink) },
                    onClick = {
                        dropdownExpanded = false
                        onSelectService("ON_DEVICE")
                    },
                    leadingIcon = {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(VestraColors.SaffronDeep))
                    },
                )
                HorizontalDivider(color = VestraColors.GlassBorder)
                DropdownMenuItem(
                    text = { Text("Browse all models…", color = VestraColors.Accent) },
                    onClick = {
                        dropdownExpanded = false
                        onModelSelectorClick()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = VestraColors.Accent,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TopBarIconButton(
                icon = Icons.Outlined.Analytics,
                contentDescription = "Cloud Usage & Token Monitor",
                testTag = TestTags.API_USAGE_TOGGLE_BUTTON,
                isActive = isUsageActive,
                onClick = onToggleUsage,
            )
            TopBarIconButton(
                icon = Icons.Outlined.Checkroom,
                contentDescription = "Open Library",
                testTag = TestTags.UNIFIED_LIBRARY_BUTTON,
                onClick = onOpenLibrary,
            )
            TopBarIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = "Open Settings",
                testTag = TestTags.UNIFIED_SETTINGS_BUTTON,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (isActive) VestraColors.Accent.copy(alpha = 0.18f) else VestraColors.GlassFillStrong
    val borderCol = if (isActive) VestraColors.Accent else VestraColors.GlassBorder
    val iconTint = if (isActive) VestraColors.Accent else VestraColors.InkMuted
    Box(
        Modifier
            .size(36.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderCol, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ModalityChipRow(selected: ComposerMode, onSelect: (ComposerMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComposerMode.entries.forEach { candidate ->
            val isSelected = candidate == selected
            val shape = RoundedCornerShape(50)
            Row(
                Modifier
                    .testTag(TestTags.modalityChip(candidate.name.lowercase()))
                    .clip(shape)
                    .background(if (isSelected) candidate.accent.copy(alpha = 0.16f) else VestraColors.GlassFill)
                    .border(
                        1.dp,
                        if (isSelected) candidate.accent.copy(alpha = 0.5f) else VestraColors.GlassBorder,
                        shape,
                    )
                    .clickable { onSelect(candidate) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(candidate.accent))
                Spacer(Modifier.width(7.dp))
                Text(
                    candidate.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) VestraColors.Ink else VestraColors.InkMuted,
                )
            }
        }
    }
}

private fun chatModelLabel(chatViewModel: ChatViewModel, appSettings: AppSettings): String =
    LocalModelCatalog.byId(chatViewModel.currentModelId())?.displayName
        ?: appSettings.selectedProvider(AiCapability.CODE).displayName
