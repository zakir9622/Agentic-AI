package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.PreflightResult
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.shared.wardrobe.WardrobeEntry
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalUuidApi::class)
class GenerativeViewModel(
    private val generative: GenerativeCloudService,
    val appSettings: AppSettings,
    val usage: UsageLedger,
    private val wardrobe: WardrobeRepository,
) : ViewModel() {

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    private val _referenceUri = MutableStateFlow<String?>(null)
    val referenceUri: StateFlow<String?> = _referenceUri

    private val _state = MutableStateFlow<GenerativeState?>(null)
    val state: StateFlow<GenerativeState?> = _state

    private val _preflightMessage = MutableStateFlow<String?>(null)
    val preflightMessage: StateFlow<String?> = _preflightMessage

    /** Higher creativity / temperature for coding models. */
    private val _creativeMode = MutableStateFlow(false)
    val creativeMode: StateFlow<Boolean> = _creativeMode

    /** Softer refusals — complete lawful coding tasks instead of declining. */
    private val _pragmaticMode = MutableStateFlow(true)
    val pragmaticMode: StateFlow<Boolean> = _pragmaticMode

    /** Extra sharpness / coherence clauses for image & video prompts. */
    private val _detailBoost = MutableStateFlow(true)
    val detailBoost: StateFlow<Boolean> = _detailBoost

    /** Fashion/lookbook framing so garment prompts are less often blocked. */
    private val _fashionContext = MutableStateFlow(true)
    val fashionContext: StateFlow<Boolean> = _fashionContext

    private var job: Job? = null
    private var generationEpoch = 0

    val isBusy: Boolean
        get() {
            val s = _state.value
            return s is GenerativeState.Preparing || s is GenerativeState.Running
        }

    fun setPrompt(value: String) {
        _prompt.value = value.take(MAX_PROMPT)
        _preflightMessage.value = null
    }

    fun setReference(uri: String?) {
        _referenceUri.value = uri
        _preflightMessage.value = null
    }

    fun setCreativeMode(enabled: Boolean) {
        _creativeMode.value = enabled
    }

    fun setPragmaticMode(enabled: Boolean) {
        _pragmaticMode.value = enabled
    }

    fun setDetailBoost(enabled: Boolean) {
        _detailBoost.value = enabled
    }

    fun setFashionContext(enabled: Boolean) {
        _fashionContext.value = enabled
    }

    /**
     * Open a studio without killing an in-flight job. Only resets prompt/result when idle.
     */
    fun prepareStudio(resetIfIdle: Boolean = true) {
        if (!resetIfIdle || isBusy) return
        _state.value = null
        _preflightMessage.value = null
        _prompt.value = ""
        _referenceUri.value = null
    }

    fun generateImage() {
        val p = sanitizePrompt(_prompt.value)
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter a prompt describing the image."
            return
        }
        _prompt.value = p
        val capability = if (_referenceUri.value == null) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT
        when (val check = appSettings.preflight(capability)) {
            is PreflightResult.Blocked -> {
                _preflightMessage.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }
        startGeneration {
            generative.generateImage(
                p,
                _referenceUri.value,
                detailBoost = _detailBoost.value,
                fashionContext = _fashionContext.value,
            )
        }
    }

    fun generateCode() {
        val p = sanitizePrompt(_prompt.value)
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter a coding prompt."
            return
        }
        _prompt.value = p
        when (val check = appSettings.preflight(AiCapability.CODE)) {
            is PreflightResult.Blocked -> {
                _preflightMessage.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }
        startGeneration {
            generative.generateCode(
                p,
                creative = _creativeMode.value,
                pragmatic = _pragmaticMode.value,
            )
        }
    }

    fun generateVideo() {
        val p = sanitizePrompt(_prompt.value)
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter a video prompt."
            return
        }
        _prompt.value = p
        when (val check = appSettings.preflight(AiCapability.VIDEO)) {
            is PreflightResult.Blocked -> {
                _preflightMessage.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }
        startGeneration {
            generative.generateVideo(
                p,
                detailBoost = _detailBoost.value,
                fashionContext = _fashionContext.value,
            )
        }
    }

    /** Soft cancel — clears UI; in-flight HTTP may finish but result is ignored. */
    fun cancel() {
        forceStop(showStopped = false)
    }

    /** Force-stop: cancel job and show a recoverable Stopped state. */
    fun forceStop(showStopped: Boolean = true) {
        job?.cancel(CancellationException("force_stop"))
        job = null
        generationEpoch++
        _state.value = if (showStopped) {
            GenerativeState.Failed("Stopped. Tap Generate to run again.")
        } else {
            null
        }
    }

    fun clearResult() {
        forceStop(showStopped = false)
        _preflightMessage.value = null
    }

    private fun startGeneration(block: () -> kotlinx.coroutines.flow.Flow<GenerativeState>) {
        job?.cancel()
        val epoch = ++generationEpoch
        _preflightMessage.value = null
        _state.value = GenerativeState.Preparing("Starting…")
        job = viewModelScope.launch {
            try {
                block().collect { next ->
                    if (epoch != generationEpoch) return@collect
                    _state.value = next
                    if (next is GenerativeState.ImageReady) {
                        ingestCreateImage(next.path)
                    }
                }
            } catch (_: CancellationException) {
                // Expected on force stop / clear — state already set by forceStop when needed
            }
        }
    }

    private fun ingestCreateImage(path: String) {
        val promptSnippet = _prompt.value.trim().take(80).ifBlank { "create" }
        runCatching {
            wardrobe.add(
                WardrobeEntry(
                    id = Uuid.random().toString(),
                    createdAtEpochMillis = System.currentTimeMillis(),
                    imagePath = path,
                    garmentUri = "create:$promptSnippet",
                    personLabel = "Create",
                    tier = EngineTier.CLOUD,
                    shootId = null,
                ),
            )
        }
    }

    private fun sanitizePrompt(raw: String): String =
        raw.trim()
            .replace("\u0000", "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .take(MAX_PROMPT)

    private companion object {
        const val MAX_PROMPT = 4000
    }
}
