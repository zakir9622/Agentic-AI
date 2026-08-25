package com.zakir.vestra.shared.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PitchDetectorTest {

    private fun sineFrame(hz: Float, sampleRate: Int, size: Int): FloatArray =
        FloatArray(size) { i -> sin(2.0 * PI * hz * i / sampleRate).toFloat() }

    @Test
    fun detects_a_clean_sine_within_tolerance() {
        val sampleRate = 22_050
        val frame = sineFrame(220f, sampleRate, 4096)
        val detected = PitchDetector.detectPitchHz(frame, sampleRate)
        assertTrue(detected != null, "expected a detected pitch for a clean 220Hz tone")
        assertTrue(kotlin.math.abs(detected - 220f) < 5f, "detected $detected, expected ~220Hz")
    }

    @Test
    fun detects_a_higher_sine_within_tolerance() {
        val sampleRate = 22_050
        val frame = sineFrame(440f, sampleRate, 4096)
        val detected = PitchDetector.detectPitchHz(frame, sampleRate)
        assertTrue(detected != null, "expected a detected pitch for a clean 440Hz tone")
        assertTrue(kotlin.math.abs(detected - 440f) < 8f, "detected $detected, expected ~440Hz")
    }

    @Test
    fun returns_null_for_silence() {
        val silence = FloatArray(4096)
        assertNull(PitchDetector.detectPitchHz(silence, 22_050))
    }

    @Test
    fun semitone_difference_matches_music_theory() {
        // An octave down is exactly -12 semitones, regardless of the reference frequency.
        assertApproximately(-12f, PitchDetector.semitoneDifference(440f, 220f))
        assertApproximately(12f, PitchDetector.semitoneDifference(220f, 440f))
        assertApproximately(0f, PitchDetector.semitoneDifference(440f, 440f))
    }

    @Test
    fun semitone_difference_rounded_matches_the_plan_example() {
        // docs/plans/lovable-parity-local-first/PLAN.md's own worked example.
        assertEquals(-12, PitchDetector.semitoneDifferenceRounded(440f, 220f))
    }

    @Test
    fun short_array_overload_matches_the_float_overload() {
        val sampleRate = 22_050
        val samples = ShortArray(4096) { i ->
            (sin(2.0 * PI * 330.0 * i / sampleRate) * 32000).toInt().toShort()
        }
        val detected = PitchDetector.detectPitchHz(samples, sampleRate)
        assertTrue(detected != null)
        assertTrue(kotlin.math.abs(detected - 330f) < 8f, "detected $detected, expected ~330Hz")
    }
}

private fun assertApproximately(expected: Float, actual: Float, tolerance: Float = 0.01f) {
    assertTrue(
        kotlin.math.abs(expected - actual) <= tolerance,
        "expected $expected within $tolerance of $actual",
    )
}
