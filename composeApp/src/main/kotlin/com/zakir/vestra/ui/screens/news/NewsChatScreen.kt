package com.zakir.vestra.ui.screens.news

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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.news.NewsItem
import com.zakir.vestra.shared.news.NewsRepository
import com.zakir.vestra.ui.ChatViewModel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassErrorBanner
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.launch

@Composable
fun NewsChatScreen(
    newsRepository: NewsRepository?,
    chatViewModel: ChatViewModel?,
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
    var chatInput by remember { mutableStateOf("") }

    LaunchedEffect(newsRepository) {
        if (newsRepository != null) {
            refreshing = true
            newsRepository.refresh()
            refreshing = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassSectionLabel("NEWS & CHAT")
            if (newsRepository != null) {
                IconButton(
                    onClick = {
                        refreshing = true
                        scope.launch {
                            newsRepository.refresh()
                            refreshing = false
                        }
                    },
                    enabled = !refreshing,
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh news", tint = VestraColors.Accent)
                }
            }
        }
        Text(
            "Fashion and AI headlines — discuss any story below.",
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.InkMuted,
            modifier = Modifier.padding(bottom = 12.dp),
        )

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

        if (newsItems.isEmpty() && !refreshing) {
            GlassCard(onClick = { onHeadlineSelected(null) }) {
                Text("No headlines yet — refresh or start a chat below.", style = MaterialTheme.typography.bodyMedium, color = VestraColors.InkMuted)
            }
        } else {
            newsItems.take(8).forEach { item ->
                Spacer(Modifier.height(8.dp))
                GlassCard(onClick = {
                    chatInput = "Discuss this headline for modest fashion and on-device AI: ${item.title}"
                    onHeadlineSelected(item.title)
                }) {
                    Text(item.source, style = MaterialTheme.typography.labelSmall, color = VestraColors.Accent)
                    Spacer(Modifier.height(4.dp))
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = VestraColors.Ink)
                }
            }
        }

        if (chatViewModel != null) {
            Spacer(Modifier.height(20.dp))
            GlassSectionLabel("CHAT")
            chatMessages.takeLast(6).forEach { msg ->
                Spacer(Modifier.height(8.dp))
                GlassCard {
                    Text(msg.role.uppercase(), style = MaterialTheme.typography.labelSmall, color = VestraColors.Accent)
                    Spacer(Modifier.height(4.dp))
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = VestraColors.Ink)
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
            Spacer(Modifier.height(12.dp))
            PromptComposer(
                prompt = chatInput,
                onPromptChange = { chatInput = it },
                modelLabel = "News chat",
                assistCount = 0,
                busy = chatBusy,
                enabled = true,
                onModelClick = {},
                onSend = {
                    val text = chatInput
                    chatInput = ""
                    chatViewModel.send(text)
                },
                onStop = { chatViewModel.cancel() },
                placeholder = "Ask about headlines, local packs, or cloud models…",
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
