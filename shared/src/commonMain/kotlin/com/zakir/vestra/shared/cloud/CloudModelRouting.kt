package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.settings.AppSettings

/**
 * Orders free models for automatic fallback when the selected Space is busy,
 * out of ZeroGPU quota, or offline.
 */
object CloudModelRouting {

    fun fallbackChain(
        selected: CloudModelProvider,
        capability: AiCapability,
        settings: AppSettings? = null,
    ): List<CloudModelProvider> {
        // Degraded Spaces (503, broken upstream) waste quota seconds when chained
        // automatically — only include them when the user explicitly picked one.
        val allowDegradedAlternates =
            CloudModelContracts.forProvider(selected).support == ModelSupportLevel.DEGRADED
        val alternates = CloudModelCatalog.forCapability(capability)
            .filter { candidate ->
                candidate.id != selected.id &&
                    candidate.platform == CloudPlatform.HF_SPACE &&
                    CloudModelContracts.forProvider(candidate).support != ModelSupportLevel.UNSUPPORTED &&
                    (allowDegradedAlternates ||
                        CloudModelContracts.forProvider(candidate).support != ModelSupportLevel.DEGRADED) &&
                    (settings == null || isUsable(candidate, settings))
            }
            .sortedWith(modelPriority())
        return listOf(selected) + alternates
    }

    fun codeFallbackChain(
        selected: CloudModelProvider,
        settings: AppSettings,
    ): List<CloudModelProvider> {
        val alternates = CloudModelCatalog.forCapability(AiCapability.CODE)
            .filter { candidate ->
                candidate.id != selected.id &&
                    CloudModelContracts.forProvider(candidate).support != ModelSupportLevel.UNSUPPORTED
            }
            .sortedWith(modelPriority().then(compareByDescending { it.speedScore }))
        return (listOf(selected) + alternates)
            .filter { isUsable(it, settings) }
            .distinctBy { it.id }
    }

    private fun isUsable(candidate: CloudModelProvider, settings: AppSettings): Boolean =
        !candidate.requiresApiKey || !settings.apiKeyFor(candidate).isNullOrBlank()

    private fun modelPriority(): Comparator<CloudModelProvider> =
        compareByDescending<CloudModelProvider> { provider ->
            when (CloudModelContracts.forProvider(provider).support) {
                ModelSupportLevel.READY -> 3
                ModelSupportLevel.DEGRADED -> 2
                ModelSupportLevel.UNSUPPORTED -> 0
            }
        }.thenByDescending { it.qualityScore }
}
