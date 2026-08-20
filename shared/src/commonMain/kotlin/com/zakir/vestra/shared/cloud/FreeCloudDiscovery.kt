package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.settings.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Resolves which curated free cloud models the user can run with current keys,
 * and optionally discovers warm HF Inference models when an HF token is present.
 */
class FreeCloudDiscovery(
    private val http: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun curatedUsable(settings: AppSettings, capability: AiCapability): List<CloudModelProvider> =
        CloudModelCatalog.forCapability(capability).filter { provider ->
            when {
                !provider.freeTier -> false
                !provider.requiresApiKey -> true
                else -> !settings.apiKeyFor(provider).isNullOrBlank()
            }
        }

    fun curatedLocked(settings: AppSettings, capability: AiCapability): List<CloudModelProvider> =
        CloudModelCatalog.forCapability(capability).filter { provider ->
            provider.freeTier && provider.requiresApiKey && settings.apiKeyFor(provider).isNullOrBlank()
        }

    /**
     * Best-effort discovery of free HF Inference endpoints matching [capability].
     * Failures return empty — curated catalog remains the source of truth.
     */
    suspend fun discoverHf(token: String?, capability: AiCapability): List<CloudModelProvider> {
        if (token.isNullOrBlank()) return emptyList()
        val pipeline = when (capability) {
            AiCapability.IMAGE_GEN -> "text-to-image"
            AiCapability.IMAGE_EDIT -> "image-to-image"
            AiCapability.CODE -> "text-generation"
            AiCapability.TRY_ON, AiCapability.VIDEO -> return emptyList()
        }
        val url =
            "https://huggingface.co/api/models?pipeline_tag=$pipeline&inference=warm&sort=downloads&direction=-1&limit=12"
        return runCatching {
            val response = http.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            check(response.status.isSuccess())
            val models = json.decodeFromString<List<HfModelHit>>(response.bodyAsText())
            models.mapNotNull { hit ->
                val id = hit.id ?: return@mapNotNull null
                CloudModelProvider(
                    id = "hf-disc-$id",
                    displayName = id.substringAfter('/'),
                    description = "Discovered free HF Inference model · $id",
                    platform = CloudPlatform.HF_INFERENCE,
                    capability = capability,
                    endpoint = id,
                    apiName = "inference",
                    license = hit.cardData?.license ?: "Check model card",
                    requiresApiKey = true,
                    freeTier = true,
                    qualityScore = 70,
                    speedScore = 70,
                    usageNote = "Auto-listed from your HF token (warm free Inference).",
                )
            }
        }.getOrDefault(emptyList())
    }
}

@Serializable
private data class HfModelHit(
    val id: String? = null,
    @SerialName("cardData") val cardData: HfCardData? = null,
)

@Serializable
private data class HfCardData(
    val license: String? = null,
)
