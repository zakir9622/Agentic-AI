package com.zakir.vestra.shared.engine.pro

import android.graphics.Bitmap
import android.util.Log
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.domain.TryOnResult
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.TryOnEngine
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import com.zakir.vestra.shared.engine.lite.Watermark
import com.zakir.vestra.shared.packs.DeviceProbe
import com.zakir.vestra.shared.packs.ModelPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

/**
 * On-device try-on diffusion in the CatVTON style: a single inpainting UNet
 * conditioned by concatenating the garment latent spatially — no text encoder.
 *
 * The pack ("pro-v1") supplies vae_encoder.onnx / vae_decoder.onnx / unet.onnx
 * plus config.json (see [ProPackConfig]). The engine is model-agnostic within
 * that contract; per-stage timings are logged under [TAG] as the benchmark
 * record for real-device validation (see docs/ARCHITECTURE.md, M4).
 */
class DiffusionEngine(
    private val packs: ModelPackManager,
    private val device: DeviceProbe,
    private val io: LiteEngineIo,
    private val masker: PersonMasker,
) : TryOnEngine {

    override val tier: EngineTier = EngineTier.PRO

    override fun isAvailable(): Availability {
        val pack = packs.pack(PACK_ID)
        return when {
            pack != null && !packs.deviceMeets(pack.minSpec) ->
                Availability.Unavailable(UnavailableReason.DEVICE_NOT_CAPABLE)
            !packs.isInstalled(PACK_ID) ->
                Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED)
            // Parsing (the inpaint mask) comes from the Lite pack's models.
            !packs.isInstalled(com.zakir.vestra.shared.engine.lite.LiteEngine.PACK_ID) ->
                Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED)
            else -> Availability.Ready
        }
    }

    override fun generate(request: TryOnRequest): Flow<GenerationState> = flow {
        val packDir = packs.installedDir(PACK_ID)
        if (packDir == null) {
            emit(GenerationState.Failed(TryOnError.ModelPackMissing))
            return@flow
        }
        val startedAt = System.currentTimeMillis()

        emit(GenerationState.Preparing("Reading images"))
        val person = io.loadPerson(request.person)
        val garment = io.loadBitmap(request.garment.uri)
        if (person == null || garment == null) {
            emit(GenerationState.Failed(TryOnError.Internal("Couldn't read the selected images")))
            return@flow
        }

        try {
            val config = Json { ignoreUnknownKeys = true }
                .decodeFromString<ProPackConfig>(File("$packDir/config.json").readText())

            emit(GenerationState.Running(0.05f, "Reading the body"))
            var t0 = System.currentTimeMillis()
            val mask = masker.maskFor(person, request.garment.category ?: GarmentCategory.DRESS)
            if (mask == null) {
                emit(
                    GenerationState.Failed(
                        TryOnError.Internal("No person detected — try a clearer full-body photo"),
                    ),
                )
                return@flow
            }
            log("person_mask", t0)

            LatentCodec(packDir, config).use { codec ->
                emit(GenerationState.Running(0.12f, "Preparing the canvas"))
                t0 = System.currentTimeMillis()
                val personCanvas = codec.centerCrop(person)
                val garmentCanvas = codec.centerCrop(garment)
                val maskCanvas = codec.resizeMask(mask, person.width, person.height)

                val maskedPersonLatent = codec.encodeMasked(personCanvas, maskCanvas)
                val garmentLatent = codec.encode(garmentCanvas)
                val conditionLatent = codec.conditionLatent(maskedPersonLatent, garmentLatent)
                val unconditionalLatent = codec.unconditionalLatent(maskedPersonLatent)
                val maskConcat = codec.maskConcat(maskCanvas)
                log("vae_encode", t0)

                val scheduler = DdimScheduler()
                val steps = config.inferenceSteps
                val timesteps = scheduler.timesteps(steps)
                val random = request.seed?.let { Random(it) } ?: Random(System.nanoTime())
                val sample = codec.initialNoise(random)

                t0 = System.currentTimeMillis()
                codec.openUnet().use { unet ->
                    timesteps.forEachIndexed { index, timestep ->
                        val noisePred = unet.predictNoise(
                            sample = sample,
                            conditionLatent = conditionLatent,
                            unconditionalLatent = unconditionalLatent,
                            maskConcat = maskConcat,
                            timestep = timestep,
                            guidanceScale = config.guidanceScale,
                        )
                        scheduler.step(sample, noisePred, timestep, steps)
                        emit(
                            GenerationState.Running(
                                0.15f + 0.7f * (index + 1) / steps,
                                "Weaving the look · ${index + 1}/$steps",
                            ),
                        )
                    }
                }
                log("denoise_${steps}steps", t0)

                emit(GenerationState.Running(0.9f, "Developing"))
                t0 = System.currentTimeMillis()
                val decoded = codec.decodePersonHalf(sample)
                // Keep untouched person pixels outside the inpaint mask.
                val composed = codec.pasteBack(person, decoded, maskCanvas)
                log("vae_decode", t0)

                val outPath = io.saveResult(Watermark.apply(composed))
                emit(
                    GenerationState.Complete(
                        TryOnResult(
                            imagePath = outPath,
                            executedTier = EngineTier.PRO,
                            durationMillis = System.currentTimeMillis() - startedAt,
                            watermarked = true,
                        ),
                    ),
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "Pro generation failed", error)
            emit(GenerationState.Failed(TryOnError.Internal(error.message ?: "Generation failed")))
        }
    }.flowOn(Dispatchers.Default)

    private fun log(stage: String, since: Long) {
        Log.i(TAG, "$stage: ${System.currentTimeMillis() - since} ms (ram=${device.totalRamMb()} MB)")
    }

    companion object {
        const val PACK_ID = "pro-v1"
        const val TAG = "VestraProBench"
    }
}

/** Person-region mask provider; implemented by the Lite pipeline's parser. */
fun interface PersonMasker {
    /** 0..1 row-major mask over the person image for the garment area; null = no person found. */
    fun maskFor(person: Bitmap, category: GarmentCategory): FloatArray?
}

@kotlinx.serialization.Serializable
data class ProPackConfig(
    val latentWidth: Int = 96,
    val latentHeight: Int = 128,
    val imageWidth: Int = 768,
    val imageHeight: Int = 1024,
    val inferenceSteps: Int = 12,
    val guidanceScale: Float = 2.5f,
    val vaeScale: Float = 0.18215f,
    /** "width" or "height" — axis the garment latent is concatenated along. */
    val concatAxis: String = "width",
)
