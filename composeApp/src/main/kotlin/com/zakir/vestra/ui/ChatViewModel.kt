package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.chat.ChatRepository
import com.zakir.vestra.shared.chat.ContextBudget
import com.zakir.vestra.shared.chat.MemoryExtraction
import com.zakir.vestra.shared.chat.MemoryRepository
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.diagnostics.RunCapability
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
import com.zakir.vestra.shared.engine.local.LocalCodeStreamEvent
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.logging.LogSource
import com.zakir.vestra.shared.logging.LogStateManager
import com.zakir.vestra.shared.news.NewsRepository
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.PreflightResult
import kotlinx.coroutines.CancellationException
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
    private val memory: MemoryRepository? = null,
    private val logStateManager: LogStateManager = LogStateManager(),
) : ViewModel() {

    val messages = chat.messages

    // Live, transient "what's happening right now" event feed for this chat session — rendered
    // via LiveGenConsole, distinct from RunDiagnostics' persistent per-run records above.
    val formattedLogs = logStateManager.formattedLines

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
        logStateManager.clear()
    }

    fun send(prompt: String) {
        val text = prompt.trim().take(4000)
        if (text.isEmpty() || _busy.value) return

        when (val check = appSettings.preflight(AiCapability.CODE)) {
            is PreflightResult.Blocked -> {
                _error.value = check.reason
                logStateManager.warn(LogSource.SYSTEM, "Preflight blocked: ${check.reason}")
                return
            }
            is PreflightResult.Ok -> Unit
        }

        val provider = appSettings.selectedProvider(AiCapability.CODE)
        // A local on-device pick routes through chatWithFallback's local branch, so the
        // cloud-platform guard below must not reject it.
        val localChat = appSettings.prefersLocal(AiCapability.CODE) && generative.localCodeReady()
        if (!localChat &&
            provider.platform !in setOf(CloudPlatform.GROQ, CloudPlatform.OPENROUTER, CloudPlatform.HF_INFERENCE)
        ) {
            _error.value = "Pick a chat-capable coding model in Settings (Groq, OpenRouter, or HF Inference)."
            return
        }

        chat.append("user", text, provider.id)
        _error.value = null
        _busy.value = true
        val activeSource = if (localChat) LogSource.LITERT else LogSource.CLOUD_API
        logStateManager.info(activeSource, "Dispatching chat request to ${provider.displayName}...")

        val history = chat.contextForLlm(maxTurns = 10)
        val system = buildSystemPrompt()
        val composedPrompt = if (history.size <= 1) {
            text
        } else {
            history.dropLast(1).joinToString("\n\n") { (role, content) ->
                "${role.uppercase()}: $content"
            } + "\n\nUSER: $text"
        }

        // A local run must be recorded under its real local model, not the selected cloud
        // provider — the diagnostics run history used to tag every local Qwen3/Gemma reply as
        // whatever cloud model was selected (e.g. "Llama 3.3 70B (Groq)"), confirmed live in a
        // user's diagnostics export where the record's own note field named the local model that
        // actually ran while modelId/modelLabel still said the cloud one.
        val localProviderId = if (localChat) generative.localChatProviderId() else null
        val builder = runDiagnostics?.startRun(
            capability = RunCapability.CHAT,
            tier = null,
            modelId = localProviderId ?: provider.id,
            modelLabel = localProviderId?.let { LocalModelCatalog.byId(it)?.displayName ?: it }
                ?: provider.displayName,
            deviceRamMb = deviceRamMb,
        )

        job?.cancel()
        job = viewModelScope.launch {
            try {
                if (localChat) {
                    val streamed = streamLocalReply(composedPrompt, system)
                    if (streamed != null) {
                        logStateManager.info(
                            LogSource.LITERT,
                            "Reply ready from ${streamed.providerId} · tokens ${streamed.tokensIn}+${streamed.tokensOut}",
                        )
                        builder?.complete(
                            success = true,
                            note = "${streamed.providerId} · tokens ${streamed.tokensIn}+${streamed.tokensOut}",
                        )
                        maybeExtractMemory(text, streamed.text)
                        return@launch
                    }
                    // Local streaming failed (or wasn't actually ready by the time we asked) —
                    // fall through to chatWithFallback, which retries local once more before
                    // cloud and carries its own offline/cloud-disabled messaging.
                    logStateManager.warn(LogSource.LITERT, "Local session unavailable, falling back to cloud...")
                }
                logStateManager.info(LogSource.CLOUD_API, "Connecting to ${provider.displayName} (${provider.platform.name})...")
                val (result, used) = generative.chatWithFallback(
                    prompt = composedPrompt,
                    system = system,
                    capability = AiCapability.CODE,
                    temperature = 0.4,
                )
                chat.append("assistant", result.text, used.id)
                logStateManager.info(
                    LogSource.CLOUD_API,
                    "Reply received from ${used.id} · tokens ${result.tokensIn}+${result.tokensOut}",
                )
                builder?.complete(
                    success = true,
                    note = "${used.id} · tokens ${result.tokensIn}+${result.tokensOut}",
                )
                maybeExtractMemory(text, result.text)
            } catch (e: Exception) {
                val rawMsg = e.message?.take(280) ?: "Chat failed"
                // Thread the diagnostics run's own id into the on-screen message for local chat
                // failures so it's look-up-able in Settings → Diagnostics — the record already
                // had a stable id, it just never reached the user-facing string.
                _error.value = if (localChat && builder != null) "$rawMsg (ref ${builder.id})" else rawMsg
                logStateManager.error(activeSource, "Chat execution error: $rawMsg")
                builder?.complete(success = false, error = rawMsg)
            } finally {
                _busy.value = false
            }
        }
    }

    private class StreamedReply(val providerId: String, val text: String, val tokensIn: Int, val tokensOut: Int)

    /**
     * Streams a local reply into a live-updating assistant bubble. Returns null (after removing
     * the empty placeholder) when the local model turned out unavailable, so the caller can fall
     * back to [GenerativeCloudService.chatWithFallback] without leaving a ghost message behind.
     */
    private suspend fun streamLocalReply(prompt: String, system: String): StreamedReply? {
        val providerId = generative.localChatProviderId()
        logStateManager.info(LogSource.LITERT, "Streaming tokens from local model $providerId...")
        val messageId = chat.appendPlaceholder("assistant", providerId)
        var failure: String? = null
        var finalText = ""
        var tokensIn = 0
        var tokensOut = 0
        generative.localChatStream(prompt, system).collect { event ->
            when (event) {
                is LocalCodeStreamEvent.Partial -> chat.updateMessage(messageId, event.textSoFar)
                is LocalCodeStreamEvent.Done -> {
                    finalText = event.text
                    tokensIn = event.tokensIn
                    tokensOut = event.tokensOut
                    chat.updateMessage(messageId, event.text, persist = true)
                }
                is LocalCodeStreamEvent.Unavailable -> failure = event.reason
            }
        }
        if (failure != null) {
            logStateManager.warn(LogSource.LITERT, "Local stream unavailable: $failure")
            chat.removeMessage(messageId)
            return null
        }
        return StreamedReply(providerId, finalText, tokensIn, tokensOut)
    }

    /**
     * Runs the Part B.1 memory-extraction prompt through the local chat model after a reply,
     * and stores any durable facts it finds. Always uses the local model regardless of which
     * model answered the visible reply, per the plan's "fully portable on-device" design — a
     * cloud call here would be a second, unnecessary network round-trip for something that
     * never needs to leave the device. Silently skips (never surfaces as a chat error) when
     * memory is disabled, unavailable, or the extraction call itself fails — a missed
     * extraction is not a chat failure. Deliberately awaited inline in the same job coroutine
     * as the primary reply, not fire-and-forget: the local engine isn't safe for concurrent
     * generate calls, and running this after the primary `finally` clears `_busy` would let a
     * fast second `send()` race it.
     */
    private suspend fun maybeExtractMemory(userText: String, assistantText: String) {
        if (memory == null) return
        if (!appSettings.memoryEnabled.value) return
        if (!generative.localCodeReady()) return
        try {
            val prompt = MemoryExtraction.buildPrompt(userText, assistantText)
            var finalText: String? = null
            generative.localChatStream(prompt, MEMORY_EXTRACTION_SYSTEM).collect { event ->
                if (event is LocalCodeStreamEvent.Done) finalText = event.text
            }
            val facts = finalText?.let { MemoryExtraction.parseFacts(it) }.orEmpty()
            if (facts.isNotEmpty()) {
                memory.addFacts(facts)
                logStateManager.info(LogSource.LITERT, "Remembered ${facts.size} new fact(s).")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logStateManager.warn(LogSource.LITERT, "Memory extraction skipped: ${e.message}")
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _busy.value = false
        logStateManager.warn(LogSource.SYSTEM, "Generation cancelled.")
    }

    private fun buildSystemPrompt(): String {
        val headlines = news?.headlineContext(5).orEmpty()
        val rememberedFacts = if (appSettings.memoryEnabled.value) {
            memory?.contextForSystemPrompt().orEmpty()
        } else {
            ""
        }
        return buildString {
            append("You are a helpful assistant for The Lookbook — modest fashion try-on and on-device AI. ")
            append("Discuss headlines, local Lite/Pro packs, and cloud free-tier models. Keep answers concise.")
            if (headlines.isNotBlank()) {
                append("\n\nRecent headlines:\n")
                append(headlines)
            }
            if (rememberedFacts.isNotBlank()) {
                append("\n\nWhat you remember about this user:\n")
                append(rememberedFacts)
            }
        }
    }

    /**
     * The model id [send] would currently dispatch to — the ready local pick if one is
     * selected and available, otherwise the selected cloud provider. Used by the composer's
     * live context-budget estimate (Part B.2), computed without side effects before a send.
     */
    fun currentModelId(): String {
        val localChat = appSettings.prefersLocal(AiCapability.CODE) && generative.localCodeReady()
        return if (localChat) generative.localChatProviderId() else appSettings.selectedProvider(AiCapability.CODE).id
    }

    /**
     * Token estimate for everything [send] would compose *besides* the live draft — the system
     * prompt plus prior turns' history — so the composer can add the draft's own token count on
     * top and warn before a send would actually truncate.
     */
    fun contextTokensBeforeDraft(): Int {
        val history = chat.contextForLlm(maxTurns = 10)
        val historyText = history.joinToString("\n\n") { (role, content) -> "${role.uppercase()}: $content" }
        return ContextBudget.estimateTokens(buildSystemPrompt()) + ContextBudget.estimateTokens(historyText)
    }

    private companion object {
        const val MEMORY_EXTRACTION_SYSTEM =
            "You extract structured data. Reply with ONLY the requested JSON, no explanation."
    }
}
