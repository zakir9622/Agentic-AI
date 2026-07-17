package com.zakir.vestra.shared.engine.lite

import android.graphics.Bitmap
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.packs.ModelPackManager

/**
 * Runs the human-parsing model from the Lite pack and derives the garment
 * target region. Shared by the Lite engine (warp target) and the Pro engine
 * (inpaint mask).
 */
class HumanParsing(private val packs: ModelPackManager) {

    /** Null when the Lite pack isn't installed or no person was found. */
    fun analyze(person: Bitmap, category: GarmentCategory): TargetRegion? {
        val packDir = packs.installedDir(LiteEngine.PACK_ID) ?: return null
        return OrtModel("$packDir/human_parse.onnx").use { model ->
            val (h, w) = model.inputSize(defaultSize = 473)
            val (logits, shape) = model.run(ImageOps.toNormalizedChw(person, h, w), h, w)
            val classes = shape.getOrNull(1)?.toInt() ?: return null
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
            val box = ImageOps.boundingBox(effective, outW, outH) ?: return null
            TargetRegion.fromMask(effective, outW, outH, box, person)
        }
    }

    /**
     * True when the image looks like a photo of a *person wearing* the outfit
     * (a face + skin + limbs), rather than a flat/hanger garment shot. Used by
     * the input guard: feeding a whole-scene model photo makes the Lite
     * compositor paste the entire picture onto the model. Returns false when the
     * pack isn't installed (can't tell → don't warn).
     */
    fun looksLikeWornPhoto(image: Bitmap): Boolean {
        val packDir = packs.installedDir(LiteEngine.PACK_ID) ?: return false
        return OrtModel("$packDir/human_parse.onnx").use { model ->
            val (h, w) = model.inputSize(defaultSize = 473)
            val (logits, shape) = model.run(ImageOps.toNormalizedChw(image, h, w), h, w)
            val classes = shape.getOrNull(1)?.toInt() ?: return false
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
        }
    }
}

internal fun GarmentCategory.atrClassIds(): Set<Int> = when (this) {
    // ATR labels: 1=hat 2=hair 4=upper-clothes 5=skirt 6=pants 7=dress
    // 12/13=legs 14/15=arms 17=scarf
    GarmentCategory.UPPER_BODY -> setOf(4, 7)
    GarmentCategory.LOWER_BODY -> setOf(5, 6)
    GarmentCategory.DRESS -> setOf(4, 5, 6, 7)
    // Abaya/jilbab/kaftan: coverage extends over arms and legs to the ankle.
    GarmentCategory.FULL_COVERAGE -> setOf(4, 5, 6, 7, 12, 13, 14, 15)
    // Hijab/scarf/dupatta replace the hair/head region, not the body.
    GarmentCategory.HEADSCARF -> setOf(1, 2, 17)
}
