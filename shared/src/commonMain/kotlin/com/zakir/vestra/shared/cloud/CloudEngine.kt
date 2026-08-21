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
import com.zakir.vestra.shared.usage.UsageLedger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Free-tier cloud try-on via Hugging Face Spaces only.
 * Never auto-routed — user must explicitly select Cloud in Settings.
 */
class CloudEngine(
    private val http: HttpClient,
    private val io: CloudImageIo,
    private val settings: AppSettings,
    private val usage: UsageLedger? = null,
) : TryOnEngine {

    private val hf = HfGradioClient(http)

    override val tier: EngineTier = EngineTier.CLOUD

    override fun isAvailable(): Availability {
        val provider = settings.selectedCloudProvider()
        return when {
            !settings.networkLikelyAvailable() ->
                Availability.Unavailable(UnavailableReason.OFFLINE)
            provider.requiresApiKey && settings.apiKeyFor(provider).isNullOrBlank() ->
                Availability.Unavailable(UnavailableReason.NOT_CONFIGURED)
            CloudModelContracts.preflightOrNull(provider) != null ->
                Availability.Unavailable(UnavailableReason.NOT_CONFIGURED)
            else -> Availability.Ready
        }
    }

    override fun generate(request: TryOnRequest): Flow<GenerationState> = flow {
        val provider = settings.selectedCloudProvider()
        val startedAt = System.currentTimeMillis()

        CloudModelContracts.preflightOrNull(provider)?.let { blocked ->
            usage?.record(
                provider,
                success = false,
                note = CloudModelContracts.usageFailureNote(provider, blocked),
            )
            emit(GenerationState.Failed(TryOnError.Internal(blocked)))
            return@flow
        }

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
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for try-on"
            }
            val resultUrlOrPath = runHfSpace(provider, personDataUrl, garmentDataUrl, category)

            emit(GenerationState.Running(0.85f, "Downloading result…"))
            val outPath = io.downloadResult(resultUrlOrPath, spaceHost = provider.endpoint)
            usage?.record(
                provider,
                success = true,
                note = "Try-on · ${provider.displayName} · ${CloudModelContracts.statusLabel(provider)}",
            )
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val friendly = CloudModelContracts.friendlyFailure(provider, e.message.orEmpty(), "Try-on")
            usage?.record(
                provider,
                success = false,
                note = CloudModelContracts.usageFailureNote(provider, e.message.orEmpty()),
            )
            val error = if (
                friendly.contains("No internet", ignoreCase = true) ||
                e.message.orEmpty().contains("Unable to resolve host", ignoreCase = true) ||
                e.message.orEmpty().contains("timeout", ignoreCase = true)
            ) {
                TryOnError.NetworkUnavailable
            } else {
                TryOnError.Internal(friendly)
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
            apiName = CloudModelContracts.effectiveApiName(provider),
            data = SpacePayloads.forTryOn(provider.id, person, garment, category),
            hfToken = settings.hfToken.value,
        )
        return extractImageRef(result)
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

/** Platform seam for loading/saving images for cloud engines. */
interface CloudImageIo {
    suspend fun loadImageBytes(person: com.zakir.vestra.shared.domain.PersonSource): ByteArray?
    suspend fun loadImageBytes(uri: String): ByteArray?
    fun toDataUrl(jpegBytes: ByteArray): String
    suspend fun downloadResult(urlOrPath: String, spaceHost: String? = null): String
}
