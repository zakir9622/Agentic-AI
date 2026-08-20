package com.zakir.vestra.shared.settings

import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudModelProvider
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.domain.EngineTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettings(private val settings: Settings) {

    private val _engineTier = MutableStateFlow(readTier())
    val engineTier: StateFlow<EngineTier> = _engineTier

    private val _likenessConsentAccepted = MutableStateFlow(settings.getBoolean(KEY_CONSENT, false))
    val likenessConsentAccepted: StateFlow<Boolean> = _likenessConsentAccepted

    private val _onboardingComplete = MutableStateFlow(settings.getBoolean(KEY_ONBOARDED, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    private val _cloudProviderId = MutableStateFlow(
        settings.getStringOrNull(KEY_CLOUD_PROVIDER) ?: CloudModelCatalog.defaultTryOnId,
    )
    val cloudProviderId: StateFlow<String> = _cloudProviderId

    private val _imageGenProviderId = MutableStateFlow(
        settings.getStringOrNull(KEY_IMAGE_GEN) ?: CloudModelCatalog.defaultImageGenId,
    )
    val imageGenProviderId: StateFlow<String> = _imageGenProviderId

    private val _imageEditProviderId = MutableStateFlow(
        settings.getStringOrNull(KEY_IMAGE_EDIT) ?: CloudModelCatalog.defaultImageEditId,
    )
    val imageEditProviderId: StateFlow<String> = _imageEditProviderId

    private val _codeProviderId = MutableStateFlow(
        settings.getStringOrNull(KEY_CODE) ?: CloudModelCatalog.defaultCodeId,
    )
    val codeProviderId: StateFlow<String> = _codeProviderId

    private val _videoProviderId = MutableStateFlow(
        settings.getStringOrNull(KEY_VIDEO) ?: CloudModelCatalog.defaultVideoId,
    )
    val videoProviderId: StateFlow<String> = _videoProviderId

    private val _hfToken = MutableStateFlow(settings.getStringOrNull(KEY_HF_TOKEN))
    val hfToken: StateFlow<String?> = _hfToken

    private val _replicateToken = MutableStateFlow(settings.getStringOrNull(KEY_REPLICATE_TOKEN))
    val replicateToken: StateFlow<String?> = _replicateToken

    private val _falApiKey = MutableStateFlow(settings.getStringOrNull(KEY_FAL_KEY))
    val falApiKey: StateFlow<String?> = _falApiKey

    private val _groqApiKey = MutableStateFlow(settings.getStringOrNull(KEY_GROQ_KEY))
    val groqApiKey: StateFlow<String?> = _groqApiKey

    private val _openRouterApiKey = MutableStateFlow(settings.getStringOrNull(KEY_OPENROUTER_KEY))
    val openRouterApiKey: StateFlow<String?> = _openRouterApiKey

    fun setEngineTier(tier: EngineTier) {
        settings.putString(KEY_TIER, tier.name)
        _engineTier.value = tier
    }

    fun setLikenessConsentAccepted() {
        settings.putBoolean(KEY_CONSENT, true)
        _likenessConsentAccepted.value = true
    }

    fun setOnboardingComplete() {
        settings.putBoolean(KEY_ONBOARDED, true)
        _onboardingComplete.value = true
    }

    fun setCloudProvider(id: String) {
        settings.putString(KEY_CLOUD_PROVIDER, id)
        _cloudProviderId.value = id
    }

    fun setImageGenProvider(id: String) {
        settings.putString(KEY_IMAGE_GEN, id)
        _imageGenProviderId.value = id
    }

    fun setImageEditProvider(id: String) {
        settings.putString(KEY_IMAGE_EDIT, id)
        _imageEditProviderId.value = id
    }

    fun setCodeProvider(id: String) {
        settings.putString(KEY_CODE, id)
        _codeProviderId.value = id
    }

    fun setVideoProvider(id: String) {
        settings.putString(KEY_VIDEO, id)
        _videoProviderId.value = id
    }

    fun setHfToken(token: String?) = putSecret(KEY_HF_TOKEN, token, _hfToken)
    fun setReplicateToken(token: String?) = putSecret(KEY_REPLICATE_TOKEN, token, _replicateToken)
    fun setFalApiKey(key: String?) = putSecret(KEY_FAL_KEY, key, _falApiKey)
    fun setGroqApiKey(key: String?) = putSecret(KEY_GROQ_KEY, key, _groqApiKey)
    fun setOpenRouterApiKey(key: String?) = putSecret(KEY_OPENROUTER_KEY, key, _openRouterApiKey)

    fun selectedCloudProvider(): CloudModelProvider =
        CloudModelCatalog.byId(_cloudProviderId.value)
            ?: CloudModelCatalog.forCapability(AiCapability.TRY_ON).first()

    fun selectedProvider(capability: AiCapability): CloudModelProvider {
        val id = when (capability) {
            AiCapability.TRY_ON -> _cloudProviderId.value
            AiCapability.IMAGE_GEN -> _imageGenProviderId.value
            AiCapability.IMAGE_EDIT -> _imageEditProviderId.value
            AiCapability.CODE -> _codeProviderId.value
            AiCapability.VIDEO -> _videoProviderId.value
        }
        return CloudModelCatalog.byId(id)
            ?: CloudModelCatalog.forCapability(capability).first()
    }

    fun apiKeyFor(provider: CloudModelProvider): String? = when (provider.platform) {
        CloudPlatform.HF_SPACE, CloudPlatform.HF_INFERENCE -> _hfToken.value
        CloudPlatform.REPLICATE -> _replicateToken.value
        CloudPlatform.FAL -> _falApiKey.value
        CloudPlatform.GROQ -> _groqApiKey.value
        CloudPlatform.OPENROUTER -> _openRouterApiKey.value
    }

    fun networkLikelyAvailable(): Boolean = true

    private fun putSecret(key: String, value: String?, flow: MutableStateFlow<String?>) {
        if (value.isNullOrBlank()) settings.remove(key) else settings.putString(key, value)
        flow.value = value?.takeIf { it.isNotBlank() }
    }

    private fun readTier(): EngineTier =
        settings.getStringOrNull(KEY_TIER)?.let { stored ->
            EngineTier.entries.firstOrNull { it.name == stored }
        } ?: EngineTier.AUTO

    private companion object {
        const val KEY_TIER = "engine_tier"
        const val KEY_CONSENT = "likeness_consent_accepted"
        const val KEY_ONBOARDED = "onboarding_complete"
        const val KEY_CLOUD_PROVIDER = "cloud_provider_id"
        const val KEY_IMAGE_GEN = "image_gen_provider_id"
        const val KEY_IMAGE_EDIT = "image_edit_provider_id"
        const val KEY_CODE = "code_provider_id"
        const val KEY_VIDEO = "video_provider_id"
        const val KEY_HF_TOKEN = "hf_token"
        const val KEY_REPLICATE_TOKEN = "replicate_token"
        const val KEY_FAL_KEY = "fal_api_key"
        const val KEY_GROQ_KEY = "groq_api_key"
        const val KEY_OPENROUTER_KEY = "openrouter_api_key"
    }
}
