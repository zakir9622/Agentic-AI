package com.zakir.vestra.shared.engine.pro

import android.graphics.Bitmap
import android.util.Log
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.effectiveCategory
import com.zakir.vestra.shared.engine.pipeline.CastingPromptBuilder
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.domain.TryOnResult
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.TryOnEngine
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import com.zakir.vestra.shared.engine.lite.Watermark
import com.zakir.vestra.shared.engine.pipeline.ConditioningStage
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
    private val applyWatermark: Boolean = false,
) : TryOnEngine {

    override val tier: EngineTier = EngineTier.PRO

    private fun resolvePackId(): String? =
        PACK_IDS.firstOrNull { packs.isInstalled(it) }

    override fun isAvailable(): Availability {
        val packId = resolvePackId() ?: return Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED)
        val pack = packs.pack(packId)
        return when {
            pack != null && !packs.deviceMeets(pack.minSpec) ->
                Availability.Unavailable(UnavailableReason.DEVICE_NOT_CAPABLE)
            !packs.isInstalled(packId) ->
                Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED)
            // Pro masks the person with the Lite pack's parser, so Lite must be installed too.
            !packs.isInstalled(com.zakir.vestra.shared.engine.lite.LiteEngine.PACK_ID) ->
                Availability.Unavailable(UnavailableReason.COMPANION_PACK_MISSING)
            else -> Availability.Ready
        }
    }

    override fun generate(request: TryOnRequest): Flow<GenerationState> = flow {
        val packId = resolvePackId()
        val packDir = packId?.let { packs.installedDir(it) }
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
            val category = request.garment.category?.effectiveCategory() ?: GarmentCategory.DRESS
            val promptSpec = com.zakir.vestra.shared.engine.pipeline.PromptSpec(
                positive = CastingPromptBuilder.buildPositive(request.casting, category),
                negative = CastingPromptBuilder.buildNegative(),
            )
            val finish: (Bitmap) -> String = { bmp ->
                io.saveResult(if (applyWatermark) Watermark.apply(bmp) else bmp)
            }

            val config = Json { ignoreUnknownKeys = true }
                .decodeFromString<ProPackConfig>(File("$packDir/config.json").readText())

            val sdPipeline = SdControlNetPipeline(
                packDir = packDir,
                config = config,
                loadPerson = { person },
                loadGarment = { garment },
                maskProvider = { p -> masker.maskFor(p, category) },
                saveResult = finish,
            )
            if (sdPipeline.requirements().isFullyConditioned) {
                try {
                    val outPath = sdPipeline.run(
                        inputs = com.zakir.vestra.shared.engine.pipeline.ConditioningInputs(
                            personImagePath = "",
                            garmentImagePath = request.garment.uri,
                            maskPath = null,
                            prompt = promptSpec,
                            seed = request.seed,
                        ),
                        onStage = { stage, fraction -> emit(GenerationState.Running(fraction, stage.label)) },
                    )
                    emit(
                        GenerationState.Complete(
                            TryOnResult(
                                imagePath = outPath,
                                executedTier = EngineTier.PRO,
                                durationMillis = System.currentTimeMillis() - startedAt,
                                watermarked = applyWatermark,
                            ),
                        ),
                    )
                    return@flow
                } catch (error: Exception) {
                    Log.e(TAG, "SD-ControlNet pipeline failed; falling back", error)
                }
            }

            // ── Stage 1: STRUCTURE — establish structural bounds (ControlNet
            // condition). The parse-derived body mask locks the garment to the
            // model's pose; a dedicated pose/depth model slots in here when the
            // pack ships one (config.structureModel).
            emit(GenerationState.Running(0.05f, ConditioningStage.STRUCTURE.label))
            var t0 = System.currentTimeMillis()
            val mask = masker.maskFor(person, category)
            if (mask == null) {
                emit(
                    GenerationState.Failed(
                        TryOnError.Internal("No person detected — try a clearer full-body photo"),
                    ),
                )
                return@flow
            }
            log("structure_mask", t0)

            LatentCodec(packDir, config).use { codec ->
                // ── Stage 2: TEXTURE — inject garment features (IP-Adapter). The
                // garment latent carries appearance into cross-attention instead
                // of a hallucination-prone text description.
                emit(GenerationState.Running(0.12f, ConditioningStage.TEXTURE.label))
                t0 = System.currentTimeMillis()
                val personCanvas = codec.centerCrop(person)
                val garmentCanvas = codec.centerCrop(garment)
                val maskCanvas = codec.resizeMask(mask, person.width, person.height)

                val maskedPersonLatent = codec.encodeMasked(personCanvas, maskCanvas)
                val garmentLatent = codec.encode(garmentCanvas)
                val conditionLatent = codec.conditionLatent(maskedPersonLatent, garmentLatent)
                val unconditionalLatent = codec.unconditionalLatent(maskedPersonLatent)
                val maskConcat = codec.maskConcat(maskCanvas)
                log("ipadapter_encode", t0)

                // ── Stage 3: SYNTHESIS — diffuse under structure + texture +
                // PromptStyle guidance (CFG 7.0, 20–25 steps, mobile-safe).
                val scheduler = DdimScheduler()
                val steps = if (config.lcmDistilled) {
                    minOf(8, maxOf(4, config.inferenceSteps / 4))
                } else {
                    config.inferenceSteps
                }
                val cfg = config.guidanceScale
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
                            guidanceScale = cfg,
                        )
                        scheduler.step(sample, noisePred, timestep, steps)
                        emit(
                            GenerationState.Running(
                                0.15f + 0.7f * (index + 1) / steps,
                                "${ConditioningStage.SYNTHESIS.label} · ${index + 1}/$steps",
                            ),
                        )
                    }
                }
                log("synthesis_${steps}steps_cfg${cfg}", t0)

                emit(GenerationState.Running(0.88f, "Developing"))
                t0 = System.currentTimeMillis()
                val decoded = codec.decodePersonHalf(sample)
                // Keep untouched person pixels outside the inpaint mask.
                val composed = codec.pasteBack(person, decoded, maskCanvas)
                log("vae_decode", t0)

                // Studio backdrop via the Lite pack's segmenter (installed as a Pro dependency).
                val liteDir = packs.installedDir(com.zakir.vestra.shared.engine.lite.LiteEngine.PACK_ID)
                val staged = if (request.backdrop == com.zakir.vestra.shared.domain.Backdrop.ORIGINAL || liteDir == null) {
                    composed
                } else {
                    emit(GenerationState.Running(0.93f, "Setting the backdrop"))
                    com.zakir.vestra.shared.engine.lite.BackdropCompositor("$liteDir/garment_seg.onnx")
                        .apply(composed, request.backdrop)
                }

                val outPath = finish(staged)
                emit(
                    GenerationState.Complete(
                        TryOnResult(
                            imagePath = outPath,
                            executedTier = EngineTier.PRO,
                            durationMillis = System.currentTimeMillis() - startedAt,
                            watermarked = applyWatermark,
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
        val PACK_IDS = listOf("pro-v2-int8", "pro-v1")
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
    // Photorealism defaults (PromptStyle): CFG 7.0, 22 steps. A pack tuned for a
    // low-CFG model (e.g. CatVTON) may override both in its config.json.
    val inferenceSteps: Int = com.zakir.vestra.shared.engine.pipeline.PromptStyle.STEPS,
    /** When true, use LCM/Hyper-SD distilled step count (4–8) instead of full diffusion. */
    val lcmDistilled: Boolean = false,
    val guidanceScale: Float = com.zakir.vestra.shared.engine.pipeline.PromptStyle.CFG_SCALE,
    val vaeScale: Float = 0.18215f,
    /** "width" or "height" — axis the garment latent is concatenated along (legacy CatVTON path). */
    val concatAxis: String = "width",
    /** Square working resolution for the SD1.5+ControlNet+IP-Adapter path. */
    val resolution: Int = 512,
    // ── SD1.5 + ControlNet-Depth + IP-Adapter component files (P7) ──
    /** ControlNet-Depth ONNX producing down/mid residuals. */
    val controlNet: String? = null,
    /** Monocular depth estimator ONNX for Stage A structural conditioning. */
    val depthModel: String? = null,
    /** IP-Adapter projection/resampler ONNX. Unused by the fused-UNet pack: the
     *  IP-Adapter Plus resampler + attention are baked into unet.onnx, so the
     *  image encoder's raw embeds feed the UNet directly (see SdControlNetPipeline). */
    val ipAdapter: String? = null,
    /** CLIP-H image encoder ONNX. Emits penultimate hidden states [1,ipEmbedSeq,ipEmbedDim]. */
    val imageEncoder: String? = null,
    /** SD text encoder ONNX (prompt → embeddings) for PromptStyle guidance. */
    val textEncoder: String? = null,
    /** Optional legacy alias retained for older packs. */
    val structureModel: String? = null,
    // ── Shapes emitted by the converter (ml/convert_pro_pack.py) so the runtime
    //    feeds exactly what the exported graphs declare. ──
    /** IP-Adapter image-embed sequence length (CLIP-H penultimate: 257). */
    val ipEmbedSeq: Int = 257,
    /** IP-Adapter image-embed hidden dim (CLIP-H: 1280). */
    val ipEmbedDim: Int = 1280,
    /** ControlNet down_0..down_11 + mid residual shapes (SD1.5 @512), in order. */
    val residualShapes: List<List<Int>> = DEFAULT_RESIDUAL_SHAPES,
) {
    companion object {
        /** SD1.5 ControlNet residual shapes at 512×512 (64×64 latent). */
        val DEFAULT_RESIDUAL_SHAPES: List<List<Int>> = listOf(
            listOf(1, 320, 64, 64), listOf(1, 320, 64, 64), listOf(1, 320, 64, 64),
            listOf(1, 320, 32, 32), listOf(1, 640, 32, 32), listOf(1, 640, 32, 32),
            listOf(1, 640, 16, 16), listOf(1, 1280, 16, 16), listOf(1, 1280, 16, 16),
            listOf(1, 1280, 8, 8), listOf(1, 1280, 8, 8), listOf(1, 1280, 8, 8),
            listOf(1, 1280, 8, 8),
        )
    }
}
