package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelContracts
import com.zakir.vestra.shared.cloud.GenerativeAssists
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.diagnostics.RunCapability
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
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
    private val runDiagnostics: RunDiagnostics? = null,
    private val deviceRamMb: Long? = null,
) : ViewModel() {

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    private val _referenceUri = MutableStateFlow<String?>(null)
    val referenceUri: StateFlow<String?> = _referenceUri

    private val _state = MutableStateFlow<GenerativeState?>(null)
    val state: StateFlow<GenerativeState?> = _state

    private val _preflightMessage = MutableStateFlow<String?>(null)
    val preflightMessage: StateFlow<String?> = _preflightMessage

    private val _lastUsedProviderId = MutableStateFlow<String?>(null)
    val lastUsedProviderId: StateFlow<String?> = _lastUsedProviderId

    private val _creativeMode = MutableStateFlow(false)
    val creativeMode: StateFlow<Boolean> = _creativeMode

    private val _pragmaticMode = MutableStateFlow(true)
    val pragmaticMode: StateFlow<Boolean> = _pragmaticMode

    private val _detailBoost = MutableStateFlow(true)
    val detailBoost: StateFlow<Boolean> = _detailBoost

    private val _fashionContext = MutableStateFlow(true)
    val fashionContext: StateFlow<Boolean> = _fashionContext

    private val _bypassFilter = MutableStateFlow(false)
    val bypassFilter: StateFlow<Boolean> = _bypassFilter

    private val _qualityGuard = MutableStateFlow(true)
    val qualityGuard: StateFlow<Boolean> = _qualityGuard

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

    fun setBypassFilter(enabled: Boolean) {
        _bypassFilter.value = enabled
    }

    fun setQualityGuard(enabled: Boolean) {
        _qualityGuard.value = enabled
    }

    fun currentAssists(): GenerativeAssists = GenerativeAssists(
        pragmatic = _pragmaticMode.value,
        creative = _creativeMode.value,
        fashionContext = _fashionContext.value,
        detailBoost = _detailBoost.value,
        bypassFilter = _bypassFilter.value,
        qualityGuard = _qualityGuard.value,
    )

    fun prepareStudio(resetIfIdle: Boolean = true) {
        if (!resetIfIdle || isBusy) return
        _state.value = null
        _preflightMessage.value = null
        _prompt.value = ""
        _referenceUri.value = null
    }

    fun preflightLabel(capability: AiCapability): String? {
        return when (val check = appSettings.preflight(capability)) {
            is PreflightResult.Blocked -> check.reason
            is PreflightResult.Ok -> "${check.provider.displayName} · ${CloudModelContracts.statusLabel(check.provider)}"
        }
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
        startGeneration(
            capability = if (_referenceUri.value == null) RunCapability.IMAGE_GEN else RunCapability.IMAGE_EDIT,
            modelLabel = appSettings.selectedProvider(capability).displayName,
        ) {
            generative.generateImage(p, _referenceUri.value, currentAssists())
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
        startGeneration(RunCapability.CODE, appSettings.selectedProvider(AiCapability.CODE).displayName) {
            generative.generateCode(p, currentAssists())
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
        startGeneration(RunCapability.VIDEO, appSettings.selectedProvider(AiCapability.VIDEO).displayName) {
            generative.generateVideo(p, currentAssists())
        }
    }

    fun cancel() {
        forceStop(showStopped = false)
    }

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

    private fun startGeneration(
        capability: RunCapability,
        modelLabel: String?,
        block: () -> kotlinx.coroutines.flow.Flow<GenerativeState>,
    ) {
        job?.cancel()
        val epoch = ++generationEpoch
        _preflightMessage.value = null
        _state.value = GenerativeState.Preparing("Starting…")
        val builder = runDiagnostics?.startRun(
            capability = capability,
            tier = EngineTier.CLOUD,
            modelId = null,
            modelLabel = modelLabel,
            deviceRamMb = deviceRamMb,
        )
        job = viewModelScope.launch {
            try {
                block().collect { next ->
                    if (epoch != generationEpoch) return@collect
                    _state.value = next
                    when (next) {
                        is GenerativeState.Running -> builder?.stage(next.stage, 0)
                        is GenerativeState.ImageReady -> {
                            _lastUsedProviderId.value = next.providerId
                            ingestCreateImage(next.path, label = "Create")
                            builder?.complete(success = true, note = next.providerId)
                        }
                        is GenerativeState.VideoReady -> {
                            _lastUsedProviderId.value = next.providerId
                            ingestCreateImage(next.path, label = "Video")
                            builder?.complete(success = true, note = next.providerId)
                        }
                        is GenerativeState.CodeReady -> {
                            _lastUsedProviderId.value = next.providerId
                            builder?.complete(
                                success = true,
                                note = "${next.providerId} · ${next.tokensIn}+${next.tokensOut} tokens",
                            )
                        }
                        is GenerativeState.Failed -> builder?.complete(success = false, error = next.message)
                        else -> Unit
                    }
                }
            } catch (_: CancellationException) {
                // Expected on force stop / clear
            } catch (e: Exception) {
                if (epoch == generationEpoch) {
                    val msg = e.message?.take(280)?.ifBlank { null } ?: "Generation failed. Tap Retry."
                    _state.value = GenerativeState.Failed(msg)
                    builder?.complete(success = false, error = msg)
                }
            }
        }
    }

    private fun ingestCreateImage(path: String, label: String) {
        val promptSnippet = _prompt.value.trim().take(80).ifBlank { label.lowercase() }
        runCatching {
            wardrobe.add(
                WardrobeEntry(
                    id = Uuid.random().toString(),
                    createdAtEpochMillis = System.currentTimeMillis(),
                    imagePath = path,
                    garmentUri = "${label.lowercase()}:$promptSnippet",
                    personLabel = label,
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
