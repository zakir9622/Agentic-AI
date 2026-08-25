package com.zakir.vestra.shared.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PitchMatcherTest {

    private val sampleRate = 22_050

    private fun toneShorts(hz: Float, size: Int = 4096): ShortArray =
        ShortArray(size) { i -> (sin(2.0 * PI * hz * i / sampleRate) * 30000).toInt().toShort() }

    @Test
    fun matching_440_to_220_suggests_minus_12_semitones() {
        // docs/plans/lovable-parity-local-first/PLAN.md's own worked example for this test.
        val result = PitchMatcher.match(
            sourceSamples = toneShorts(440f),
            targetSamples = toneShorts(220f),
            sampleRate = sampleRate,
        )
        val matched = result as? PitchMatchResult.Matched
        assertTrue(matched != null, "expected a Matched result, got $result")
        assertEquals(-12, matched.semitones)
    }

    @Test
    fun matching_the_same_pitch_suggests_zero_shift() {
        val result = PitchMatcher.match(
            sourceSamples = toneShorts(330f),
            targetSamples = toneShorts(330f),
            sampleRate = sampleRate,
        )
        val matched = result as? PitchMatchResult.Matched
        assertTrue(matched != null)
        assertEquals(0, matched.semitones)
    }

    @Test
    fun silence_is_reported_as_unavailable_not_a_crash() {
        val result = PitchMatcher.match(
            sourceSamples = ShortArray(4096),
            targetSamples = toneShorts(220f),
            sampleRate = sampleRate,
        )
        assertTrue(result is PitchMatchResult.Unavailable)
    }
}
