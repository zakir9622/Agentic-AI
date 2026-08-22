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

    /** Rolling live console lines for the current generation (newest last). */
    private val _liveLog = MutableStateFlow<List<String>>(emptyList())
    val liveLog: StateFlow<List<String>> = _liveLog

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

    private val _bypassFilter = MutableStateFlow(true)
    val bypassFilter: StateFlow<Boolean> = _bypassFilter

    private val _qualityGuard = MutableStateFlow(true)
    val qualityGuard: StateFlow<Boolean> = _qualityGuard

    private val _inferenceSteps = MutableStateFlow(22)
    val inferenceSteps: StateFlow<Int> = _inferenceSteps

    private val _guidanceScale = MutableStateFlow(7.0f)
    val guidanceScale: StateFlow<Float> = _guidanceScale

    private val _seed = MutableStateFlow<Long?>(null)
    val seed: StateFlow<Long?> = _seed

    private val _voicePersonaId = MutableStateFlow(com.zakir.vestra.shared.audio.VoiceCatalog.defaultId)
    val voicePersonaId: StateFlow<String> = _voicePersonaId

    private val _voiceKnobs = MutableStateFlow(com.zakir.vestra.shared.audio.VoiceKnobs.Default)
    val voiceKnobs: StateFlow<com.zakir.vestra.shared.audio.VoiceKnobs> = _voiceKnobs

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

    fun setInferenceSteps(value: Int) {
        _inferenceSteps.value = value.coerceIn(4, 50)
    }

    fun setGuidanceScale(value: Float) {
        _guidanceScale.value = value.coerceIn(1f, 15f)
    }

    fun setSeed(value: Long?) {
        _seed.value = value?.coerceAtLeast(0L)
    }

    fun currentAssists(): GenerativeAssists = GenerativeAssists(
        pragmatic = _pragmaticMode.value,
        creative = _creativeMode.value,
        fashionContext = _fashionContext.value,
        detailBoost = _detailBoost.value,
        bypassFilter = _bypassFilter.value,
        qualityGuard = _qualityGuard.value,
        inferenceSteps = _inferenceSteps.value.takeIf { it != 22 },
        guidanceScale = _guidanceScale.value.takeIf { it != 7.0f },
        seed = _seed.value,
    )

    fun prepareStudio(resetIfIdle: Boolean = true) {
        if (!resetIfIdle || isBusy) return
        _state.value = null
        _liveLog.value = emptyList()
        _preflightMessage.value = null
        _prompt.value = ""
        _referenceUri.value = null
    }

    fun preflightLabel(capability: AiCapability): String? {
        return when (val check = appSettings.preflight(capability)) {
            is PreflightResult.Blocked -> check.reason
            is PreflightResult.Ok -> "${check.provider.displayName} · ${CloudModelContracts.liveStatusLabel(check.provider, appSettings.modelHealth)}"
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

    fun setVoicePersona(id: String) {
        _voicePersonaId.value = id
    }

    fun setVoiceKnobs(knobs: com.zakir.vestra.shared.audio.VoiceKnobs) {
        _voiceKnobs.value = knobs.sanitized()
    }

    fun generateAudio() {
        val p = sanitizePrompt(_prompt.value)
        if (p.isEmpty()) {
            _preflightMessage.value = "Enter text to speak, or record audio and tap Apply voice change."
            return
        }
        _prompt.value = p
        when (val check = appSettings.preflight(AiCapability.AUDIO)) {
            is PreflightResult.Blocked -> {
                // Allow offline voice-changer when reference audio is set.
                if (_referenceUri.value == null || !p.equals("voice-change", ignoreCase = true)) {
                    _preflightMessage.value = check.reason
                    return
                }
            }
            is PreflightResult.Ok -> Unit
        }
        val persona = com.zakir.vestra.shared.audio.VoiceCatalog.byId(_voicePersonaId.value)
        startGeneration(RunCapability.AUDIO, appSettings.selectedProvider(AiCapability.AUDIO).displayName) {
            generative.generateAudio(
                prompt = p,
                persona = persona,
                knobs = _voiceKnobs.value,
                referenceAudioUri = _referenceUri.value,
            )
        }
    }

    /** Offline path: apply local DSP knobs to a recorded / attached WAV clip. */
    fun applyVoiceChange() {
        val clip = _referenceUri.value
        if (clip.isNullOrBlank()) {
            _preflightMessage.value = "Record or attach audio first, then apply voice knobs."
            return
        }
        _prompt.value = "voice-change"
        _preflightMessage.value = null
        startGeneration(RunCapability.AUDIO, "Local voice changer") {
            generative.generateAudio(
                prompt = "voice-change",
                persona = com.zakir.vestra.shared.audio.VoiceCatalog.byId(_voicePersonaId.value),
                knobs = _voiceKnobs.value,
                referenceAudioUri = clip,
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
        appendLive("Stopped by user")
        _state.value = if (showStopped) {
            GenerativeState.Failed("Stopped. Tap Generate to run again.")
        } else {
            null
        }
    }

    fun clearResult() {
        forceStop(showStopped = false)
        _liveLog.value = emptyList()
        _preflightMessage.value = null
    }

    private fun appendLive(line: String) {
        val stamped = line.take(160)
        _liveLog.value = (_liveLog.value + stamped).takeLast(40)
        runCatching {
            com.zakir.vestra.diagnostics.CrashReporter.i("Gen", stamped)
        }
    }

    private fun startGeneration(
        capability: RunCapability,
        modelLabel: String?,
        block: () -> kotlinx.coroutines.flow.Flow<GenerativeState>,
    ) {
        job?.cancel()
        val epoch = ++generationEpoch
        _preflightMessage.value = null
        _liveLog.value = emptyList()
        _state.value = GenerativeState.Preparing("Starting…")
        appendLive("Start · ${capability.name} · ${modelLabel ?: "model"}")
        val builder = runDiagnostics?.startRun(
            capability = capability,
            tier = EngineTier.CLOUD,
            modelId = null,
            modelLabel = modelLabel,
            deviceRamMb = deviceRamMb,
        )
        job = viewModelScope.launch {
            var lastStageAt = System.currentTimeMillis()
            try {
                block().collect { next ->
                    if (epoch != generationEpoch) return@collect
                    _state.value = next
                    when (next) {
                        is GenerativeState.Preparing -> appendLive(next.message)
                        is GenerativeState.Running -> {
                            appendLive(next.stage)
                            val now = System.currentTimeMillis()
                            builder?.stage(next.stage, now - lastStageAt)
                            lastStageAt = now
                        }
                        is GenerativeState.ImageReady -> {
                            appendLive("Image ready")
                            _lastUsedProviderId.value = next.providerId
                            ingestCreateImage(next.path, label = "Create")
                            builder?.complete(success = true, note = next.providerId)
                        }
                        is GenerativeState.VideoReady -> {
                            appendLive("Video ready")
                            _lastUsedProviderId.value = next.providerId
                            ingestCreateImage(next.path, label = "Video")
                            builder?.complete(success = true, note = next.providerId)
                        }
                        is GenerativeState.AudioReady -> {
                            appendLive("Audio ready")
                            _lastUsedProviderId.value = next.providerId
                            builder?.complete(success = true, note = next.providerId)
                        }
                        is GenerativeState.CodeReady -> {
                            appendLive("Code ready · ${next.tokensIn}+${next.tokensOut} tokens")
                            _lastUsedProviderId.value = next.providerId
                            builder?.complete(
                                success = true,
                                note = "${next.providerId} · ${next.tokensIn}+${next.tokensOut} tokens",
                            )
                        }
                        is GenerativeState.Failed -> {
                            appendLive("Failed · ${next.message.take(120)}")
                            builder?.complete(success = false, error = next.message)
                        }
                    }
                }
            } catch (_: CancellationException) {
                // Expected on force stop / clear
            } catch (e: Exception) {
                if (epoch == generationEpoch) {
                    val msg = e.message?.take(280)?.ifBlank { null } ?: "Generation failed. Tap Retry."
                    appendLive("Error · $msg")
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
