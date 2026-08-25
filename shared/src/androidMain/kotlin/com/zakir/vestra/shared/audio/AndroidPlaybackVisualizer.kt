package com.zakir.vestra.shared.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live FFT capture of a currently-playing audio session for the Audio Studio's playback
 * spectrum scope. Wraps `android.media.audiofx.Visualizer`, which taps the mix output of a
 * given `audioSessionId` (e.g. a `MediaPlayer`'s) system-side — no separate microphone capture
 * involved, so this needs no extra runtime permission beyond what the app already declares.
 *
 * The byte-to-magnitude conversion itself is the pure, unit-tested [magnitudesFromFft] — this
 * class only owns the platform capture lifecycle. Device-timing/behavior of the capture callback
 * itself is unverified in this environment (no device — see `docs/DRAWBACKS.md`); the conversion
 * math is real and tested.
 */
class AndroidPlaybackVisualizer(private val audioSessionId: Int) {

    private val _magnitudes = MutableStateFlow(FloatArray(0))
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    private var visualizer: Visualizer? = null

    /** Starts FFT capture. Returns false (and leaves [magnitudes] empty) if unsupported here. */
    fun start(): Boolean {
        return runCatching {
            val captureSize = Visualizer.getCaptureSizeRange()[1]
            val rate = Visualizer.getMaxCaptureRate().coerceIn(0, 20_000)
            val v = Visualizer(audioSessionId)
            v.captureSize = captureSize
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (fft != null) _magnitudes.value = magnitudesFromFft(fft)
                    }
                },
                rate,
                false,
                true,
            )
            v.enabled = true
            visualizer = v
            true
        }.getOrElse { false }
    }

    fun stop() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
        _magnitudes.value = FloatArray(0)
    }
}
