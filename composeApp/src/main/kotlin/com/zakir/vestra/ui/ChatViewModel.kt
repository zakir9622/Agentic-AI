package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.chat.ChatRepository
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.diagnostics.RunCapability
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
import com.zakir.vestra.shared.news.NewsRepository
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.PreflightResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chat: ChatRepository,
    private val news: NewsRepository?,
    private val generative: GenerativeCloudService,
    private val appSettings: AppSettings,
    private val runDiagnostics: RunDiagnostics?,
    private val deviceRamMb: Long?,
) : ViewModel() {

    val messages = chat.messages

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var job: Job? = null

    fun clearError() {
        _error.value = null
    }

    fun clearHistory() {
        chat.clear()
        _error.value = null
    }

    fun send(prompt: String) {
        val text = prompt.trim().take(4000)
        if (text.isEmpty() || _busy.value) return

        when (val check = appSettings.preflight(AiCapability.CODE)) {
            is PreflightResult.Blocked -> {
                _error.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }

        val provider = appSettings.selectedProvider(AiCapability.CODE)
        if (provider.platform !in setOf(CloudPlatform.GROQ, CloudPlatform.OPENROUTER, CloudPlatform.HF_INFERENCE)) {
            _error.value = "Pick a chat-capable coding model in Settings (Groq, OpenRouter, or HF Inference)."
            return
        }

        chat.append("user", text, provider.id)
        _error.value = null
        _busy.value = true

        val headlines = news?.headlineContext(5).orEmpty()
        val history = chat.contextForLlm(maxTurns = 10)
        val system = buildString {
            append("You are a helpful assistant for The Lookbook — modest fashion try-on and on-device AI. ")
            append("Discuss headlines, local Lite/Pro packs, and cloud free-tier models. Keep answers concise.")
            if (headlines.isNotBlank()) {
                append("\n\nRecent headlines:\n")
                append(headlines)
            }
        }
        val composedPrompt = if (history.size <= 1) {
            text
        } else {
            history.dropLast(1).joinToString("\n\n") { (role, content) ->
                "${role.uppercase()}: $content"
            } + "\n\nUSER: $text"
        }

        val builder = runDiagnostics?.startRun(
            capability = RunCapability.CHAT,
            tier = null,
            modelId = provider.id,
            modelLabel = provider.displayName,
            deviceRamMb = deviceRamMb,
        )

        job?.cancel()
        job = viewModelScope.launch {
            try {
                val result = generative.chat(
                    prompt = composedPrompt,
                    system = system,
                    capability = AiCapability.CODE,
                    temperature = 0.4,
                )
                chat.append("assistant", result.text, provider.id)
                builder?.complete(success = true, note = "tokens ${result.tokensIn}+${result.tokensOut}")
            } catch (e: Exception) {
                val msg = e.message?.take(280) ?: "Chat failed"
                _error.value = msg
                builder?.complete(success = false, error = msg)
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _busy.value = false
    }
}
