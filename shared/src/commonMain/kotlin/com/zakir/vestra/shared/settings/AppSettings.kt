package com.zakir.vestra.shared.settings

import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudModelContracts
import com.zakir.vestra.shared.cloud.CloudModelProvider
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.cloud.ModelHealthTracker
import com.zakir.vestra.shared.cloud.ModelSupportLevel
import com.zakir.vestra.shared.cloud.requiresSpace
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.local.LocalModelCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppSettings(private val settings: Settings) {

    val modelHealth = ModelHealthTracker(settings)

    private val _engineTier = MutableStateFlow(readTier())
    val engineTier: StateFlow<EngineTier> = _engineTier

    private val _appearanceMode = MutableStateFlow(readAppearance())
    val appearanceMode: StateFlow<AppearanceMode> = _appearanceMode

    private val _likenessConsentAccepted = MutableStateFlow(settings.getBoolean(KEY_CONSENT, false))
    val likenessConsentAccepted: StateFlow<Boolean> = _likenessConsentAccepted


    private val _onboardingComplete = MutableStateFlow(settings.getBoolean(KEY_ONBOARDED, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    /**
     * When true, LiteRT-LM may use the GPU backend for Gemma 4 / vision / audio. Default true —
     * `LiteRtLmEngine` falls back to CPU automatically if the GPU delegate fails to initialize
     * (a real failure mode confirmed via a Pixel 9 device log; see `LiteRtLmEngine.kt`), so
     * trying GPU first is safe: it either loads faster, or transparently degrades to the same
     * CPU path this always had. Not removable via Settings — the fallback makes an off switch
     * unnecessary; NNAPI below is the one backend flag that stays opt-in, since its failure mode
     * bypasses that fallback entirely.
     */
    private val _preferLiteRtLmGpu = MutableStateFlow(settings.getBoolean(KEY_PREFER_LITERT_GPU, true))
    val preferLiteRtLmGpu: StateFlow<Boolean> = _preferLiteRtLmGpu

    /**
     * When true (and [preferLiteRtLmGpu] is also on), LiteRT-LM tries the NPU backend before
     * GPU, falling back to GPU then CPU on failure — same defensive pattern as [preferLiteRtLmGpu]
     * itself. Default true: unverified on any specific device in this codebase's dev/test loop,
     * but the fallback chain means an unsupported NPU just costs one failed attempt before
     * landing on GPU/CPU exactly as before, never a hang or crash.
     */
    private val _preferLiteRtLmNpu = MutableStateFlow(settings.getBoolean(KEY_PREFER_LITERT_NPU, true))
    val preferLiteRtLmNpu: StateFlow<Boolean> = _preferLiteRtLmNpu

    /**
     * When true, sets LiteRT-LM's `ExperimentalFlags.enableSpeculativeDecoding` before a GPU or
     * NPU engine initializes (never for CPU), for faster decode. Default true: this is an
     * SDK-marked `@RequiresOptIn` flag Google's own docs call "experimental and temporary," but
     * it only ever changes decode speed, not correctness, and [LiteRtLmEngine]'s init lock
     * already closed the one real bug found here (a data race on this global SDK flag).
     */
    private val _preferSpeculativeDecoding =
        MutableStateFlow(settings.getBoolean(KEY_PREFER_SPECULATIVE_DECODING, true))
    val preferSpeculativeDecoding: StateFlow<Boolean> = _preferSpeculativeDecoding

    /**
     * Master switch for cloud generation, app-wide. Default false — local-only until the
     * user explicitly opts in. When off, [preflight] blocks any capability whose selected
     * provider isn't a local generator, regardless of network/API-key state.
     */
    private val _cloudModelsEnabled = MutableStateFlow(settings.getBoolean(KEY_CLOUD_MODELS_ENABLED, false))
    val cloudModelsEnabled: StateFlow<Boolean> = _cloudModelsEnabled

    /**
     * Prompt-level safety preset (see `com.zakir.vestra.shared.safety.SafetyPresets`) — its
     * guard clause is appended to the prompt before generation. Defaults to "standard", not
     * "off", matching lookbookweb's own default.
     */
    private val _safetyPresetId = MutableStateFlow(
        settings.getString(KEY_SAFETY_PRESET, com.zakir.vestra.shared.safety.SafetyPresets.DEFAULT_ID),
    )
    val safetyPresetId: StateFlow<String> = _safetyPresetId

    /**
     * Whether image/video generation may run a local offline-vision pass over the reference
     * image before generating (see `GenerativeAssists.analyzeReference`). Previously a per-
     * session studio toggle; moved here (alongside the safety preset, its closest analog) so it
     * survives app restarts and isn't lost when its studio-side UI was removed. Default false —
     * it costs a real on-device inference pass and requires a downloaded vision-capable pack.
     */
    private val _analyzeReferenceEnabled = MutableStateFlow(settings.getBoolean(KEY_ANALYZE_REFERENCE, false))
    val analyzeReferenceEnabled: StateFlow<Boolean> = _analyzeReferenceEnabled

    /**
     * Whether News/Chat extracts durable facts from conversation turns via the local chat
     * model and re-injects them into future system prompts (Part B.1). Defaults on — the
     * extraction call and its storage never leave the device, matching this app's local-first
     * stance — but the user can turn it off from the memory panel in Settings.
     */
    private val _memoryEnabled = MutableStateFlow(settings.getBoolean(KEY_MEMORY_ENABLED, true))
    val memoryEnabled: StateFlow<Boolean> = _memoryEnabled

    /**
     * When true, ONNX Runtime may attach NNAPI. Default false, and stays opt-in unlike the
     * LiteRT-LM backend flags above — NNAPI session create has been observed to SIGSEGV/OOM the
     * whole process on Pixel 9 during lite pack load/verify (see `OrtEpPolicy.kt`), a failure
     * mode that bypasses the try/catch fallback pattern entirely rather than degrading gracefully.
     */
    private val _preferNnapi = MutableStateFlow(settings.getBoolean(KEY_PREFER_NNAPI, false))
    val preferNnapi: StateFlow<Boolean> = _preferNnapi

    private val _cloudProviderId = MutableStateFlow(migrateProviderId(KEY_CLOUD_PROVIDER, AiCapability.TRY_ON))
    val cloudProviderId: StateFlow<String> = _cloudProviderId

    private val _imageGenProviderId = MutableStateFlow(migrateProviderId(KEY_IMAGE_GEN, AiCapability.IMAGE_GEN))
    val imageGenProviderId: StateFlow<String> = _imageGenProviderId

    private val _imageEditProviderId = MutableStateFlow(migrateProviderId(KEY_IMAGE_EDIT, AiCapability.IMAGE_EDIT))
    val imageEditProviderId: StateFlow<String> = _imageEditProviderId

    private val _codeProviderId = MutableStateFlow(migrateProviderId(KEY_CODE, AiCapability.CODE))
    val codeProviderId: StateFlow<String> = _codeProviderId

    private val _videoProviderId = MutableStateFlow(migrateProviderId(KEY_VIDEO, AiCapability.VIDEO))
    val videoProviderId: StateFlow<String> = _videoProviderId

    private val _audioProviderId = MutableStateFlow(migrateProviderId(KEY_AUDIO, AiCapability.AUDIO))
    val audioProviderId: StateFlow<String> = _audioProviderId

    private val _hfToken = MutableStateFlow(settings.getStringOrNull(KEY_HF_TOKEN))
    val hfToken: StateFlow<String?> = _hfToken

    private val _groqApiKey = MutableStateFlow(settings.getStringOrNull(KEY_GROQ_KEY))
    val groqApiKey: StateFlow<String?> = _groqApiKey

    private val _openRouterApiKey = MutableStateFlow(settings.getStringOrNull(KEY_OPENROUTER_KEY))
    val openRouterApiKey: StateFlow<String?> = _openRouterApiKey

    /** Injected by Android; defaults optimistic for unit tests. */
    var networkProbe: () -> Boolean = { true }

    fun setEngineTier(tier: EngineTier) {
        settings.putString(KEY_TIER, tier.name)
        _engineTier.value = tier
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        settings.putString(KEY_APPEARANCE, mode.name)
        _appearanceMode.value = mode
    }

    fun setPreferNnapi(enabled: Boolean) {
        settings.putBoolean(KEY_PREFER_NNAPI, enabled)
        _preferNnapi.value = enabled
    }

    fun setPreferLiteRtLmGpu(enabled: Boolean) {
        settings.putBoolean(KEY_PREFER_LITERT_GPU, enabled)
        _preferLiteRtLmGpu.value = enabled
    }

    fun setPreferLiteRtLmNpu(enabled: Boolean) {
        settings.putBoolean(KEY_PREFER_LITERT_NPU, enabled)
        _preferLiteRtLmNpu.value = enabled
    }

    fun setPreferSpeculativeDecoding(enabled: Boolean) {
        settings.putBoolean(KEY_PREFER_SPECULATIVE_DECODING, enabled)
        _preferSpeculativeDecoding.value = enabled
    }

    fun setCloudModelsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_CLOUD_MODELS_ENABLED, enabled)
        _cloudModelsEnabled.value = enabled
    }

    fun setSafetyPresetId(id: String) {
        settings.putString(KEY_SAFETY_PRESET, id)
        _safetyPresetId.value = id
    }

    fun setAnalyzeReferenceEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_ANALYZE_REFERENCE, enabled)
        _analyzeReferenceEnabled.value = enabled
    }

    fun setMemoryEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_MEMORY_ENABLED, enabled)
        _memoryEnabled.value = enabled
    }

    fun clearApiTokens() {
        setHfToken(null)
        setGroqApiKey(null)
        setOpenRouterApiKey(null)
    }

    fun setLikenessConsentAccepted() {
        settings.putBoolean(KEY_CONSENT, true)
        _likenessConsentAccepted.value = true
    }

    fun setOnboardingComplete() {
        settings.putBoolean(KEY_ONBOARDED, true)
        _onboardingComplete.value = true
    }

    fun setCloudProvider(id: String) = setProvider(KEY_CLOUD_PROVIDER, id, AiCapability.TRY_ON, _cloudProviderId)
    fun setImageGenProvider(id: String) = setProvider(KEY_IMAGE_GEN, id, AiCapability.IMAGE_GEN, _imageGenProviderId)
    fun setImageEditProvider(id: String) = setProvider(KEY_IMAGE_EDIT, id, AiCapability.IMAGE_EDIT, _imageEditProviderId)
    fun setCodeProvider(id: String) = setProvider(KEY_CODE, id, AiCapability.CODE, _codeProviderId)
    fun setVideoProvider(id: String) = setProvider(KEY_VIDEO, id, AiCapability.VIDEO, _videoProviderId)
    fun setAudioProvider(id: String) = setProvider(KEY_AUDIO, id, AiCapability.AUDIO, _audioProviderId)

    fun setHfToken(token: String?) {
        // Never silently rewrite the user's model selection (finding H).
        putSecret(KEY_HF_TOKEN, token, _hfToken)
    }
    fun setGroqApiKey(key: String?) = putSecret(KEY_GROQ_KEY, key, _groqApiKey)
    fun setOpenRouterApiKey(key: String?) = putSecret(KEY_OPENROUTER_KEY, key, _openRouterApiKey)

    private val _discoveredProviders = MutableStateFlow<List<CloudModelProvider>>(emptyList())
    val discoveredProviders: StateFlow<List<CloudModelProvider>> = _discoveredProviders

    fun rememberDiscovered(providers: List<CloudModelProvider>) {
        if (providers.isEmpty()) return
        _discoveredProviders.value =
            (_discoveredProviders.value + providers.filter { it.freeTier }).distinctBy { it.id }
    }

    fun resolveProvider(id: String, capability: AiCapability): CloudModelProvider =
        CloudModelCatalog.byId(id)
            ?.takeIf { it.usableFor(capability) }
            ?: _discoveredProviders.value.firstOrNull { it.id == id && it.usableFor(capability) }
            ?: CloudModelCatalog.defaultFor(capability)

    /**
     * Visual capabilities run through the Gradio Space client, so an HF Inference model can
     * never satisfy them — selecting one only produces "Only free Hugging Face Spaces are
     * supported for images" at generation time.
     */
    private fun CloudModelProvider.usableFor(capability: AiCapability): Boolean =
        this.capability == capability &&
            freeTier &&
            estCostUsd <= 0.0 &&
            CloudModelContracts.forProvider(this).support != ModelSupportLevel.UNSUPPORTED &&
            when {
                !capability.requiresSpace() -> true
                platform == CloudPlatform.HF_SPACE || platform == CloudPlatform.HF_INFERENCE -> true
                else -> false
            }

    fun selectedCloudProvider(): CloudModelProvider =
        resolveProvider(_cloudProviderId.value, AiCapability.TRY_ON)

    fun selectedProvider(capability: AiCapability): CloudModelProvider {
        val id = selectionId(capability)
        if (LocalModelCatalog.isSelectableStudioId(id, capability)) {
            // Local route selected — cloud default is only used if local fails / for estimates.
            return CloudModelCatalog.defaultFor(capability)
        }
        // Legacy auto-listed HF "warm" text models are not Inference Providers chat routes.
        if (capability == AiCapability.CODE && id.startsWith("hf-disc-")) {
            val curated = if (!_hfToken.value.isNullOrBlank()) {
                CloudModelCatalog.byId("qwen25-coder-hf") ?: CloudModelCatalog.defaultFor(capability)
            } else {
                CloudModelCatalog.defaultFor(capability)
            }
            if (_codeProviderId.value != curated.id) {
                settings.putString(KEY_CODE, curated.id)
                _codeProviderId.value = curated.id
            }
            return curated
        }
        val resolved = resolveProvider(id, capability)
        // Persist the correction so a stale Inference stub cannot come back on the next launch.
        if (resolved.id != id) {
            keyFor(capability)?.let { key -> settings.putString(key, resolved.id) }
            flowFor(capability)?.let { flow -> flow.value = resolved.id }
        }
        return resolved
    }

    /** Raw stored selection id (cloud provider or local catalog id). */
    fun selectionId(capability: AiCapability): String = when (capability) {
        AiCapability.TRY_ON -> _cloudProviderId.value
        AiCapability.IMAGE_GEN -> _imageGenProviderId.value
        AiCapability.IMAGE_EDIT -> _imageEditProviderId.value
        AiCapability.CODE -> _codeProviderId.value
        AiCapability.VIDEO -> _videoProviderId.value
        AiCapability.AUDIO -> _audioProviderId.value
    }

    /** True when the user explicitly picked an on-device studio generator. */
    fun prefersLocal(capability: AiCapability): Boolean =
        LocalModelCatalog.isSelectableStudioId(selectionId(capability), capability)

    fun setLocalGenerator(capability: AiCapability, localId: String) {
        require(LocalModelCatalog.isSelectableStudioId(localId, capability)) {
            "Not a selectable local generator: $localId for $capability"
        }
        val key = keyFor(capability) ?: return
        val flow = flowFor(capability) ?: return
        settings.putString(key, localId)
        flow.value = localId
    }

    private fun keyFor(capability: AiCapability): String? = when (capability) {
        AiCapability.TRY_ON -> KEY_CLOUD_PROVIDER
        AiCapability.IMAGE_GEN -> KEY_IMAGE_GEN
        AiCapability.IMAGE_EDIT -> KEY_IMAGE_EDIT
        AiCapability.CODE -> KEY_CODE
        AiCapability.VIDEO -> KEY_VIDEO
        AiCapability.AUDIO -> KEY_AUDIO
    }

    private fun flowFor(capability: AiCapability): MutableStateFlow<String>? = when (capability) {
        AiCapability.TRY_ON -> _cloudProviderId
        AiCapability.IMAGE_GEN -> _imageGenProviderId
        AiCapability.IMAGE_EDIT -> _imageEditProviderId
        AiCapability.CODE -> _codeProviderId
        AiCapability.VIDEO -> _videoProviderId
        AiCapability.AUDIO -> _audioProviderId
    }

    fun apiKeyFor(provider: CloudModelProvider): String? = when (provider.platform) {
        CloudPlatform.HF_SPACE, CloudPlatform.HF_INFERENCE -> _hfToken.value
        CloudPlatform.GROQ -> _groqApiKey.value
        CloudPlatform.OPENROUTER -> _openRouterApiKey.value
    }

    fun networkLikelyAvailable(): Boolean = networkProbe()

    /**
     * Whether any network generation call is permitted at all.
     *
     * [preflight] is only a pre-check and deliberately lets a local selection through, so it
     * cannot be the sole gate: a local pack that fails at runtime would otherwise fall back to
     * cloud with the toggle off. Every code path that is about to reach the network must consult
     * this immediately before doing so.
     */
    fun cloudGenerationAllowed(): Boolean = _cloudModelsEnabled.value

    /** User-facing reason for a blocked cloud call, shared by preflight and the runtime gates. */
    fun cloudDisabledReason(capability: AiCapability): String {
        val label = capability.name.lowercase().replace('_', ' ')
        return "Cloud models are off — enable them in Settings to use $label via cloud, " +
            "or pick a local model instead."
    }

    fun preflight(capability: AiCapability): PreflightResult {
        if (prefersLocal(capability)) {
            // Allowed through so a local pick can start; if the local engine then fails, the
            // runtime gate in GenerativeCloudService — not this function — stops the cloud
            // fallback. Do not treat this early return as "cloud is permitted".
            return PreflightResult.Ok(selectedProvider(capability))
        }
        if (!_cloudModelsEnabled.value) {
            return PreflightResult.Blocked(cloudDisabledReason(capability))
        }
        val provider = selectedProvider(capability)
        // Do not hard-block on ConnectivityManager — it often lags 5G/Wi‑Fi and caused
        // false "No internet" while the status bar showed signal. Generation attempts
        // the HTTP call; CloudFailureClassifier maps real DNS failures.
        if (provider.requiresApiKey && apiKeyFor(provider).isNullOrBlank()) {
            return PreflightResult.Blocked(
                "Add a free ${provider.platform.name} API key in Settings to use ${provider.displayName}.",
            )
        }
        CloudModelContracts.preflightOrNull(provider)?.let { hint ->
            return PreflightResult.Blocked(hint)
        }
        return PreflightResult.Ok(provider)
    }

    private fun setProvider(
        key: String,
        id: String,
        capability: AiCapability,
        flow: MutableStateFlow<String>,
    ) {
        if (LocalModelCatalog.isSelectableStudioId(id, capability)) {
            settings.putString(key, id)
            flow.value = id
            return
        }
        val resolved = resolveProvider(id, capability)
        settings.putString(key, resolved.id)
        flow.value = resolved.id
    }

    private fun migrateProviderId(key: String, capability: AiCapability): String {
        val stored = settings.getStringOrNull(key)
        if (stored != null && LocalModelCatalog.isSelectableStudioId(stored, capability)) {
            return stored
        }
        // One-time: InstructPix2Pix was the default edit model but its Space often returns
        // empty Gradio errors — prefer Qwen Image Edit unless the user re-selects it later.
        if (capability == AiCapability.IMAGE_EDIT &&
            (stored == "instruct-pix2pix-hf" || stored == "instruct-pix2pix-inference")
        ) {
            val curated = CloudModelCatalog.defaultFor(capability)
            settings.putString(key, curated.id)
            return curated.id
        }
        // Prefer free Image Spaces — HF Inference monthly credits deplete fast.
        // Force off broken SDXL Lightning; leave an explicit Inference selection alone.
        if (capability == AiCapability.IMAGE_GEN) {
            when (stored) {
                null, "sdxl-lightning-hf" -> {
                    settings.putString(key, "flux-schnell-hf")
                    return "flux-schnell-hf"
                }
            }
        }
        if (capability == AiCapability.CODE && stored == "deepseek-r1-free-or") {
            settings.putString(key, "openrouter-free")
            return "openrouter-free"
        }
        // Prefer Edge-TTS as default — Kokoro ZeroGPU often queues past the audio budget.
        if (capability == AiCapability.AUDIO && (stored == null || stored == "mms-tts-eng-hf")) {
            val curated = CloudModelCatalog.defaultFor(capability)
            settings.putString(key, curated.id)
            return curated.id
        }
        if (capability == AiCapability.CODE && stored == "llama33-70b-groq" &&
            settings.getStringOrNull(KEY_GROQ_KEY).isNullOrBlank()
        ) {
            val fallback = when {
                !settings.getStringOrNull(KEY_HF_TOKEN).isNullOrBlank() -> "qwen25-coder-hf"
                !settings.getStringOrNull(KEY_OPENROUTER_KEY).isNullOrBlank() -> "openrouter-free"
                else -> null
            }
            if (fallback != null && fallback != stored) {
                settings.putString(key, fallback)
                return fallback
            }
        }
        val resolved = stored?.let { CloudModelCatalog.byId(it) }
            ?.takeIf { it.usableFor(capability) }
            ?: CloudModelCatalog.defaultFor(capability)
        if (stored != resolved.id) settings.putString(key, resolved.id)
        // Drop legacy paid keys if present
        settings.remove(KEY_REPLICATE_TOKEN)
        settings.remove(KEY_FAL_KEY)
        return resolved.id
    }

    private fun putSecret(key: String, value: String?, flow: MutableStateFlow<String?>) {
        if (value.isNullOrBlank()) settings.remove(key) else settings.putString(key, value)
        flow.value = value?.takeIf { it.isNotBlank() }
    }

    private fun readTier(): EngineTier =
        settings.getStringOrNull(KEY_TIER)?.let { stored ->
            EngineTier.entries.firstOrNull { it.name == stored }
        } ?: EngineTier.AUTO

    private fun readAppearance(): AppearanceMode =
        settings.getStringOrNull(KEY_APPEARANCE)?.let { stored ->
            AppearanceMode.entries.firstOrNull { it.name == stored }
        } ?: AppearanceMode.SYSTEM

    private companion object {
        const val KEY_TIER = "engine_tier"
        const val KEY_APPEARANCE = "appearance_mode"
        const val KEY_CONSENT = "likeness_consent_accepted"
        const val KEY_ONBOARDED = "onboarding_complete"
        const val KEY_CLOUD_PROVIDER = "cloud_provider_id"
        const val KEY_IMAGE_GEN = "image_gen_provider_id"
        const val KEY_IMAGE_EDIT = "image_edit_provider_id"
        const val KEY_CODE = "code_provider_id"
        const val KEY_VIDEO = "video_provider_id"
        const val KEY_AUDIO = "audio_provider_id"
        const val KEY_HF_TOKEN = "hf_token"
        const val KEY_GROQ_KEY = "groq_api_key"
        const val KEY_OPENROUTER_KEY = "openrouter_api_key"
        const val KEY_REPLICATE_TOKEN = "replicate_token"
        const val KEY_FAL_KEY = "fal_api_key"
        const val KEY_PREFER_NNAPI = "prefer_nnapi"
        const val KEY_PREFER_LITERT_GPU = "prefer_litert_gpu"
        const val KEY_PREFER_LITERT_NPU = "prefer_litert_npu"
        const val KEY_PREFER_SPECULATIVE_DECODING = "prefer_litert_speculative_decoding"
        const val KEY_CLOUD_MODELS_ENABLED = "cloud_models_enabled"
        const val KEY_SAFETY_PRESET = "safety_preset_id"
        const val KEY_ANALYZE_REFERENCE = "analyze_reference_enabled"
        const val KEY_MEMORY_ENABLED = "chat_memory_enabled"
    }
}

sealed interface PreflightResult {
    data class Ok(val provider: CloudModelProvider) : PreflightResult
    data class Blocked(val reason: String) : PreflightResult
}
