package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.settings.AppSettings
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

    private var job: Job? = null

    fun setPrompt(value: String) {
        _prompt.value = value
    }

    fun setReference(uri: String?) {
        _referenceUri.value = uri
    }

    fun generateImage() {
        val p = _prompt.value.trim()
        if (p.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            generative.generateImage(p, _referenceUri.value).collect { _state.value = it }
        }
    }

    fun generateCode() {
        val p = _prompt.value.trim()
        if (p.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            generative.generateCode(p).collect { _state.value = it }
        }
    }

    fun generateVideo() {
        val p = _prompt.value.trim()
        if (p.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            generative.generateVideo(p).collect { _state.value = it }
        }
    }

    fun clearResult() {
        job?.cancel()
        _state.value = null
    }
}
