package com.zakir.vestra.shared.audio

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Converts the raw byte layout Android's `android.media.audiofx.Visualizer.getFft()` returns
 * into per-bin magnitudes `SpectrumScope` can render.
 *
 * Per the platform's documented layout: byte 0 is the real part of the DC (0Hz) bin, byte 1 is
 * the real part of the Nyquist bin (both purely real, no imaginary counterpart), and each
 * remaining bin `i` (1 until bins-1) is packed as `[real, imaginary]` at `fft[2*i]`/`fft[2*i+1]`.
 * Kept as a pure function — independent of `android.media` — so the conversion itself is
 * unit-testable on the JVM without a device; only the live capture registration is platform code.
 */
fun magnitudesFromFft(fft: ByteArray): FloatArray {
    if (fft.size < 2) return FloatArray(0)
    val bins = fft.size / 2
    val out = FloatArray(bins)
    out[0] = abs(fft[0].toFloat())
    if (bins > 1) out[bins - 1] = abs(fft[1].toFloat())
    var i = 1
    while (i < bins - 1) {
        val re = fft[2 * i].toFloat()
        val im = fft[2 * i + 1].toFloat()
        out[i] = sqrt(re * re + im * im)
        i++
    }
    return out
}
