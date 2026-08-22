package com.zakir.vestra.shared.settings

import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudModelContracts
import com.zakir.vestra.shared.cloud.CloudModelProvider
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.cloud.ModelSupportLevel
import com.zakir.vestra.shared.cloud.requiresSpace
import com.zakir.vestra.shared.domain.EngineTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppSettings(private val settings: Settings) {

    private val _engineTier = MutableStateFlow(readTier())
    val engineTier: StateFlow<EngineTier> = _engineTier

    private val _appearanceMode = MutableStateFlow(readAppearance())
    val appearanceMode: StateFlow<AppearanceMode> = _appearanceMode

    private val _likenessConsentAccepted = MutableStateFlow(settings.getBoolean(KEY_CONSENT, false))
    val likenessConsentAccepted: StateFlow<Boolean> = _likenessConsentAccepted


    private val _onboardingComplete = MutableStateFlow(settings.getBoolean(KEY_ONBOARDED, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

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

    fun setHfToken(token: String?) {
        putSecret(KEY_HF_TOKEN, token, _hfToken)
        if (!token.isNullOrBlank()) {
            val imageGen = _imageGenProviderId.value
            if (imageGen == "flux-schnell-hf" || imageGen == "sdxl-lightning-hf") {
                setImageGenProvider("flux-schnell-inference")
            }
        }
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
        val id = when (capability) {
            AiCapability.TRY_ON -> _cloudProviderId.value
            AiCapability.IMAGE_GEN -> _imageGenProviderId.value
            AiCapability.IMAGE_EDIT -> _imageEditProviderId.value
            AiCapability.CODE -> _codeProviderId.value
            AiCapability.VIDEO -> _videoProviderId.value
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

    private fun keyFor(capability: AiCapability): String? = when (capability) {
        AiCapability.TRY_ON -> KEY_CLOUD_PROVIDER
        AiCapability.IMAGE_GEN -> KEY_IMAGE_GEN
        AiCapability.IMAGE_EDIT -> KEY_IMAGE_EDIT
        AiCapability.CODE -> KEY_CODE
        AiCapability.VIDEO -> KEY_VIDEO
    }

    private fun flowFor(capability: AiCapability): MutableStateFlow<String>? = when (capability) {
        AiCapability.TRY_ON -> _cloudProviderId
        AiCapability.IMAGE_GEN -> _imageGenProviderId
        AiCapability.IMAGE_EDIT -> _imageEditProviderId
        AiCapability.CODE -> _codeProviderId
        AiCapability.VIDEO -> _videoProviderId
    }

    fun apiKeyFor(provider: CloudModelProvider): String? = when (provider.platform) {
        CloudPlatform.HF_SPACE, CloudPlatform.HF_INFERENCE -> _hfToken.value
        CloudPlatform.GROQ -> _groqApiKey.value
        CloudPlatform.OPENROUTER -> _openRouterApiKey.value
    }

    fun networkLikelyAvailable(): Boolean = networkProbe()

    fun preflight(capability: AiCapability): PreflightResult {
        val provider = selectedProvider(capability)
        if (!networkLikelyAvailable()) {
            return PreflightResult.Blocked("No internet connection. Local try-on still works offline with Lite/Pro packs.")
        }
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
        val resolved = resolveProvider(id, capability)
        settings.putString(key, resolved.id)
        flow.value = resolved.id
    }

    private fun migrateProviderId(key: String, capability: AiCapability): String {
        val stored = settings.getStringOrNull(key)
        // One-time: InstructPix2Pix was the default edit model but its Space often returns
        // empty Gradio errors — prefer Qwen Image Edit unless the user re-selects it later.
        if (capability == AiCapability.IMAGE_EDIT &&
            (stored == "instruct-pix2pix-hf" || stored == "instruct-pix2pix-inference")
        ) {
            val curated = CloudModelCatalog.defaultFor(capability)
            settings.putString(key, curated.id)
            return curated.id
        }
        // When HF token is configured, prefer Inference Providers for image gen (OpenCode-style).
        if (capability == AiCapability.IMAGE_GEN &&
            !settings.getStringOrNull(KEY_HF_TOKEN).isNullOrBlank()
        ) {
            val stored = settings.getStringOrNull(key)
            if (stored == null || stored == "flux-schnell-hf" || stored == "sdxl-lightning-hf") {
                settings.putString(key, "flux-schnell-inference")
                return "flux-schnell-inference"
            }
        }
        if (capability == AiCapability.CODE && stored == "llama33-70b-groq" &&
            settings.getStringOrNull(KEY_GROQ_KEY).isNullOrBlank()
        ) {
            val fallback = when {
                !settings.getStringOrNull(KEY_HF_TOKEN).isNullOrBlank() -> "qwen25-coder-hf"
                !settings.getStringOrNull(KEY_OPENROUTER_KEY).isNullOrBlank() -> "openrouter-free"
                else -> stored
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
        const val KEY_HF_TOKEN = "hf_token"
        const val KEY_GROQ_KEY = "groq_api_key"
        const val KEY_OPENROUTER_KEY = "openrouter_api_key"
        const val KEY_REPLICATE_TOKEN = "replicate_token"
        const val KEY_FAL_KEY = "fal_api_key"
    }
}

sealed interface PreflightResult {
    data class Ok(val provider: CloudModelProvider) : PreflightResult
    data class Blocked(val reason: String) : PreflightResult
}
