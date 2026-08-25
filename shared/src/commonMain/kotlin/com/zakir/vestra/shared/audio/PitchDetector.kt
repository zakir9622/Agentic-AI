package com.zakir.vestra.shared.audio

import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Autocorrelation-based fundamental-frequency detector — no neural model, pure signal
 * processing. Matches lookbookweb's `analyseVoice()` approach: find the lag that maximizes
 * self-similarity within a plausible human-voice pitch range, then convert lag → Hz.
 */
object PitchDetector {

    /**
     * Detects the fundamental frequency of one windowed [frame] of mono samples in -1f..1f.
     * Returns null when no lag in [minHz]..[maxHz] shows meaningful periodicity (silence/noise).
     */
    fun detectPitchHz(
        frame: FloatArray,
        sampleRate: Int,
        minHz: Float = 60f,
        maxHz: Float = 900f,
    ): Float? {
        if (frame.size < 4) return null
        val minLag = (sampleRate / maxHz).toInt().coerceAtLeast(1)
        val maxLag = (sampleRate / minHz).toInt().coerceAtMost(frame.size - 1)
        if (maxLag <= minLag) return null

        // Normalize against zero-lag energy so silence/noise doesn't report a spurious pitch.
        var energy = 0f
        for (v in frame) energy += v * v
        if (energy <= 1e-6f) return null

        var bestLag = -1
        var bestCorr = 0f
        for (lag in minLag..maxLag) {
            var sum = 0f
            for (i in 0 until frame.size - lag) {
                sum += frame[i] * frame[i + lag]
            }
            if (sum > bestCorr) {
                bestCorr = sum
                bestLag = lag
            }
        }
        if (bestLag <= 0 || bestCorr / energy < 0.1f) return null
        return sampleRate.toFloat() / bestLag
    }

    /** Detects pitch from 16-bit PCM samples, using a centered window of up to [frameSize]. */
    fun detectPitchHz(samples: ShortArray, sampleRate: Int, frameSize: Int = 4096): Float? {
        if (samples.isEmpty()) return null
        val start = ((samples.size - frameSize) / 2).coerceAtLeast(0)
        val end = (start + frameSize).coerceAtMost(samples.size)
        val frame = FloatArray(end - start) { samples[start + it] / 32768f }
        return detectPitchHz(frame, sampleRate)
    }

    /** Semitone shift needed to move [sourceHz] to [targetHz]; positive = pitch up. */
    fun semitoneDifference(sourceHz: Float, targetHz: Float): Float {
        require(sourceHz > 0f && targetHz > 0f) { "Frequencies must be positive" }
        return 12f * log2(targetHz / sourceHz)
    }

    /** Convenience: rounds to the nearest whole semitone, matching [VoiceKnobs.pitchSemitones]'s UI granularity. */
    fun semitoneDifferenceRounded(sourceHz: Float, targetHz: Float): Int =
        semitoneDifference(sourceHz, targetHz).roundToInt()
}
