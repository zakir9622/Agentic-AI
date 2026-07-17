package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.domain.TryOnResult
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.TryOnEngine
import com.zakir.vestra.shared.engine.UnavailableReason
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.TimeSource

/**
 * Maximum-quality tier: ships both images to the `tryon` Edge Function, which
 * runs a hosted try-on model and deletes the inputs after the run. Only ever
 * used when the user explicitly selects Cloud in Settings — see EngineRouter.
 */
class CloudEngine(
    private val http: HttpClient,
    private val io: CloudIo,
    private val config: CloudConfig,
) : TryOnEngine {

    override val tier: EngineTier = EngineTier.CLOUD

    override fun isAvailable(): Availability = when {
        !config.isConfigured -> Availability.Unavailable(UnavailableReason.NOT_CONFIGURED)
        !io.isOnline() -> Availability.Unavailable(UnavailableReason.OFFLINE)
        else -> Availability.Ready
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun generate(request: TryOnRequest): Flow<GenerationState> = flow {
        val started = TimeSource.Monotonic.markNow()
        emit(GenerationState.Preparing(com.zakir.vestra.shared.engine.pipeline.ConditioningStage.TEXTURE.label))

        val garment = io.readJpegBytes(request.garment.uri)
        val person = when (val source = request.person) {
            is PersonSource.UserPhoto -> io.readJpegBytes(source.uri)
            is PersonSource.AiModel -> io.readAiModelJpegBytes(source.modelId)
        }
        if (garment == null || person == null) {
            emit(GenerationState.Failed(TryOnError.Internal("Couldn't read the selected images")))
            return@flow
        }

        emit(GenerationState.Running(0.2f, "Uploading to the atelier"))
        try {
            val response = http.post(config.tryOnUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
                header("apikey", config.anonKey)
                setBody(
                    Json.encodeToString(
                        TryOnBody(
                            garmentB64 = Base64.encode(garment),
                            personB64 = Base64.encode(person),
                            category = request.garment.category.wireName(),
                            // Same photorealism guidance the on-device Pro engine uses,
                            // for hosted models that accept prompt/CFG/steps.
                            positive = com.zakir.vestra.shared.engine.pipeline.PromptStyle.POSITIVE,
                            negative = com.zakir.vestra.shared.engine.pipeline.PromptStyle.NEGATIVE,
                            cfgScale = com.zakir.vestra.shared.engine.pipeline.PromptStyle.CFG_SCALE,
                            steps = com.zakir.vestra.shared.engine.pipeline.PromptStyle.STEPS,
                        ),
                    ),
                )
            }
            if (!response.status.isSuccess()) {
                val detail = runCatching {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString<ErrorBody>(response.bodyAsText()).error
                }.getOrNull()
                emit(GenerationState.Failed(TryOnError.Internal(detail ?: "Cloud generation failed (${response.status.value})")))
                return@flow
            }
            emit(GenerationState.Running(0.85f, com.zakir.vestra.shared.engine.pipeline.ConditioningStage.SYNTHESIS.label))
            val path = io.saveWatermarkedJpeg(response.bodyAsBytes())
            emit(
                GenerationState.Complete(
                    TryOnResult(
                        imagePath = path,
                        executedTier = EngineTier.CLOUD,
                        durationMillis = started.elapsedNow().inWholeMilliseconds,
                        watermarked = true,
                    ),
                ),
            )
        } catch (error: Exception) {
            emit(
                GenerationState.Failed(
                    if (io.isOnline()) {
                        TryOnError.Internal(error.message ?: "Cloud generation failed")
                    } else {
                        TryOnError.NetworkUnavailable
                    },
                ),
            )
        }
    }
}

/** Platform IO for the cloud tier; Android implements over LiteEngineIo/Watermark. */
interface CloudIo {
    fun isOnline(): Boolean
    /** JPEG bytes (re-encoded, EXIF-stripped, bounded size) for the given content uri. */
    fun readJpegBytes(uri: String): ByteArray?
    fun readAiModelJpegBytes(modelId: String): ByteArray?
    /** Persists the returned image with the visible AI watermark + EXIF tag. */
    fun saveWatermarkedJpeg(bytes: ByteArray): String
}

data class CloudConfig(
    val tryOnUrl: String,
    val reportUrl: String,
    val anonKey: String,
) {
    val isConfigured: Boolean get() = tryOnUrl.startsWith("https://") && anonKey.isNotBlank()
}

@Serializable
private data class TryOnBody(
    @SerialName("garment_b64") val garmentB64: String,
    @SerialName("person_b64") val personB64: String,
    val category: String,
    val positive: String,
    val negative: String,
    @SerialName("cfg_scale") val cfgScale: Float,
    val steps: Int,
)

@Serializable
private data class ErrorBody(val error: String? = null)

private fun GarmentCategory?.wireName(): String = when (this) {
    GarmentCategory.UPPER_BODY -> "upper_body"
    GarmentCategory.LOWER_BODY -> "lower_body"
    // Hosted IDM-VTON only knows three categories; full-coverage garments and
    // headscarves route through "dresses" as the closest coverage region.
    GarmentCategory.DRESS, GarmentCategory.FULL_COVERAGE, GarmentCategory.HEADSCARF, null -> "dresses"
}
