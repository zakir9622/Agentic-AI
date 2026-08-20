package com.zakir.vestra.shared.settings

import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudModelProvider
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
        settings.getStringOrNull(KEY_CLOUD_PROVIDER) ?: CloudModelCatalog.defaultId,
    )
    val cloudProviderId: StateFlow<String> = _cloudProviderId

    private val _hfToken = MutableStateFlow(settings.getStringOrNull(KEY_HF_TOKEN))
    val hfToken: StateFlow<String?> = _hfToken

    private val _replicateToken = MutableStateFlow(settings.getStringOrNull(KEY_REPLICATE_TOKEN))
    val replicateToken: StateFlow<String?> = _replicateToken

    private val _falApiKey = MutableStateFlow(settings.getStringOrNull(KEY_FAL_KEY))
    val falApiKey: StateFlow<String?> = _falApiKey

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

    fun setHfToken(token: String?) {
        if (token.isNullOrBlank()) settings.remove(KEY_HF_TOKEN) else settings.putString(KEY_HF_TOKEN, token)
        _hfToken.value = token?.takeIf { it.isNotBlank() }
    }

    fun setReplicateToken(token: String?) {
        if (token.isNullOrBlank()) settings.remove(KEY_REPLICATE_TOKEN) else settings.putString(KEY_REPLICATE_TOKEN, token)
        _replicateToken.value = token?.takeIf { it.isNotBlank() }
    }

    fun setFalApiKey(key: String?) {
        if (key.isNullOrBlank()) settings.remove(KEY_FAL_KEY) else settings.putString(KEY_FAL_KEY, key)
        _falApiKey.value = key?.takeIf { it.isNotBlank() }
    }

    fun selectedCloudProvider(): CloudModelProvider =
        CloudModelCatalog.byId(_cloudProviderId.value) ?: CloudModelCatalog.providers.first()

    fun apiKeyFor(provider: CloudModelProvider): String? = when (provider.platform) {
        com.zakir.vestra.shared.cloud.CloudPlatform.HF_SPACE -> _hfToken.value
        com.zakir.vestra.shared.cloud.CloudPlatform.REPLICATE -> _replicateToken.value
        com.zakir.vestra.shared.cloud.CloudPlatform.FAL -> _falApiKey.value
    }

    /** Optimistic — real connectivity failures surface at generation time. */
    fun networkLikelyAvailable(): Boolean = true

    private fun readTier(): EngineTier =
        settings.getStringOrNull(KEY_TIER)?.let { stored ->
            EngineTier.entries.firstOrNull { it.name == stored }
        } ?: EngineTier.AUTO

    private companion object {
        const val KEY_TIER = "engine_tier"
        const val KEY_CONSENT = "likeness_consent_accepted"
        const val KEY_ONBOARDED = "onboarding_complete"
        const val KEY_CLOUD_PROVIDER = "cloud_provider_id"
        const val KEY_HF_TOKEN = "hf_token"
        const val KEY_REPLICATE_TOKEN = "replicate_token"
        const val KEY_FAL_KEY = "fal_api_key"
    }
}
