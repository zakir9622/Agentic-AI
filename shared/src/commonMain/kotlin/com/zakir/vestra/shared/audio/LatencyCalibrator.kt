package com.zakir.vestra.shared.audio

/**
 * Round-trip audio latency estimation via cross-correlation: play a short reference tone through
 * the speaker while simultaneously recording through the mic, then find the sample offset where
 * the recorded signal best matches the reference. That offset, converted to milliseconds, is the
 * device's speaker→mic round-trip latency — used to size DSP buffers so live monitoring doesn't
 * audibly lag. The device I/O (`AudioTrack`/`AudioRecord`) lives in the Android-specific caller;
 * this is the pure signal-processing core, fully testable with synthetic delayed signals.
 */
object LatencyCalibrator {

    /** Sample offset (in [recorded]) that best aligns with [reference], via normalized cross-correlation. */
    fun estimateOffsetSamples(reference: FloatArray, recorded: FloatArray, maxLagSamples: Int = recorded.size): Int {
        if (reference.isEmpty() || recorded.isEmpty()) return 0
        var bestLag = 0
        var bestScore = Float.NEGATIVE_INFINITY
        val maxLag = maxLagSamples.coerceIn(0, recorded.size - 1)
        for (lag in 0..maxLag) {
            val n = minOf(reference.size, recorded.size - lag)
            if (n <= 0) continue
            var sum = 0f
            for (i in 0 until n) {
                sum += reference[i] * recorded[i + lag]
            }
            val score = sum / n
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        return bestLag
    }

    fun estimateLatencyMs(reference: FloatArray, recorded: FloatArray, sampleRate: Int): Float {
        if (sampleRate <= 0) return 0f
        val lag = estimateOffsetSamples(reference, recorded)
        return lag * 1000f / sampleRate
    }
}
