package com.zakir.vestra.shared.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LatencyCalibratorTest {

    private val sampleRate = 8_000

    private fun chirp(size: Int, startHz: Float = 400f, endHz: Float = 2000f): FloatArray =
        FloatArray(size) { i ->
            val t = i.toFloat() / sampleRate
            val hz = startHz + (endHz - startHz) * i / size
            sin(2.0 * PI * hz * t).toFloat()
        }

    @Test
    fun finds_a_known_delay_with_silence_padding() {
        val reference = chirp(1600)
        val delaySamples = 240
        val recorded = FloatArray(reference.size + delaySamples)
        reference.copyInto(recorded, destinationOffset = delaySamples)

        val estimated = LatencyCalibrator.estimateOffsetSamples(reference, recorded)
        assertTrue(
            kotlin.math.abs(estimated - delaySamples) <= 2,
            "expected offset near $delaySamples, got $estimated",
        )
    }

    @Test
    fun zero_delay_is_detected_as_zero() {
        val reference = chirp(1200)
        val recorded = reference.copyOf()
        assertEquals(0, LatencyCalibrator.estimateOffsetSamples(reference, recorded))
    }

    @Test
    fun latency_ms_matches_the_sample_offset() {
        val reference = chirp(1600)
        val delaySamples = 400 // 50ms at 8kHz
        val recorded = FloatArray(reference.size + delaySamples)
        reference.copyInto(recorded, destinationOffset = delaySamples)

        val ms = LatencyCalibrator.estimateLatencyMs(reference, recorded, sampleRate)
        assertTrue(kotlin.math.abs(ms - 50f) < 2f, "expected ~50ms, got ${ms}ms")
    }

    @Test
    fun empty_signals_do_not_crash() {
        assertEquals(0, LatencyCalibrator.estimateOffsetSamples(FloatArray(0), FloatArray(0)))
        assertEquals(0, LatencyCalibrator.estimateOffsetSamples(floatArrayOf(1f), FloatArray(0)))
    }
}
