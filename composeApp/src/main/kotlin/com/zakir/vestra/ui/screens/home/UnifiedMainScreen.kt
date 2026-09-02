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
import androidx.compose.material.icons.outlined.EditNote
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoCamera
import com.zakir.vestra.ui.components.HistoryRow
import com.zakir.vestra.ui.components.SectionHeaderRow
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.InterruptedJobsBanner
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.ActiveTool
import com.zakir.vestra.ui.components.ComposerSource
import com.zakir.vestra.ui.components.ComposerTool
import com.zakir.vestra.ui.components.ComposerToolsSheet
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    /** Label on the hero card's primary button for this mode. */
    val heroCta: String,
    /** How this mode reads as a capability tile when a *different* mode is active. */
    val capabilityTitle: String,
    val capabilitySubtitle: String,
    val icon: ImageVector,
    /** The composer's placeholder while this tool is active. */
    val placeholder: String,
    /** How this tool reads in the composer's `+` sheet. */
    val toolLabel: String,
    val toolDescription: String,
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
        heroCta = "Start Chat",
        capabilityTitle = "Ask Anything",
        capabilitySubtitle = "Advice & ideas",
        icon = Icons.AutoMirrored.Outlined.Chat,
        placeholder = "Ask Lookbook",
        toolLabel = "Chat",
        toolDescription = "Ask anything, get an answer",
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
        heroCta = "Create Image",
        capabilityTitle = "Image Studio",
        capabilitySubtitle = "Render a look",
        icon = Icons.Outlined.AutoAwesome,
        placeholder = "Describe an image to create",
        toolLabel = "Images",
        toolDescription = "Create and edit",
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
        heroCta = "Create Clip",
        capabilityTitle = "Clip Maker",
        capabilitySubtitle = "Short motion",
        icon = Icons.Outlined.Movie,
        placeholder = "Describe a short clip",
        toolLabel = "Videos",
        toolDescription = "Bring ideas to life",
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
        heroCta = "Write Code",
        capabilityTitle = "Code Wizard",
        capabilitySubtitle = "Debug & generate",
        icon = Icons.Outlined.Code,
        placeholder = "Ask for code",
        toolLabel = "Canvas",
        toolDescription = "Code, write, or explain",
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
        heroCta = "Speak It",
        capabilityTitle = "Voice Over",
        capabilitySubtitle = "Text to speech",
        icon = Icons.Outlined.GraphicEq,
        placeholder = "Type something to hear it spoken",
        toolLabel = "Audio",
        toolDescription = "Make audio tracks",
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
    val context = LocalContext.current
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
    val newestAssistantId = remember(chatMessages) {
        chatMessages.lastOrNull { it.role.equals("assistant", ignoreCase = true) }?.id
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
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Persist the read grant: the reference is held as a string across recompositions and
        // process death, and without this the URI is unreadable by the time generation runs.
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            generativeViewModel.setReference(it.toString())
        }
    }
    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCaptureUri?.let { generativeViewModel.setReference(it.toString()) }
        pendingCaptureUri = null
    }
    fun openCamera() {
        val captures = java.io.File(context.filesDir, "captures").apply { mkdirs() }
        val file = java.io.File(captures, "reference_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        pendingCaptureUri = uri
        captureLauncher.launch(uri)
    }
    val launchCamera = com.zakir.vestra.ui.util.rememberCameraGatedAction(
        onGranted = { openCamera() },
        onDenied = {
            com.zakir.vestra.ui.components.GlassSnackbar.show(
                "Camera permission is needed to take a reference photo",
                com.zakir.vestra.ui.components.SnackbarLevel.WARNING,
            )
        },
    )

    // Dictation runs in the system's own recogniser activity, so it needs no RECORD_AUDIO grant.
    // A device with no recogniser installed returns null here and the composer hides its mic
    // rather than offering a button that opens nothing.
    val dictateIntent = remember { com.zakir.vestra.ui.util.dictationIntent(context, "Speak your prompt") }
    val dictate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotEmpty()) {
            if (mode == ComposerMode.CHAT) chatInput = spoken else generativeViewModel.setPrompt(spoken)
        }
    }

    var showToolsSheet by remember { mutableStateOf(false) }

    // The chip names the model; the hint row underneath explains why it can't run, if it can't.
    // Folding both into one string is what produced "Pick a cloud model in the model pi…".
    val composerModelLabel = if (mode == ComposerMode.CHAT) {
        chatModelLabel(chatViewModel, appSettings)
    } else {
        generativeViewModel.modelLabel(mode.capability!!)
    }
    val composerBlockedReason = mode.capability?.let(generativeViewModel::blockedReason)

    // Recent activity for Home's HISTORY section, derived from the thread that already exists
    // rather than a second store. Empty on a cold install, which is why the section hides itself.
    val recentHistory = remember(allTurns, chatMessages) {
        (allTurns.map { it.prompt to it.timestampMs } + chatMessages.filter { it.role == "user" }.map { it.text to it.timestampMs })
            .sortedByDescending { it.second }
            .take(3)
            .mapIndexed { index, (text, ts) ->
                HomeHistoryEntry(
                    id = "h$index-$ts",
                    title = text.take(48),
                    subtitle = "Tap to reuse this prompt",
                    timeLabel = relativeTime(ts),
                )
            }
    }

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
                modelLabel = composerModelLabel,
                onModelClick = { showModelPicker = true },
                // Hidden on an empty thread rather than disabled: a "new chat" button on a
                // conversation that is already new is a control that does nothing.
                onNewChat = if (entries.isNotEmpty()) {
                    {
                        chatViewModel.clearHistory()
                        generativeViewModel.clearAllTurns()
                        chatInput = ""
                        generativeViewModel.setPrompt("")
                        generativeViewModel.setReference(null)
                        mode = ComposerMode.CHAT
                    }
                } else {
                    null
                },
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
                            history = recentHistory,
                            onHistory = { entry ->
                                if (mode == ComposerMode.CHAT) {
                                    chatInput = entry.title
                                } else {
                                    generativeViewModel.setPrompt(entry.title)
                                }
                            },
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
                        is ThreadEntry.Chat -> ChatMessageBubble(
                            message = entry.message,
                            index = index,
                            // Only the newest assistant turn can be regenerated — re-running an
                            // older one would have to discard every reply after it.
                            onRegenerate = if (entry.message.id == newestAssistantId && !chatBusy) {
                                chatViewModel::regenerate
                            } else {
                                null
                            },
                        )
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

            Column(Modifier.padding(horizontal = SpacingTokens.section, vertical = 8.dp)) {
                PromptComposer(
                    prompt = if (mode == ComposerMode.CHAT) chatInput else genPrompt,
                    onPromptChange = {
                        if (mode == ComposerMode.CHAT) chatInput = it else generativeViewModel.setPrompt(it)
                    },
                    blockedReason = composerBlockedReason,
                    busy = if (mode == ComposerMode.CHAT) chatBusy else genBusy,
                    enabled = true,
                    onSend = {
                        val text = chatInput
                        if (mode == ComposerMode.CHAT) chatInput = ""
                        send(text)
                    },
                    onStop = { if (mode == ComposerMode.CHAT) chatViewModel.cancel() else generativeViewModel.forceStop() },
                    accent = mode.accent,
                    placeholder = mode.placeholder,
                    onOpenTools = { showToolsSheet = true },
                    onDictate = dictateIntent?.let { intent -> { dictate.launch(intent) } },
                    activeTool = if (mode == ComposerMode.CHAT) {
                        null
                    } else {
                        ActiveTool(
                            label = mode.label,
                            icon = mode.icon,
                            accent = mode.accent,
                            onClear = { mode = ComposerMode.CHAT },
                        )
                    },
                    referenceUri = genReference,
                    onClearReference = { generativeViewModel.setReference(null) },
                )
                Spacer(Modifier.height(SpacingTokens.xxs))
                Text(
                    LookbookCopy.COMPOSER_DISCLAIMER,
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.InkMuted.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            if (showToolsSheet) {
                ComposerToolsSheet(
                    onDismiss = { showToolsSheet = false },
                    sources = listOf(
                        ComposerSource("photos", "Photos", Icons.Outlined.Image) {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        ComposerSource("camera", "Camera", Icons.Outlined.PhotoCamera) { launchCamera() },
                        ComposerSource("files", "Files", Icons.Outlined.AttachFile) {
                            pickFile.launch(arrayOf("image/*"))
                        },
                    ),
                    tools = ComposerMode.entries.map { candidate ->
                        ComposerTool(
                            id = candidate.name.lowercase(),
                            label = candidate.toolLabel,
                            description = candidate.toolDescription,
                            icon = candidate.icon,
                            accent = candidate.accent,
                            selected = candidate == mode,
                            onClick = {
                                mode = candidate
                                candidate.capability?.let(generativeViewModel::bindStudio)
                            },
                        )
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
    modelLabel: String,
    onModelClick: () -> Unit,
    /** Clears the thread. Null while it is already empty, so the control is never a no-op. */
    onNewChat: (() -> Unit)? = null,
) {
    // The model selector is the top bar's primary control, as it is in the reference app. It
    // used to sit in the composer's action row, where a 360dp phone gave it about 150dp and it
    // rendered "FLUX.1 Schnell · Ready · verified 6m ago" into that — three facts truncated to
    // one useless one. Up here it has the width for a name, and the composer got its row back.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.section, vertical = SpacingTokens.xs),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopModelSelector(
            label = modelLabel,
            onClick = onModelClick,
            modifier = Modifier.weight(1f),
        )
        if (onNewChat != null) {
            TopBarIconButton(
                icon = Icons.Outlined.EditNote,
                contentDescription = "Start a new chat",
                testTag = TestTags.NEW_CHAT_BUTTON,
                onClick = onNewChat,
            )
        }
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

/**
 * The active model's name, one tap from the picker.
 *
 * No app mark beside it, deliberately. A 28dp glyph plus its gap took 36dp of a 360dp phone's
 * top bar, and the first render of this control spent it: "Bonsai Image 4B (LiteRT)" came out as
 * "Bonsai Image 4B (Li…" — the mark cost exactly the characters that name the model. The user
 * knows which app they opened; they do not know which of forty models is about to run.
 */
@Composable
private fun TopModelSelector(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .testTag(TestTags.TOP_MODEL_SELECTOR)
            .heightIn(min = ControlTokens.iconButton)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Model: $label. Opens the model picker." }
            .padding(horizontal = SpacingTokens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            shortModelName(label),
            style = MaterialTheme.typography.titleSmall,
            color = VestraColors.Ink,
            maxLines = 1,
            // The full, unshortened name stays in the row's contentDescription, so neither the
            // trim above nor an ellipsis here can hide which model is selected from a reader.
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            Icons.Outlined.ArrowDropDown,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Drops a trailing parenthetical from a model name for display: "Llama 3.3 70B (Groq)" → "Llama
 * 3.3 70B".
 *
 * Catalog names carry their runtime in parentheses — `(Groq)`, `(LiteRT)`, `(ZeroGPU)`,
 * `(free models)`. That is the least useful part of the string in a one-line control and the
 * first part worth losing: the runtime is visible on the picker sheet the control opens, and
 * keeping it is what pushed the actual model number off the end.
 */
internal fun shortModelName(label: String): String {
    val open = label.lastIndexOf(" (")
    val trimmed = if (open > 0 && label.endsWith(")")) label.substring(0, open) else label
    return trimmed.trim().ifEmpty { label }
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
 * What the user sees before their first message.
 *
 * The reference app puts exactly three things here: its mark, one greeting line, and the
 * composer. This screen previously stacked a hero card, a 2×2 grid of capability tiles, three
 * suggestion cards and a history list above the composer — four competing calls to action on a
 * screen whose job is to get out of the way of one text field.
 *
 * The tiles in particular were pure duplication once the `+` sheet existed: they offered the same
 * five generators, from a second place, in different words. What survives is the mark, the
 * greeting, and one row of starters — starters earn their space because a cold install has
 * nothing in the thread to imply what the app can be asked for.
 *
 * History moved into the same principle: it renders only once there *is* history, and as compact
 * rows rather than a titled section competing with the greeting.
 */
@Composable
internal fun HomeEmptyState(
    mode: ComposerMode,
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
    history: List<HomeHistoryEntry> = emptyList(),
    onHistory: (HomeHistoryEntry) -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.HOME_EMPTY_STATE)
            .padding(top = 48.dp, bottom = SpacingTokens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(VestraColors.Accent, VestraColors.SaffronDeep),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = VestraColors.Ivory,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(SpacingTokens.md))
        Text(
            LookbookCopy.HOME_GREETING,
            style = MaterialTheme.typography.headlineSmall,
            color = VestraColors.Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(SpacingTokens.xs))
        Text(
            mode.emptyStatePrompt,
            style = MaterialTheme.typography.bodyMedium,
            color = VestraColors.InkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = SpacingTokens.md),
        )

        Spacer(Modifier.height(SpacingTokens.xl))
        suggestions.take(3).forEachIndexed { index, suggestion ->
            SuggestionCard(
                text = suggestion,
                accent = mode.accent,
                index = index,
                onClick = { onSuggestion(suggestion) },
            )
            Spacer(Modifier.height(SpacingTokens.xs))
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(SpacingTokens.lg))
            Box(Modifier.testTag(TestTags.HOME_HISTORY_SECTION)) {
                Column {
                    SectionHeaderRow(label = "RECENT")
                    Spacer(Modifier.height(SpacingTokens.sm))
                    history.forEachIndexed { index, entry ->
                        HistoryRow(
                            icon = Icons.AutoMirrored.Outlined.Chat,
                            title = entry.title,
                            subtitle = entry.subtitle,
                            timeLabel = entry.timeLabel,
                            onClick = { onHistory(entry) },
                            modifier = Modifier.testTag(TestTags.homeHistoryRow(index)),
                        )
                        Spacer(Modifier.height(SpacingTokens.xs))
                    }
                }
            }
        }
    }
}

/** One recent-activity row on Home. [timeLabel] is pre-formatted — the row does no date maths. */
data class HomeHistoryEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
)

@Composable
private fun SuggestionCard(text: String, accent: Color, index: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.homeSuggestion(index))
            .clip(shape)
            .background(VestraColors.GlassFill)
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

/** Coarse "2h ago" style stamp for history rows. Precision past the hour is not useful here. */
private fun relativeTime(timestampMs: Long): String {
    val deltaMin = ((System.currentTimeMillis() - timestampMs) / 60_000L).coerceAtLeast(0)
    return when {
        deltaMin < 1 -> "now"
        deltaMin < 60 -> "${deltaMin}m ago"
        deltaMin < 60 * 24 -> "${deltaMin / 60}h ago"
        else -> "${deltaMin / (60 * 24)}d ago"
    }
}

private fun chatModelLabel(chatViewModel: ChatViewModel, appSettings: AppSettings): String =
    LocalModelCatalog.byId(chatViewModel.currentModelId())?.displayName
        ?: appSettings.selectedProvider(AiCapability.CODE).displayName
