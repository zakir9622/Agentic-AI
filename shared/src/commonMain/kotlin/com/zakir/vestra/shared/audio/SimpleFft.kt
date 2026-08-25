package com.zakir.vestra.shared.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Minimal iterative radix-2 Cooley-Tukey FFT — no external DSP dependency. Powers
 * `SpectrumScope`'s magnitude bars; playback-side visualization only (the mic level meter uses
 * plain RMS, not a spectrum).
 */
object SimpleFft {

    /**
     * Magnitude spectrum of [samples] (values in -1f..1f). Internally zero-padded/truncated to
     * the next power of two. Returns `n/2` bins spanning 0..sampleRate/2 Hz.
     */
    fun magnitudeSpectrum(samples: FloatArray): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)
        var n = 1
        while (n < samples.size) n = n shl 1
        val real = FloatArray(n)
        val imag = FloatArray(n)
        for (i in samples.indices) real[i] = samples[i]
        fft(real, imag)
        val mags = FloatArray(n / 2)
        for (i in mags.indices) {
            mags[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return mags
    }

    /** In-place iterative FFT; `real`/`imag` must have a power-of-two length. */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wr = cos(ang).toFloat()
            val wi = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curWr = 1f
                var curWi = 0f
                for (k in 0 until len / 2) {
                    val evenR = real[i + k]
                    val evenI = imag[i + k]
                    val oddR = real[i + k + len / 2] * curWr - imag[i + k + len / 2] * curWi
                    val oddI = real[i + k + len / 2] * curWi + imag[i + k + len / 2] * curWr
                    real[i + k] = evenR + oddR
                    imag[i + k] = evenI + oddI
                    real[i + k + len / 2] = evenR - oddR
                    imag[i + k + len / 2] = evenI - oddI
                    val nextWr = curWr * wr - curWi * wi
                    val nextWi = curWr * wi + curWi * wr
                    curWr = nextWr
                    curWi = nextWi
                }
                i += len
            }
            len = len shl 1
        }
    }
}
