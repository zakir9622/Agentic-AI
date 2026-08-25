package com.zakir.vestra.shared.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleFftTest {

    @Test
    fun peak_bin_matches_a_known_sine_frequency() {
        val sampleRate = 8_000
        val size = 1024 // already a power of two
        val toneHz = 1000f // exact bin: 1000 * 1024 / 8000 = 128
        val samples = FloatArray(size) { i -> sin(2.0 * PI * toneHz * i / sampleRate).toFloat() }

        val mags = SimpleFft.magnitudeSpectrum(samples)
        assertEquals(size / 2, mags.size)

        val peakBin = mags.indices.maxBy { mags[it] }
        val expectedBin = (toneHz * size / sampleRate).toInt()
        assertTrue(
            kotlin.math.abs(peakBin - expectedBin) <= 1,
            "expected peak near bin $expectedBin, got $peakBin",
        )
    }

    @Test
    fun pads_non_power_of_two_input() {
        val samples = FloatArray(1000) { i -> if (i % 17 == 0) 1f else 0f }
        val mags = SimpleFft.magnitudeSpectrum(samples)
        assertEquals(512, mags.size) // next power of two above 1000 is 1024
    }

    @Test
    fun silence_produces_a_flat_zero_spectrum() {
        val mags = SimpleFft.magnitudeSpectrum(FloatArray(256))
        assertTrue(mags.all { it == 0f })
    }

    @Test
    fun empty_input_returns_empty_spectrum() {
        assertEquals(0, SimpleFft.magnitudeSpectrum(FloatArray(0)).size)
    }
}
