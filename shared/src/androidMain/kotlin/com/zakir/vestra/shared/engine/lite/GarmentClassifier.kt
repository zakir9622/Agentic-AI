package com.zakir.vestra.shared.engine.lite

import android.graphics.Bitmap
import com.zakir.vestra.shared.domain.GarmentCategory

/**
 * Heuristic garment categorization from the segmentation cutout's geometry.
 * Used when the user leaves the category on Auto; a trained classifier can
 * replace this without touching callers (same signature, better priors).
 *
 * Signals: aspect ratio of the opaque bounding box, and how much of the box
 * the garment fills (scarves are thin/hollow, abayas are tall solid columns).
 */
object GarmentClassifier {

    fun classify(garmentCutout: Bitmap): GarmentCategory {
        val width = garmentCutout.width
        val height = garmentCutout.height
        val pixels = IntArray(width * height)
        garmentCutout.getPixels(pixels, 0, width, 0, 0, width, height)

        var opaque = 0
        // Row occupancy tells apart "legs" shapes (two columns) from solid bodies.
        var splitRows = 0
        var measuredRows = 0
        for (y in 0 until height step (height / 64).coerceAtLeast(1)) {
            var runs = 0
            var inRun = false
            var rowOpaque = 0
            for (x in 0 until width) {
                val isOpaque = (pixels[y * width + x] ushr 24) > 40
                if (isOpaque) {
                    rowOpaque++
                    if (!inRun) {
                        runs++
                        inRun = true
                    }
                } else {
                    inRun = false
                }
            }
            if (rowOpaque > width / 20) {
                measuredRows++
                if (runs >= 2) splitRows++
            }
        }
        for (pixel in pixels) if ((pixel ushr 24) > 40) opaque++

        val aspect = height.toFloat() / width.coerceAtLeast(1)
        val fill = opaque.toFloat() / (width * height)
        val splitFraction = if (measuredRows == 0) 0f else splitRows.toFloat() / measuredRows

        return when {
            fill < 0.30f && aspect < 1.4f -> GarmentCategory.HIJAB
            splitFraction > 0.45f && aspect > 1.1f -> GarmentCategory.LOWER_BODY
            aspect > 1.8f && fill > 0.45f -> GarmentCategory.ABAYA
            aspect > 1.25f -> GarmentCategory.DRESS
            else -> GarmentCategory.KURTA
        }
    }
}
