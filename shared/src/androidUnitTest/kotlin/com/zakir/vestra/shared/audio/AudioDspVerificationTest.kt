package com.zakir.vestra.shared.audio

import com.zakir.vestra.shared.engine.local.AndroidLocalVoiceChanger
import com.zakir.vestra.shared.engine.local.LocalAudioResult
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * D2 (docs/plans/lovable-parity-local-first/PLAN.md): verifies the actual shipped voice-changer
 * DSP (`AndroidLocalVoiceChanger`, applied to every local voice-change result) against real
 * signals — a 440Hz tone shifted +12 semitones should measure near 880Hz, 2x speed should roughly
 * halve duration, extreme knobs must not clip, and default knobs should preserve pitch/length.
 * This is JVM-only (no Android framework calls in `AndroidLocalVoiceChanger` itself), so it runs
 * for real here rather than being an unexecuted `androidTest` stub — unlike `AndroidMicRecorder`/
 * `AndroidLatencyCalibrator`, which do call real `android.media` APIs and are unverified on
 * device (see `docs/DRAWBACKS.md`).
 */
@RunWith(RobolectricTestRunner::class)
class AudioDspVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sampleRate = 22_050

    private fun toneWav(hz: Float, durationMs: Int): java.io.File {
        val sampleCount = sampleRate * durationMs / 1000
        val samples = ShortArray(sampleCount) { i ->
            (sin(2.0 * PI * hz * i / sampleRate) * 20000).toInt().toShort()
        }
        val file = tempFolder.newFile("tone_${System.nanoTime()}.wav")
        WavIo.writePcm16MonoWav(file, samples, sampleRate)
        return file
    }

    private fun transformOrFail(inputPath: String, knobs: VoiceKnobs): PcmWav {
        val changer = AndroidLocalVoiceChanger(tempFolder.newFolder("out_${System.nanoTime()}"))
        val result = changer.transform(inputPath, knobs)
        val ok = result as? LocalAudioResult.Ok
            ?: error("Expected LocalAudioResult.Ok, got $result")
        return WavIo.readPcm16MonoWav(java.io.File(ok.audioPath))
            ?: error("Could not read back the transformed WAV")
    }

    @Test
    fun pitchShiftUpTwelveSemitonesRoughlyDoublesFrequency() {
        val input = toneWav(440f, 500)
        val output = transformOrFail(input.absolutePath, VoiceKnobs(pitchSemitones = 12f))

        val detected = PitchDetector.detectPitchHz(output.samples, output.sampleRate)
        assertTrue(detected != null, "expected a detectable pitch in the shifted output")
        // 5% tolerance, matching the plan's own spec for this test.
        assertTrue(
            kotlin.math.abs(detected - 880f) / 880f < 0.05f,
            "expected ~880Hz after +12 semitones, got ${detected}Hz",
        )
    }

    @Test
    fun pitchShiftDownTwelveSemitonesRoughlyHalvesFrequency() {
        val input = toneWav(440f, 500)
        val output = transformOrFail(input.absolutePath, VoiceKnobs(pitchSemitones = -12f))

        val detected = PitchDetector.detectPitchHz(output.samples, output.sampleRate)
        assertTrue(detected != null, "expected a detectable pitch in the shifted output")
        assertTrue(
            kotlin.math.abs(detected - 220f) / 220f < 0.05f,
            "expected ~220Hz after -12 semitones, got ${detected}Hz",
        )
    }

    @Test
    fun doubleSpeedRoughlyHalvesDuration() {
        val input = toneWav(300f, 1000)
        val output = transformOrFail(input.absolutePath, VoiceKnobs(speed = 2f))

        val inputDurationMs = 1000
        val outputDurationMs = output.samples.size * 1000 / output.sampleRate
        assertTrue(
            kotlin.math.abs(outputDurationMs - inputDurationMs / 2) < inputDurationMs * 0.1,
            "expected ~${inputDurationMs / 2}ms at 2x speed, got ${outputDurationMs}ms",
        )
    }

    @Test
    fun halfSpeedRoughlyDoublesDuration() {
        val input = toneWav(300f, 500)
        val output = transformOrFail(input.absolutePath, VoiceKnobs(speed = 0.5f))

        val inputDurationMs = 500
        val outputDurationMs = output.samples.size * 1000 / output.sampleRate
        assertTrue(
            kotlin.math.abs(outputDurationMs - inputDurationMs * 2) < inputDurationMs * 0.2,
            "expected ~${inputDurationMs * 2}ms at 0.5x speed, got ${outputDurationMs}ms",
        )
    }

    @Test
    fun extremeKnobsNeverClipBeyondSixteenBitRange() {
        val input = toneWav(440f, 500)
        val output = transformOrFail(
            input.absolutePath,
            VoiceKnobs(pitchSemitones = 12f, speed = 2f, formant = 1.5f, warmth = 1f, clarity = 1f),
        )
        for (s in output.samples) {
            assertTrue(
                s >= Short.MIN_VALUE && s <= Short.MAX_VALUE,
                "sample $s exceeded 16-bit PCM range",
            )
        }
    }

    @Test
    fun stereoSixteenBitWavIsAutoConvertedInsteadOfRejected() {
        // Simulates cloud-TTS output that isn't already mono 16-bit — the strict reader used to
        // reject this outright; it should now fall back to readAnyWav + toMono16 and succeed.
        val sampleCount = sampleRate * 500 / 1000
        val interleaved = ShortArray(sampleCount * 2) { i ->
            val sampleIndex = i / 2
            (sin(2.0 * PI * 440f * sampleIndex / sampleRate) * 20000).toInt().toShort()
        }
        val pcmBytes = java.nio.ByteBuffer.allocate(interleaved.size * 2)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        interleaved.forEach { pcmBytes.putShort(it) }
        val file = tempFolder.newFile("stereo_${System.nanoTime()}.wav")
        writeTestWav(file, pcmBytes.array(), sampleRate, channels = 2, bits = 16, format = 1)

        val output = transformOrFail(file.absolutePath, VoiceKnobs.Default)
        assertTrue(output.samples.isNotEmpty(), "expected non-empty output from a downmixed stereo input")
    }

    @Test
    fun monoThirtyTwoBitFloatWavIsAutoConvertedInsteadOfRejected() {
        val sampleCount = sampleRate * 500 / 1000
        val floats = FloatArray(sampleCount) { i -> (sin(2.0 * PI * 440f * i / sampleRate) * 0.6).toFloat() }
        val pcmBytes = java.nio.ByteBuffer.allocate(floats.size * 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        floats.forEach { pcmBytes.putFloat(it) }
        val file = tempFolder.newFile("float_${System.nanoTime()}.wav")
        writeTestWav(file, pcmBytes.array(), sampleRate, channels = 1, bits = 32, format = 3)

        val output = transformOrFail(file.absolutePath, VoiceKnobs.Default)
        assertTrue(output.samples.isNotEmpty(), "expected non-empty output from a mono float32 input")
    }

    @Test
    fun defaultKnobsPreservePitchAndLength() {
        val input = toneWav(330f, 500)
        val inputWav = WavIo.readPcm16MonoWav(input) ?: error("failed to read input fixture")
        val output = transformOrFail(input.absolutePath, VoiceKnobs.Default)

        assertEquals(inputWav.samples.size, output.samples.size, "default knobs must not change sample count")

        val detected = PitchDetector.detectPitchHz(output.samples, output.sampleRate)
        assertTrue(detected != null, "expected a detectable pitch after default-knob processing")
        assertTrue(
            kotlin.math.abs(detected - 330f) / 330f < 0.05f,
            "expected pitch to survive default (near-identity) knobs, got ${detected}Hz",
        )
    }
}
