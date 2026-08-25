package com.zakir.vestra.ui.screens.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.chat.ContextBudget
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.news.NewsItem
import com.zakir.vestra.shared.news.NewsRepository
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.ChatViewModel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.LiveGenConsole
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.QuickPromptItem
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.launch

@Composable
fun NewsChatScreen(
    newsRepository: NewsRepository?,
    chatViewModel: ChatViewModel?,
    appSettings: AppSettings? = null,
    freeCloudDiscovery: FreeCloudDiscovery? = null,
    packManager: ModelPackManager? = null,
    onHeadlineSelected: (String?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val newsItems by newsRepository?.items?.collectAsState()
        ?: remember { mutableStateOf(emptyList<NewsItem>()) }
    val newsError by newsRepository?.error?.collectAsState()
        ?: remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    val chatMessages by chatViewModel?.messages?.collectAsState()
        ?: remember { mutableStateOf(emptyList<com.zakir.vestra.shared.chat.ChatMessage>()) }
    val chatBusy by chatViewModel?.busy?.collectAsState() ?: remember { mutableStateOf(false) }
    val chatError by chatViewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val chatLogs by chatViewModel?.formattedLogs?.collectAsState() ?: remember { mutableStateOf(emptyList<String>()) }
    var chatInput by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }

    val codeId by appSettings?.codeProviderId?.collectAsState()
        ?: remember { mutableStateOf(CloudModelCatalog.defaultFor(AiCapability.CODE).id) }
    val cloudEnabled by appSettings?.cloudModelsEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val packStates by packManager?.states?.collectAsState()
        ?: remember { mutableStateOf(emptyMap()) }
    val chatProvider = appSettings?.selectedProvider(AiCapability.CODE)
        ?: CloudModelCatalog.defaultFor(AiCapability.CODE)

    // Cloud rows only exist once the master toggle is on — otherwise the sheet is
    // on-device only, matching what preflight() will actually allow to run.
    val pickerModels = remember(freeCloudDiscovery, appSettings, cloudEnabled) {
        when {
            !cloudEnabled -> emptyList()
            appSettings != null && freeCloudDiscovery != null ->
                freeCloudDiscovery.selectable(appSettings, AiCapability.CODE)
            else -> CloudModelCatalog.forCapability(AiCapability.CODE)
        }
    }
    val onDeviceEntries = remember(packStates) {
        LocalModelCatalog.forStudioPicker(AiCapability.CODE).map { entry ->
            val packReady = entry.packId?.let { packStates[it]?.isReady() == true } == true ||
                packStates[entry.id]?.isReady() == true
            OnDevicePickerEntry(
                id = entry.id,
                displayName = entry.displayName,
                detail = entry.testingNote,
                ready = LocalModelCatalog.studioEntryReady(entry, packReady),
                statusLabel = LocalModelCatalog.studioStatusLabel(entry, packReady),
            )
        }
    }
    val quickPrompts = remember(newsItems) {
        val headlinePrompts = newsItems.take(2).map { item ->
            QuickPromptItem(
                prompt = "Discuss this headline for modest fashion and on-device AI: ${item.title}",
                tag = item.source.take(12),
            )
        }
        headlinePrompts + QuickPromptItem("What can this app do on-device?", "HELP")
    }
    val localChatSelected = LocalModelCatalog.isSelectableStudioId(codeId, AiCapability.CODE)
    val chatModelLabel = if (localChatSelected) {
        (LocalModelCatalog.byId(codeId)?.displayName ?: "Local on-device") + " (offline)"
    } else {
        chatProvider.displayName
    }

    LaunchedEffect(newsRepository) {
        if (newsRepository != null) {
            refreshing = true
            newsRepository.refresh()
            refreshing = false
        }
    }

    SpatialBackground {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        if (newsRepository == null) {
            GlassCard(onClick = { onHeadlineSelected(null) }) {
                Text("News feed unavailable.", style = MaterialTheme.typography.bodyMedium, color = VestraColors.InkMuted)
            }
            return@Column
        }

        if (newsError != null && newsItems.isEmpty()) {
            GlassCard {
                Text(newsError ?: "Could not load headlines.", style = MaterialTheme.typography.bodyMedium, color = VestraColors.InkMuted)
            }
            Spacer(Modifier.height(12.dp))
        }

        NewsHeadlinesBar(
            newsItems = newsItems,
            refreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    try {
                        newsRepository.refresh()
                    } finally {
                        refreshing = false
                    }
                }
            },
            onHeadlineClick = { item, _ ->
                chatInput = "Discuss this headline for modest fashion and on-device AI: ${item.title}"
                onHeadlineSelected(item.title)
            },
        )

        if (chatViewModel != null) {
            Spacer(Modifier.height(16.dp))
            if (chatMessages.isEmpty() && !chatBusy) {
                ChatEmptyState(onPromptSelected = { chatInput = it })
            } else {
                chatMessages.takeLast(6).forEachIndexed { index, msg ->
                    ChatMessageBubble(message = msg, index = index)
                }
                if (chatBusy) {
                    ChatTypingIndicator(modelLabel = chatModelLabel)
                }
            }
            if (chatError != null) {
                Spacer(Modifier.height(8.dp))
                GlassErrorBanner(
                    message = chatError!!,
                    onRetry = { chatViewModel.clearError() },
                    retryLabel = "Dismiss",
                    onDismiss = { chatViewModel.clearError() },
                )
            }
            if (chatBusy) {
                LiveGenConsole(lines = chatLogs)
            }
            val contextBudget = remember(chatInput, chatMessages) {
                val before = chatViewModel.contextTokensBeforeDraft()
                val draftTokens = ContextBudget.estimateTokens(chatInput)
                ContextBudget.evaluate(before + draftTokens, chatViewModel.currentModelId())
            }
            ContextBudgetBar(budget = contextBudget, hasDraft = chatInput.isNotBlank())
            Spacer(Modifier.height(12.dp))
            PromptComposer(
                prompt = chatInput,
                onPromptChange = { chatInput = it },
                modelLabel = chatModelLabel,
                assistCount = 0,
                busy = chatBusy,
                enabled = true,
                onModelClick = { if (appSettings != null) showModelPicker = true },
                onSend = {
                    val text = chatInput
                    chatInput = ""
                    chatViewModel.send(text)
                },
                onStop = { chatViewModel.cancel() },
                placeholder = "Ask about headlines, local packs, or cloud models…",
                quickPrompts = quickPrompts,
                onSelectQuickPrompt = { chatInput = it },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    }

    if (showModelPicker && appSettings != null) {
        ModelPickerSheet(
            title = if (cloudEnabled) "Chat models" else "Chat models · on-device",
            models = pickerModels,
            selectedId = codeId,
            onDeviceEntries = onDeviceEntries,
            health = appSettings.modelHealth,
            onSelect = { chosen -> appSettings.setCodeProvider(chosen.id) },
            onSelectDevice = { entry ->
                if (entry.ready) appSettings.setLocalGenerator(AiCapability.CODE, entry.id)
            },
            onDismiss = { showModelPicker = false },
        )
    }
}
