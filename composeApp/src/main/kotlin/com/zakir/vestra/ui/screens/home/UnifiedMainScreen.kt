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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
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
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.InterruptedJobsBanner
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.components.StudioTurnBubble
import com.zakir.vestra.ui.screens.news.ChatMessageBubble
import com.zakir.vestra.ui.screens.news.ChatTypingIndicator
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.launch

/**
 * The whole app is one screen: a single scrolling conversation that mixes Chat replies with
 * Image/Video/Code/Audio results, per the approved Gemini-style redesign. There is no dashboard
 * to browse and no bottom dock — Library and Settings are reached from the two icons at top
 * right. A composer mode chip (Chat/Image/Video/Code/Audio) picks which generator the next
 * message routes to; everything it produces lands in the same thread.
 *
 * [ComposerMode], [UnifiedTopBar], [HomeEmptyState] and [ModalityChipRow] are `internal` rather
 * than `private` so `ScreenshotTest` can render the real composables at several widths instead of
 * a reconstruction of them — the vertical-text regression this screen shipped with was invisible
 * precisely because nothing rendered these surfaces outside the app.
 */
internal enum class ComposerMode(
    val label: String,
    val capability: AiCapability?,
    /** One line under the greeting on an empty thread, naming what this mode does. */
    val emptyStatePrompt: String,
    /** One-tap starters for the empty state. Tapping one fills the composer and sends. */
    val suggestions: List<String>,
) {
    CHAT(
        "Chat",
        null,
        "Ask anything — styling advice, ideas, or a second opinion.",
        listOf(
            "Build me a capsule wardrobe for a week of travel",
            "What colours flatter a warm skin tone?",
            "Suggest three outfits for a winter wedding",
        ),
    ),
    IMAGE(
        "Image",
        AiCapability.IMAGE_GEN,
        "Describe a look and it gets rendered.",
        listOf(
            "A flowing linen abaya in warm sand, studio lighting",
            "Editorial street style, oversized wool coat, overcast city",
            "Silk scarf detail, macro shot, soft window light",
        ),
    ),
    VIDEO(
        "Video",
        AiCapability.VIDEO,
        "Describe a short clip and it gets animated.",
        listOf(
            "Slow pan across a rack of autumn coats",
            "Fabric catching the light as it falls",
            "A model turning to camera, golden hour",
        ),
    ),
    CODE(
        "Code",
        AiCapability.CODE,
        "Ask for code and get a runnable answer.",
        listOf(
            "Write a Kotlin extension that formats a price range",
            "Explain this Compose recomposition problem",
            "A SQL query for top sellers by category",
        ),
    ),
    AUDIO(
        "Audio",
        AiCapability.AUDIO,
        "Type something and hear it spoken.",
        listOf(
            "Read this season's lookbook introduction aloud",
            "Narrate a thirty-second product description",
            "Speak a friendly welcome message",
        ),
    ),
    ;

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

    // The chip names the model; the hint row underneath explains why it can't run, if it can't.
    // Folding both into one string is what produced "Pick a cloud model in the model pi…".
    val composerModelLabel = if (mode == ComposerMode.CHAT) {
        chatModelLabel(chatViewModel, appSettings)
    } else {
        generativeViewModel.modelLabel(mode.capability!!)
    }
    val composerBlockedReason = mode.capability?.let(generativeViewModel::blockedReason)

    fun send(text: String) {
        when (mode) {
            ComposerMode.CHAT -> chatViewModel.send(text)
            ComposerMode.IMAGE -> onGenerate(AiCapability.IMAGE_GEN)
            ComposerMode.VIDEO -> onGenerate(AiCapability.VIDEO)
            ComposerMode.CODE -> onGenerate(AiCapability.CODE)
            ComposerMode.AUDIO -> onGenerate(AiCapability.AUDIO)
        }
    }

    /**
     * Empty-state starter tap: seed the composer with the suggestion and fire it in one action.
     * Chat sends the text directly (its input is local state); every generative mode has to go
     * through the view model's prompt first, since that is what `generate*()` reads.
     */
    fun sendSuggestion(suggestion: String) {
        if (mode == ComposerMode.CHAT) {
            chatInput = ""
        } else {
            generativeViewModel.setPrompt(suggestion)
        }
        send(suggestion)
    }

    SpatialBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            UnifiedTopBar(
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = onOpenSettings,
            )

            Box(Modifier.padding(horizontal = SpacingTokens.section)) {
                InterruptedJobsBanner(localJobStore)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = SpacingTokens.section, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (entries.isEmpty()) {
                    item(key = "home_empty_state") {
                        HomeEmptyState(
                            mode = mode,
                            suggestions = mode.suggestions,
                            onSuggestion = ::sendSuggestion,
                        )
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
                    modelLabel = composerModelLabel,
                    blockedReason = composerBlockedReason,
                    busy = if (mode == ComposerMode.CHAT) chatBusy else genBusy,
                    onModelClick = { showModelPicker = true },
                    enabled = true,
                    onSend = {
                        val text = chatInput
                        if (mode == ComposerMode.CHAT) chatInput = ""
                        send(text)
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
internal fun UnifiedTopBar(
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Three unweighted children under `SpaceBetween` used to demand ~423dp of a 324dp content
    // width here: a brand block with no maxLines, a model chip capped at 130dp showing a
    // compound "service · model" string that therefore always ellipsized, and a fixed 120dp
    // action row. The chip is gone — the composer already owns model selection, and one model
    // control beats two — so the brand can take `weight(1f)` and the actions their natural size.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.section, vertical = SpacingTokens.sm),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(RadiusTokens.sm))
                    .background(VestraColors.Accent),
            )
            Spacer(Modifier.width(SpacingTokens.sm))
            Text(
                LookbookCopy.PRODUCT_NAME,
                style = MaterialTheme.typography.titleLarge,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(ControlTokens.iconButton)
            .testTag(testTag)
            .clip(CircleShape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = VestraColors.Ink, modifier = Modifier.size(18.dp))
    }
}

/**
 * What the user sees before their first generation. This slot used to hold the API usage
 * dashboard — a token counter as the hero of an otherwise empty screen. It is a greeting and
 * four one-tap starters now; the monitor moved to Settings, where telemetry belongs.
 *
 * Tapping a starter fills the composer and fires immediately, so a cold install reaches its
 * first result in two taps: pick a mode, tap a starter.
 */
@Composable
internal fun HomeEmptyState(
    mode: ComposerMode,
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.HOME_EMPTY_STATE)
            .padding(top = SpacingTokens.xxl, bottom = SpacingTokens.md),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            LookbookCopy.HOME_GREETING,
            style = MaterialTheme.typography.headlineMedium,
            color = VestraColors.Ink,
        )
        Spacer(Modifier.height(SpacingTokens.xs))
        Text(
            mode.emptyStatePrompt,
            style = MaterialTheme.typography.bodyLarge,
            color = VestraColors.InkMuted,
        )
        Spacer(Modifier.height(SpacingTokens.xl))
        GlassSectionLabel("TRY ONE OF THESE", color = VestraColors.InkMuted)
        suggestions.forEachIndexed { index, suggestion ->
            SuggestionCard(
                text = suggestion,
                accent = mode.accent,
                index = index,
                onClick = { onSuggestion(suggestion) },
            )
            Spacer(Modifier.height(SpacingTokens.xs))
        }
    }
}

@Composable
private fun SuggestionCard(text: String, accent: Color, index: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.homeSuggestion(index))
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = VestraColors.Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun ModalityChipRow(selected: ComposerMode, onSelect: (ComposerMode) -> Unit) {
    // Five short chips fit every phone this app supports, so the row distributes width evenly
    // rather than sitting in a `horizontalScroll` that offered no affordance and silently
    // stopped the chips from ever sharing the available space.
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xxs + 2.dp),
    ) {
        ComposerMode.entries.forEach { candidate ->
            val isSelected = candidate == selected
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .weight(1f)
                    .testTag(TestTags.modalityChip(candidate.name.lowercase()))
                    .height(ControlTokens.chip)
                    .clip(shape)
                    // Selection reads from a filled accent against a flat surface, not from a
                    // 16%-vs-0% alpha shift that was nearly invisible at chip size.
                    .background(if (isSelected) candidate.accent else VestraColors.GlassFillStrong)
                    .then(
                        if (isSelected) Modifier else Modifier.border(1.dp, VestraColors.GlassBorder, shape),
                    )
                    .clickable { onSelect(candidate) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    candidate.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) VestraColors.Ivory else VestraColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun chatModelLabel(chatViewModel: ChatViewModel, appSettings: AppSettings): String =
    LocalModelCatalog.byId(chatViewModel.currentModelId())?.displayName
        ?: appSettings.selectedProvider(AiCapability.CODE).displayName
