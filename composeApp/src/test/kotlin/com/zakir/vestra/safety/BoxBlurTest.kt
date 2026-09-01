package com.zakir.vestra.safety

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class BoxBlurTest {

    @Test
    fun blurringASharpEdgeSmoothsTheTransition() {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        for (y in 0 until 20) {
            for (x in 0 until 20) {
                bitmap.setPixel(x, y, if (x < 10) Color.BLACK else Color.WHITE)
            }
        }
        BoxBlur.blurRegion(bitmap, Rect(0, 0, 20, 20), radius = 5, passes = 2)

        val center = bitmap.getPixel(10, 10)
        val r = Color.red(center)
        assertTrue(
            "expected a blurred gray value near the old edge, got red=$r",
            r in 30..225,
        )
    }

    @Test
    fun regionOutsideBoundsIsClampedNotCrashed() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        // Should not throw despite an out-of-bounds rect.
        BoxBlur.blurRegion(bitmap, Rect(-5, -5, 50, 50), radius = 3)
    }

    @Test
    fun zeroRadiusIsANoOp() {
        val bitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(2, 2, Color.RED)
        BoxBlur.blurRegion(bitmap, Rect(0, 0, 5, 5), radius = 0)
        assertEquals(Color.RED, bitmap.getPixel(2, 2))
    }

    @Test
    fun emptyRegionDoesNotCrash() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        BoxBlur.blurRegion(bitmap, Rect(3, 3, 3, 3), radius = 5)
    }

    @Test
    fun uniformColorStaysUniformAfterBlur() {
        val bitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
        for (y in 0 until 12) {
            for (x in 0 until 12) bitmap.setPixel(x, y, Color.rgb(120, 80, 200))
        }
        BoxBlur.blurRegion(bitmap, Rect(0, 0, 12, 12), radius = 3, passes = 2)
        val center = bitmap.getPixel(6, 6)
        assertEquals(120, Color.red(center))
        assertEquals(80, Color.green(center))
        assertEquals(200, Color.blue(center))
    }
}
