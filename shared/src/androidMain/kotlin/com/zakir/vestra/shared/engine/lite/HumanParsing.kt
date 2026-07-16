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
}

internal fun GarmentCategory.atrClassIds(): Set<Int> = when (this) {
    // ATR labels: 4=upper-clothes 5=skirt 6=pants 7=dress
    GarmentCategory.UPPER_BODY -> setOf(4, 7)
    GarmentCategory.LOWER_BODY -> setOf(5, 6)
    GarmentCategory.DRESS -> setOf(4, 5, 6, 7)
}
