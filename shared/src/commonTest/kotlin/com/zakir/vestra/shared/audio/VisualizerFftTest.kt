package com.zakir.vestra.shared.audio

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VisualizerFftTest {

    @Test
    fun emptyInputReturnsEmptyOutput() {
        assertTrue(magnitudesFromFft(ByteArray(0)).isEmpty())
    }

    @Test
    fun singleByteInputReturnsEmptyOutput() {
        assertTrue(magnitudesFromFft(byteArrayOf(5)).isEmpty())
    }

    @Test
    fun dcAndNyquistBinsAreTheirAbsoluteRealValue() {
        // 8 bytes -> 4 bins: bin0 (DC), bin3 (Nyquist), bins 1-2 packed as [re, im].
        val fft = byteArrayOf(10, -20, 0, 0, 0, 0, 0, 0)
        val mags = magnitudesFromFft(fft)
        assertEquals(4, mags.size)
        assertEquals(10f, mags[0])
        assertEquals(20f, mags[3])
    }

    @Test
    fun middleBinsAreRealImaginaryMagnitude() {
        // bin1 packed at fft[2]/fft[3] = (3, 4) -> magnitude 5 (3-4-5 triangle).
        val fft = byteArrayOf(0, 0, 3, 4, 0, 0)
        val mags = magnitudesFromFft(fft)
        assertEquals(3, mags.size)
        assertEquals(5f, mags[1])
    }

    @Test
    fun allZeroInputProducesAllZeroMagnitudes() {
        val mags = magnitudesFromFft(ByteArray(16))
        assertTrue(mags.all { it == 0f })
    }

    @Test
    fun outputLengthIsHalfInputLength() {
        val fft = ByteArray(128) { (it % 100).toByte() }
        assertEquals(64, magnitudesFromFft(fft).size)
    }

    @Test
    fun magnitudeMatchesDirectSqrtComputationForArbitraryBins() {
        val fft = byteArrayOf(0, 0, 6, 8, -9, 12, 0, 0)
        val mags = magnitudesFromFft(fft)
        assertEquals(sqrt(6f * 6f + 8f * 8f), mags[1])
        assertEquals(sqrt(9f * 9f + 12f * 12f), mags[2])
    }
}
