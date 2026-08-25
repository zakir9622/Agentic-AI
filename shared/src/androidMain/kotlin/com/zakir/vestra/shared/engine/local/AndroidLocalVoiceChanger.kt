package com.zakir.vestra.shared.engine.local

import com.zakir.vestra.shared.audio.VoiceKnobs
import com.zakir.vestra.shared.audio.WavIo
import java.io.File
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Offline voice changer — true on-device DSP (pitch, speed, formant, warmth, clarity)
 * on mono 16-bit WAV. No neural pack required.
 */
class AndroidLocalVoiceChanger(
    private val outputDir: File,
) : LocalVoiceChanger {

    override fun isReady(): Boolean = true

    override fun transform(inputPath: String, knobs: VoiceKnobs): LocalAudioResult {
        val k = knobs.sanitized()
        val input = File(inputPath)
        if (!input.isFile) {
            return LocalAudioResult.Unavailable("Audio file missing: $inputPath")
        }
        return runCatching {
            val wav = WavIo.readPcm16MonoWav(input)
                ?: return LocalAudioResult.Unavailable(
                    "Voice changer needs mono 16-bit WAV (cloud TTS saves WAV). Re-generate or convert.",
                )
            var samples = wav.samples
            samples = applyPitchAndSpeed(samples, k.pitchSemitones, k.speed, k.formant)
            samples = applyTone(samples, k.warmth, k.clarity)
            val out = File(outputDir, "voice_${System.currentTimeMillis()}.wav")
            WavIo.writePcm16MonoWav(out, samples, wav.sampleRate)
            LocalAudioResult.Ok(out.absolutePath)
        }.getOrElse { err ->
            LocalAudioResult.Unavailable(err.message?.take(160) ?: "Voice changer failed")
        }
    }

    /**
     * Combined pitch + speed + formant via resample ratio.
     * pitch↑ shortens period; formant scales independently as a mild ratio bias.
     */
    private fun applyPitchAndSpeed(
        samples: ShortArray,
        pitchSemitones: Float,
        speed: Float,
        formant: Float,
    ): ShortArray {
        val pitchRatio = 2.0.pow((pitchSemitones / 12.0)).toFloat()
        val readStep = (pitchRatio * formant.coerceIn(0.85f, 1.15f) / speed.coerceAtLeast(0.01f))
            .coerceIn(0.25f, 4f)
        val outLen = (samples.size / readStep).roundToInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        var pos = 0.0
        for (i in 0 until outLen) {
            val idx = pos.toInt().coerceIn(0, samples.size - 1)
            val frac = (pos - idx).toFloat()
            val a = samples[idx].toInt()
            val b = samples[(idx + 1).coerceAtMost(samples.size - 1)].toInt()
            out[i] = (a + (b - a) * frac).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            pos += readStep
            if (pos >= samples.size - 1) break
        }
        return out
    }

    /** Simple shelving EQ: warmth boosts lows, clarity boosts highs. */
    private fun applyTone(samples: ShortArray, warmth: Float, clarity: Float): ShortArray {
        val out = ShortArray(samples.size)
        var low = 0f
        var high = 0f
        val lowAlpha = 0.08f + warmth * 0.12f
        val highAlpha = 0.15f + clarity * 0.25f
        val lowGain = 0.7f + warmth * 0.8f
        val highGain = 0.7f + clarity * 0.9f
        for (i in samples.indices) {
            val x = samples[i] / 32768f
            low += lowAlpha * (x - low)
            high = highAlpha * (x - low) + (1f - highAlpha) * high
            val y = (low * lowGain + high * highGain).coerceIn(-1f, 1f)
            out[i] = (y * 32767f).roundToInt().toShort()
        }
        // Tiny DC block
        var mean = 0.0
        for (s in out) mean += s
        mean /= out.size.coerceAtLeast(1)
        if (kotlin.math.abs(mean) > 1.0) {
            val m = mean.roundToInt()
            for (i in out.indices) {
                out[i] = (out[i] - m).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }
}
