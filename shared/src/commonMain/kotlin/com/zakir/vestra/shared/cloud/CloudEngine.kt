package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.domain.TryOnResult
import com.zakir.vestra.shared.domain.effectiveCategory
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.TryOnEngine
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.settings.AppSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Cloud try-on via curated open-source models (HF Spaces, Replicate, FAL).
 * Only runs when the user explicitly selects the Cloud tier — never auto-routed.
 */
class CloudEngine(
    private val http: HttpClient,
    private val io: CloudImageIo,
    private val settings: AppSettings,
) : TryOnEngine {

    private val hf = HfGradioClient(http)
    private val replicate = ReplicateClient(http)
    private val fal = FalClient(http)

    override val tier: EngineTier = EngineTier.CLOUD

    override fun isAvailable(): Availability {
        val provider = settings.selectedCloudProvider()
        return when {
            !settings.networkLikelyAvailable() ->
                Availability.Unavailable(UnavailableReason.OFFLINE)
            provider.requiresApiKey && settings.apiKeyFor(provider).isNullOrBlank() ->
                Availability.Unavailable(UnavailableReason.NOT_CONFIGURED)
            else -> Availability.Ready
        }
    }

    override fun generate(request: TryOnRequest): Flow<GenerationState> = flow {
        val provider = settings.selectedCloudProvider()
        val startedAt = System.currentTimeMillis()

        emit(GenerationState.Preparing("Connecting to ${provider.displayName}"))
        val personBytes = io.loadImageBytes(request.person)
        val garmentBytes = io.loadImageBytes(request.garment.uri)
        if (personBytes == null || garmentBytes == null) {
            emit(GenerationState.Failed(TryOnError.Internal("Couldn't read the selected images")))
            return@flow
        }

        val personDataUrl = io.toDataUrl(personBytes)
        val garmentDataUrl = io.toDataUrl(garmentBytes)
        val category = request.garment.category?.effectiveCategory() ?: GarmentCategory.ABAYA

        try {
            emit(GenerationState.Running(0.2f, "Uploading to ${provider.displayName}…"))
            val resultUrlOrPath = when (provider.platform) {
                CloudPlatform.HF_SPACE -> runHfSpace(provider, personDataUrl, garmentDataUrl, category)
                CloudPlatform.REPLICATE -> runReplicate(provider, personDataUrl, garmentDataUrl, category)
                CloudPlatform.FAL -> runFal(provider, personDataUrl, garmentDataUrl)
            }

            emit(GenerationState.Running(0.85f, "Downloading result…"))
            val outPath = io.downloadResult(resultUrlOrPath)
            emit(
                GenerationState.Complete(
                    TryOnResult(
                        imagePath = outPath,
                        executedTier = EngineTier.CLOUD,
                        durationMillis = System.currentTimeMillis() - startedAt,
                        watermarked = false,
                    ),
                ),
            )
        } catch (e: Exception) {
            val msg = e.message ?: "Cloud generation failed"
            val error = if (msg.contains("Unable to resolve host") || msg.contains("Network")) {
                TryOnError.NetworkUnavailable
            } else {
                TryOnError.Internal(msg)
            }
            emit(GenerationState.Failed(error))
        }
    }

    private suspend fun runHfSpace(
        provider: CloudModelProvider,
        person: String,
        garment: String,
        category: GarmentCategory,
    ): String {
        val result = hf.predict(
            spaceHost = provider.endpoint,
            apiName = provider.apiName,
            data = hfPayload(provider.id, person, garment, category),
            hfToken = settings.hfToken.value,
        )
        return extractImageRef(result)
    }

    private suspend fun runReplicate(
        provider: CloudModelProvider,
        person: String,
        garment: String,
        category: GarmentCategory,
    ): String {
        val token = settings.replicateToken.value ?: error("Replicate API token required")
        val version = provider.replicateVersion ?: error("Replicate version not configured")
        val input = when (provider.id) {
            "ootd-replicate" -> mapOf(
                "model_image" to person,
                "garment_image" to garment,
                "garment_category" to category.ootdReplicateCategory(),
            )
            else -> mapOf(
                "human_img" to person,
                "garm_img" to garment,
                "garment_des" to category.replicateLabel(),
            )
        }
        return replicate.predict(version = version, input = input, apiToken = token)
    }

    private suspend fun runFal(
        provider: CloudModelProvider,
        person: String,
        garment: String,
    ): String {
        val key = settings.falApiKey.value ?: error("FAL API key required")
        return fal.predict(
            endpoint = provider.endpoint,
            input = mapOf(
                "human_image_url" to person,
                "garment_image_url" to garment,
            ),
            apiKey = key,
        )
    }

    private fun hfPayload(
        id: String,
        person: String,
        garment: String,
        category: GarmentCategory,
    ): List<String> = when (id) {
        "idm-vton-hf" -> listOf(person, garment, category.idmLabel(), "upper_body", "false", "30", "42")
        "leffa-hf" -> listOf(person, garment, "vton", "1.0")
        "ootd-hf" -> listOf(person, garment, category.ootdLabel(), "1.0")
        "fitdit-hf" -> listOf(person, garment, category.fitditLabel())
        "catvton-hf" -> listOf(person, garment)
        else -> listOf(person, garment)
    }

    private fun extractImageRef(element: kotlinx.serialization.json.JsonElement): String {
        return when (element) {
            is JsonPrimitive -> element.content
            is kotlinx.serialization.json.JsonObject -> {
                element["url"]?.jsonPrimitive?.content
                    ?: element["path"]?.jsonPrimitive?.content
                    ?: element["image"]?.jsonPrimitive?.content
                    ?: error("Unrecognized cloud output format")
            }
            else -> error("Unrecognized cloud output format")
        }
    }
}

private fun GarmentCategory.idmLabel(): String = when (this) {
    GarmentCategory.LOWER_BODY -> "lower_body"
    GarmentCategory.HIJAB, GarmentCategory.NIQAB, GarmentCategory.DUPATTA, GarmentCategory.HEADSCARF -> "upper_body"
    else -> "dresses"
}

private fun GarmentCategory.replicateLabel(): String = idmLabel()

private fun GarmentCategory.ootdLabel(): String = when (this) {
    GarmentCategory.LOWER_BODY -> "Lower-body"
    else -> "Upper-body"
}

private fun GarmentCategory.ootdReplicateCategory(): String = when (this) {
    GarmentCategory.LOWER_BODY -> "lowerbody"
    GarmentCategory.ABAYA, GarmentCategory.KAFTAN, GarmentCategory.SHALWAR_KAMEEZ -> "dress"
    else -> "upperbody"
}

private fun GarmentCategory.fitditLabel(): String = when (this) {
    GarmentCategory.LOWER_BODY -> "lower"
    else -> "upper"
}

/** Platform seam for loading/saving images for cloud engines. */
interface CloudImageIo {
    suspend fun loadImageBytes(person: com.zakir.vestra.shared.domain.PersonSource): ByteArray?
    suspend fun loadImageBytes(uri: String): ByteArray?
    fun toDataUrl(jpegBytes: ByteArray): String
    suspend fun downloadResult(urlOrPath: String): String
}
