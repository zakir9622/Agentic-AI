package com.zakir.vestra.shared.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlin.math.PI
import kotlin.math.sin

sealed class CalibrationResult {
    data class Measured(val latencyMs: Float) : CalibrationResult()
    data class Unavailable(val reason: String) : CalibrationResult()
}

/**
 * Speaker→mic round-trip latency calibration: plays a short reference tone through [AudioTrack]
 * while simultaneously recording through [AudioRecord], then hands both signals to
 * [LatencyCalibrator]'s cross-correlation core to estimate the delay. Honesty note matching this
 * app's established pattern for hardware-dependent code (see the GPU-delegate fallback in
 * `LiteRtLmEngine`): this class has not been exercised on a real device in this development
 * environment — the cross-correlation core it calls is fully unit-tested with synthetic signals,
 * but simultaneous `AudioTrack`/`AudioRecord` I/O timing is device-dependent and unverified here.
 */
class AndroidLatencyCalibrator(
    private val sampleRate: Int = 22_050,
) {
    private val toneHz = 1200f
    private val toneDurationMs = 300

    fun calibrate(): CalibrationResult {
        val toneSamples = generateTone()
        val recordBufferMs = toneDurationMs + 600 // pad past the tone to catch a slow round trip
        val recordSamples = (sampleRate.toLong() * recordBufferMs / 1000L).toInt()

        val minRecBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minRecBuf <= 0) return CalibrationResult.Unavailable("Microphone unavailable on this device.")

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                (minRecBuf * 2).coerceAtLeast(recordSamples * 2),
            )
        } catch (error: Exception) {
            return CalibrationResult.Unavailable(error.message?.take(120) ?: "Could not open microphone.")
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return CalibrationResult.Unavailable("Microphone failed to initialize.")
        }

        val minPlayBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minPlayBuf, toneSamples.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (error: Exception) {
            recorder.release()
            return CalibrationResult.Unavailable(error.message?.take(120) ?: "Could not open speaker output.")
        }

        return try {
            track.write(toneSamples, 0, toneSamples.size)
            val recorded = ShortArray(recordSamples)
            recorder.startRecording()
            track.play()
            var written = 0
            while (written < recorded.size) {
                val n = recorder.read(recorded, written, recorded.size - written)
                if (n <= 0) break
                written += n
            }
            recorder.stop()
            track.stop()

            val reference = FloatArray(toneSamples.size) { toneSamples[it] / 32768f }
            val recordedFloat = FloatArray(written) { recorded[it] / 32768f }
            if (recordedFloat.size < reference.size) {
                CalibrationResult.Unavailable("Recording ended before the reference tone could align.")
            } else {
                val latencyMs = LatencyCalibrator.estimateLatencyMs(reference, recordedFloat, sampleRate)
                CalibrationResult.Measured(latencyMs)
            }
        } catch (error: Exception) {
            CalibrationResult.Unavailable(error.message?.take(120) ?: "Calibration failed")
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun generateTone(): ShortArray {
        val count = sampleRate * toneDurationMs / 1000
        return ShortArray(count) { i ->
            val fade = minOf(i, count - i, sampleRate / 100).coerceAtLeast(0) / (sampleRate / 100f).coerceAtLeast(1f)
            (sin(2.0 * PI * toneHz * i / sampleRate) * 32000 * fade.coerceIn(0f, 1f)).toInt().toShort()
        }
    }
}
