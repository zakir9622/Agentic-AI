package com.zakir.vestra.shared.engine.lite

import android.graphics.Bitmap
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.PackVerifyStatus
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.domain.TryOnResult
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.TryOnEngine
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.quality.NoOpQualityPostProcessor
import com.zakir.vestra.shared.quality.QualityEnhancer
import com.zakir.vestra.shared.quality.QualityPostProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Fully-offline try-on for every supported device:
 *
 *  1. garment_seg.onnx  → soft garment mask → alpha cutout
 *  2. human_parse.onnx  → per-pixel body-region classes on the person image
 *  3. contour warp      → garment meshed onto the target region's row profile
 *  4. harmonize + blend → luminance match, feathered composite
 *  5. finalize          → AI watermark + EXIF tag, JPEG to app storage
 *
 * Model files come from the "lite-v1" pack; nothing here touches the network.
 */
class LiteEngine(
    private val packs: ModelPackManager,
    private val io: LiteEngineIo,
    private val parsing: HumanParsing,
    private val quality: QualityPostProcessor = NoOpQualityPostProcessor,
) : TryOnEngine {

    override val tier: EngineTier = EngineTier.LITE

    override fun isAvailable(): Availability = when {
        !packs.isInstalled(PACK_ID) ->
            Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED)
        packs.isReady(PACK_ID) -> Availability.Ready
        packs.verifyStatus(PACK_ID) == PackVerifyStatus.FAILED ->
            Availability.Unavailable(UnavailableReason.PACK_VERIFY_FAILED)
        else ->
            Availability.Unavailable(UnavailableReason.PACK_VERIFY_PENDING)
    }

    override fun generate(request: TryOnRequest): Flow<GenerationState> = flow {
        if (!packs.isReady(PACK_ID)) {
            val msg = when (packs.verifyStatus(PACK_ID)) {
                PackVerifyStatus.FAILED ->
                    "Lite pack failed verification — open Settings → Model packs and re-download lite-v1."
                PackVerifyStatus.VERIFYING ->
                    "Lite pack is verifying — wait a moment and try again."
                else ->
                    "Lite pack not ready yet — wait for verification to finish."
            }
            emit(GenerationState.Failed(TryOnError.Internal(msg)))
            return@flow
        }
        val packDir = packs.installedDir(PACK_ID)
        if (packDir == null) {
            emit(GenerationState.Failed(TryOnError.ModelPackMissing))
            return@flow
        }
        val startedAt = System.currentTimeMillis()

        emit(GenerationState.Preparing("Reading images"))
        val garment = io.loadBitmap(request.garment.uri)
        val person = io.loadPerson(request.person)
        if (garment == null || person == null) {
            emit(GenerationState.Failed(TryOnError.Internal("Couldn't read the selected images")))
            return@flow
        }

        try {
            emit(GenerationState.Running(0.15f, "Extracting garment"))
            val garmentCut = runCatching {
                OrtSessionCache.open("$packDir/garment_seg.onnx").let { model ->
                    extractGarment(model, garment)
                }
            }.getOrElse { error ->
                emit(
                    GenerationState.Failed(
                        TryOnError.Internal(
                            error.message?.take(120)
                                ?: "Lite segmentation model failed — re-download lite-v1 in Settings → Model packs.",
                        ),
                    ),
                )
                return@flow
            }

            // Auto mode: infer the category from the cutout's geometry so abayas,
            // scarves, and trousers each land on the right body region.
            val category = request.garment.category ?: GarmentClassifier.classify(garmentCut)

            emit(GenerationState.Running(0.45f, "Reading the body"))
            val region = parsing.analyze(person, category)
            if (region == null) {
                emit(
                    GenerationState.Failed(
                        TryOnError.Internal("No person detected — try a clearer full-body photo"),
                    ),
                )
                return@flow
            }

            emit(GenerationState.Running(0.7f, "Fitting the garment"))
            val fitted = ContourWarp.fit(garmentCut, region)

            emit(GenerationState.Running(0.82f, "Harmonizing light"))
            val composed = Harmonizer.compose(person, fitted, region)

            // Studio backdrop: segment the model out and re-stage on the chosen scene.
            val staged = if (request.backdrop == com.zakir.vestra.shared.domain.Backdrop.ORIGINAL) {
                composed
            } else {
                emit(GenerationState.Running(0.9f, "Setting the backdrop"))
                runCatching {
                    BackdropCompositor("$packDir/garment_seg.onnx").apply(composed, request.backdrop)
                }.getOrElse { composed }
            }

            emit(GenerationState.Running(0.95f, "Developing"))
            val finalImage = QualityEnhancer.upscaleIfInstalled(quality, staged)
            val outPath = io.saveResult(Watermark.apply(finalImage))

            emit(
                GenerationState.Complete(
                    TryOnResult(
                        imagePath = outPath,
                        executedTier = EngineTier.LITE,
                        durationMillis = System.currentTimeMillis() - startedAt,
                        watermarked = true,
                    ),
                ),
            )
        } catch (error: Exception) {
            emit(GenerationState.Failed(TryOnError.Internal(error.message ?: "Generation failed")))
        }
    }.flowOn(Dispatchers.Default)

    private fun extractGarment(model: OrtModel, garment: Bitmap): Bitmap {
        val (h, w) = model.inputSize(defaultSize = 320)
        val (output, shape) = model.run(ImageOps.toNormalizedChw(garment, h, w), h, w)
        val maskH = shape.getOrNull(2)?.toInt() ?: h
        val maskW = shape.getOrNull(3)?.toInt() ?: w
        // First output plane of u2net-family models is the fused saliency map.
        val plane = output.copyOfRange(0, maskH * maskW)
        val mask = ImageOps.normalizeAndResizeMask(plane, maskW, maskH, garment.width, garment.height)
        val cut = ImageOps.applyAlphaMask(garment, mask)
        return cropToAlpha(cut)
    }

    private fun cropToAlpha(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val opaque = BooleanArray(width * height) { (pixels[it] ushr 24) > 40 }
        val box = ImageOps.boundingBox(opaque, width, height) ?: return bitmap
        return Bitmap.createBitmap(bitmap, box[0], box[1], box[2] - box[0] + 1, box[3] - box[1] + 1)
    }

    companion object {
        const val PACK_ID = "lite-v1"
    }
}
