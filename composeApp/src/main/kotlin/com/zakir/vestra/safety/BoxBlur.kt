package com.zakir.vestra.safety

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * In-place box blur over a rectangular region of a mutable [Bitmap] — no RenderScript (deprecated
 * on modern Android) and no native dependency. Several box-blur passes approximate a gaussian
 * blur closely enough for a privacy pass, where the goal is "unrecognizable," not photographic
 * quality.
 */
object BoxBlur {

    fun blurRegion(bitmap: Bitmap, rect: Rect, radius: Int, passes: Int = 3) {
        val clamped = Rect(
            rect.left.coerceIn(0, bitmap.width),
            rect.top.coerceIn(0, bitmap.height),
            rect.right.coerceIn(0, bitmap.width),
            rect.bottom.coerceIn(0, bitmap.height),
        )
        val w = clamped.width()
        val h = clamped.height()
        if (w <= 0 || h <= 0 || radius <= 0) return

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, clamped.left, clamped.top, w, h)
        repeat(passes) {
            boxBlurHorizontal(pixels, w, h, radius)
            boxBlurVertical(pixels, w, h, radius)
        }
        bitmap.setPixels(pixels, 0, w, clamped.left, clamped.top, w, h)
    }

    private fun boxBlurHorizontal(pixels: IntArray, w: Int, h: Int, radius: Int) {
        val out = IntArray(pixels.size)
        for (y in 0 until h) {
            val rowStart = y * w
            for (x in 0 until w) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dx in -radius..radius) {
                    val xi = (x + dx).coerceIn(0, w - 1)
                    val p = pixels[rowStart + xi]
                    a += (p ushr 24) and 0xff
                    r += (p ushr 16) and 0xff
                    g += (p ushr 8) and 0xff
                    b += p and 0xff
                    count++
                }
                out[rowStart + x] = averagePixel(a, r, g, b, count)
            }
        }
        System.arraycopy(out, 0, pixels, 0, pixels.size)
    }

    private fun boxBlurVertical(pixels: IntArray, w: Int, h: Int, radius: Int) {
        val out = IntArray(pixels.size)
        for (x in 0 until w) {
            for (y in 0 until h) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -radius..radius) {
                    val yi = (y + dy).coerceIn(0, h - 1)
                    val p = pixels[yi * w + x]
                    a += (p ushr 24) and 0xff
                    r += (p ushr 16) and 0xff
                    g += (p ushr 8) and 0xff
                    b += p and 0xff
                    count++
                }
                out[y * w + x] = averagePixel(a, r, g, b, count)
            }
        }
        System.arraycopy(out, 0, pixels, 0, pixels.size)
    }

    private fun averagePixel(a: Int, r: Int, g: Int, b: Int, count: Int): Int =
        ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
}
