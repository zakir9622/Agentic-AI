package com.zakir.vestra.shared.settings

import com.russhwolf.settings.Settings
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

    private fun readTier(): EngineTier =
        settings.getStringOrNull(KEY_TIER)?.let { stored ->
            EngineTier.entries.firstOrNull { it.name == stored }
        } ?: EngineTier.AUTO

    private companion object {
        const val KEY_TIER = "engine_tier"
        const val KEY_CONSENT = "likeness_consent_accepted"
        const val KEY_ONBOARDED = "onboarding_complete"
    }
}
