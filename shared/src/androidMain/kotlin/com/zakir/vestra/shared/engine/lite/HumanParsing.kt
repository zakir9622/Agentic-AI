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
            val (h, w) = model.inputSize(defaultSize = 512)
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
     * rather than a flat/hanger garment shot.
     *
     * Uses a cheap bitmap heuristic only — never opens `human_parse.onnx` here.
     * Loading that ~67 MB graph on garment pick caused native process deaths on
     * Pixel 9 (see troubleshooting bundles @ v3.0.13). Full ATR parsing still
     * runs at generate time via [analyze].
     */
    fun looksLikeWornPhoto(image: Bitmap): Boolean =
        runCatching { looksLikeWornPhotoHeuristic(image) }.getOrDefault(false)

    /**
     * Portrait + skin-tone cluster in the upper band → likely a worn/model shot.
     * False negatives are OK (user can still try on); false positives only warn.
     */
    internal fun looksLikeWornPhotoHeuristic(image: Bitmap): Boolean {
        val width = image.width
        val height = image.height
        if (width < 32 || height < 32) return false
        val portrait = height > width * 1.05f
        if (!portrait) return false

        var skin = 0
        var samples = 0
        val stepX = (width / 24).coerceAtLeast(1)
        val stepY = (height / 24).coerceAtLeast(1)
        val yMax = (height * 0.38f).toInt().coerceAtLeast(1)
        val x0 = width / 5
        val x1 = width * 4 / 5
        var y = 0
        while (y < yMax) {
            var x = x0
            while (x < x1) {
                val p = image.getPixel(x, y)
                val r = (p ushr 16) and 0xff
                val g = (p ushr 8) and 0xff
                val b = p and 0xff
                // Crude skin gate (works across common lighting; not a face detector).
                if (r > 95 && g > 40 && b > 20 && r > g && r > b && (r - g) > 12) {
                    skin++
                }
                samples++
                x += stepX
            }
            y += stepY
        }
        if (samples == 0) return false
        return skin.toFloat() / samples > 0.08f
    }

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
