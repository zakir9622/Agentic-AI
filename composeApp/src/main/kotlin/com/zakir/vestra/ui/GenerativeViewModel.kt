package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.PreflightResult
import com.zakir.vestra.shared.usage.UsageLedger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GenerativeViewModel(
    private val generative: GenerativeCloudService,
    val appSettings: AppSettings,
    val usage: UsageLedger,
) : ViewModel() {

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    private val _referenceUri = MutableStateFlow<String?>(null)
    val referenceUri: StateFlow<String?> = _referenceUri

    private val _state = MutableStateFlow<GenerativeState?>(null)
    val state: StateFlow<GenerativeState?> = _state

    private val _preflightMessage = MutableStateFlow<String?>(null)
    val preflightMessage: StateFlow<String?> = _preflightMessage

    private var job: Job? = null
    private var generationEpoch = 0

    fun setPrompt(value: String) {
        _prompt.value = value.take(MAX_PROMPT)
        _preflightMessage.value = null
    }

    fun setReference(uri: String?) {
        _referenceUri.value = uri
        _preflightMessage.value = null
    }

    fun generateImage() {
        val p = _prompt.value.trim()
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter a prompt describing the image."
            return
        }
        val capability = if (_referenceUri.value == null) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT
        when (val check = appSettings.preflight(capability)) {
            is PreflightResult.Blocked -> {
                _preflightMessage.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }
        startGeneration { generative.generateImage(p, _referenceUri.value) }
    }

    fun generateCode() {
        val p = _prompt.value.trim()
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter a coding prompt."
            return
        }
        when (val check = appSettings.preflight(AiCapability.CODE)) {
            is PreflightResult.Blocked -> {
                _preflightMessage.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }
        startGeneration { generative.generateCode(p) }
    }

    fun generateVideo() {
        val p = _prompt.value.trim()
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter a video prompt."
            return
        }
        when (val check = appSettings.preflight(AiCapability.VIDEO)) {
            is PreflightResult.Blocked -> {
                _preflightMessage.value = check.reason
                return
            }
            is PreflightResult.Ok -> Unit
        }
        startGeneration { generative.generateVideo(p) }
    }

    fun cancel() {
        job?.cancel()
        job = null
        generationEpoch++
        _state.value = null
    }

    fun clearResult() {
        cancel()
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
                    if (epoch == generationEpoch) _state.value = next
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Expected on cancel / clear
            }
        }
    }

    private companion object {
        const val MAX_PROMPT = 2000
    }
}
