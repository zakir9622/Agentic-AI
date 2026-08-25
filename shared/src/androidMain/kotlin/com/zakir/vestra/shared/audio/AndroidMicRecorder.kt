package com.zakir.vestra.shared.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Short on-device PCM → WAV recorder for Audio Studio voice-change.
 * Caps at [maxDurationMs] so clips stay small enough for the DSP changer.
 */
class AndroidMicRecorder(
    private val outputDir: File,
    private val sampleRate: Int = 22_050,
    private val maxDurationMs: Long = 15_000L,
) {
    private val recording = AtomicBoolean(false)
    private var worker: Thread? = null
    @Volatile
    private var lastPath: String? = null
    @Volatile
    private var lastError: String? = null

    private val _amplitude = MutableStateFlow(0f)
    /** Live RMS amplitude (0f..1f) of the buffer chunk most recently read while recording. */
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    val isRecording: Boolean get() = recording.get()
    val lastRecordingPath: String? get() = lastPath
    val lastFailure: String? get() = lastError

    fun start(): Boolean {
        if (!recording.compareAndSet(false, true)) return false
        lastError = null
        lastPath = null
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) {
            recording.set(false)
            lastError = "Microphone unavailable on this device."
            return false
        }
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        } catch (error: SecurityException) {
            recording.set(false)
            lastError = "Microphone permission denied."
            return false
        } catch (error: Exception) {
            recording.set(false)
            lastError = error.message?.take(120) ?: "Could not open microphone."
            return false
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            recording.set(false)
            lastError = "Microphone failed to initialize."
            return false
        }
        val pcm = ByteArrayOutputStream()
        worker = thread(name = "lookbook-mic", isDaemon = true) {
            try {
                recorder.startRecording()
                val buf = ByteArray(minBuf)
                val deadline = System.currentTimeMillis() + maxDurationMs
                while (recording.get() && System.currentTimeMillis() < deadline) {
                    val n = recorder.read(buf, 0, buf.size)
                    if (n > 0) {
                        pcm.write(buf, 0, n)
                        _amplitude.value = rmsAmplitude(buf, n)
                    }
                }
            } catch (error: Exception) {
                lastError = error.message?.take(120) ?: "Recording failed"
            } finally {
                runCatching {
                    recorder.stop()
                }
                recorder.release()
                recording.set(false)
                _amplitude.value = 0f
                val samples = pcm.toByteArray()
                if (samples.size < sampleRate) {
                    if (lastError == null) lastError = "Recording too short — hold the mic longer."
                    return@thread
                }
                val out = File(outputDir, "mic_${System.currentTimeMillis()}.wav")
                runCatching {
                    WavIo.writePcm16MonoWav(out, samples, sampleRate)
                    lastPath = out.absolutePath
                }.onFailure { err ->
                    lastError = err.message?.take(120) ?: "Could not save recording"
                }
            }
        }
        return true
    }

    fun stop(): String? {
        recording.set(false)
        worker?.join(2_500)
        worker = null
        return lastPath
    }

    fun clear() {
        lastPath = null
        lastError = null
    }

    /** RMS of the 16-bit little-endian PCM samples in `buf[0 until n]`, normalized to 0f..1f. */
    private fun rmsAmplitude(buf: ByteArray, n: Int): Float {
        val sampleCount = n / 2
        if (sampleCount <= 0) return 0f
        var sumSquares = 0.0
        for (i in 0 until sampleCount) {
            val lo = buf[i * 2].toInt() and 0xff
            val hi = buf[i * 2 + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toInt()
            sumSquares += (sample * sample).toDouble()
        }
        val rms = sqrt(sumSquares / sampleCount)
        return (rms / 32768.0).coerceIn(0.0, 1.0).toFloat()
    }
}
