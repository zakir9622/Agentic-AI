package com.zakir.vestra.shared.engine.lite

import android.graphics.Bitmap
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.packs.ModelPackManager

/**
 * Runs the human-parsing model from the Lite pack and derives the garment
 * target region. Shared by the Lite engine (warp target) and the Pro engine
 * (inpaint mask).
 *
 * Never throws — corrupt or mid-copy ONNX files return null / false instead of
 * crashing the garment screen or generation pipeline.
 */
class HumanParsing(private val packs: ModelPackManager) {

    /** Null when the Lite pack isn't ready or no person was found. */
    fun analyze(person: Bitmap, category: GarmentCategory): TargetRegion? =
        withHumanParseModel { model ->
            val (h, w) = model.inputSize(defaultSize = 473)
            val (logits, shape) = model.run(ImageOps.toNormalizedChw(person, h, w), h, w)
            val classes = shape.getOrNull(1)?.toInt() ?: return@withHumanParseModel null
            val outH = shape.getOrNull(2)?.toInt() ?: h
            val outW = shape.getOrNull(3)?.toInt() ?: w
            val classMap = ImageOps.argmax(logits, classes, outH * outW)

            val wanted = category.atrClassIds()
            val regionMask = BooleanArray(outH * outW) { classMap[it] in wanted }
            // Fall back to "anything person-shaped" when the exact classes are
            // absent (e.g. parsing a bare-torso photo for a dress).
            val effective = if (regionMask.none { it }) {
                BooleanArray(outH * outW) { classMap[it] != 0 }
            } else {
                regionMask
            }
            val box = ImageOps.boundingBox(effective, outW, outH) ?: return@withHumanParseModel null
            TargetRegion.fromMask(effective, outW, outH, box, person)
        }

    /**
     * True when the image looks like a photo of a *person wearing* the outfit
     * (a face + skin + limbs), rather than a flat/hanger garment shot. Used by
     * the input guard: feeding a whole-scene model photo makes the Lite
     * compositor paste the entire picture onto the model. Returns false when the
     * pack isn't ready (can't tell → don't warn).
     */
    fun looksLikeWornPhoto(image: Bitmap): Boolean =
        withHumanParseModel { model ->
            val (h, w) = model.inputSize(defaultSize = 473)
            val (logits, shape) = model.run(ImageOps.toNormalizedChw(image, h, w), h, w)
            val classes = shape.getOrNull(1)?.toInt() ?: return@withHumanParseModel false
            val outH = shape.getOrNull(2)?.toInt() ?: h
            val outW = shape.getOrNull(3)?.toInt() ?: w
            val classMap = ImageOps.argmax(logits, classes, outH * outW)
            val total = (outH * outW).toFloat()

            // ATR: 2=hair 11=face 12/13=legs 14/15=arms.
            var face = 0; var hair = 0; var limbs = 0
            for (c in classMap) {
                when (c) {
                    11 -> face++
                    2 -> hair++
                    12, 13, 14, 15 -> limbs++
                }
            }
            // A worn photo shows a visible face/hair AND exposed skin (limbs).
            // A flat garment shows neither meaningfully.
            (face / total > 0.004f || hair / total > 0.02f) && limbs / total > 0.02f
        } ?: false

    private inline fun <T> withHumanParseModel(block: (OrtModel) -> T): T? {
        if (!packs.isReady(LiteEngine.PACK_ID)) return null
        val packDir = packs.installedDir(LiteEngine.PACK_ID) ?: return null
        return runCatching {
            OrtSessionCache.open("$packDir/human_parse.onnx").let { block(it) }
        }.getOrNull()
    }
}

internal fun GarmentCategory.atrClassIds(): Set<Int> = when (this) {
    // ATR labels: 1=hat 2=hair 4=upper-clothes 5=skirt 6=pants 7=dress
    // 11=face 12/13=legs 14/15=arms 17=scarf
    GarmentCategory.UPPER_BODY, GarmentCategory.KURTA -> setOf(4, 7)
    GarmentCategory.LOWER_BODY -> setOf(5, 6)
    GarmentCategory.DRESS, GarmentCategory.LEHENGA -> setOf(4, 5, 6, 7)
    GarmentCategory.ABAYA, GarmentCategory.JILBAB, GarmentCategory.KAFTAN,
    GarmentCategory.FULL_COVERAGE, GarmentCategory.SHALWAR_KAMEEZ,
    -> setOf(4, 5, 6, 7, 12, 13, 14, 15)
    GarmentCategory.HIJAB, GarmentCategory.DUPATTA, GarmentCategory.HEADSCARF -> setOf(1, 2, 17)
    GarmentCategory.NIQAB -> setOf(1, 2, 11, 17)
}
