package com.zakir.vestra.shared.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

data class PcmWav(val samples: ShortArray, val sampleRate: Int)

/** Every sample normalized to -1f..1f, interleaved by channel — see [WavIo.readAnyWav]. */
data class RawWav(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val format: Int,
)

/** Minimal mono 16-bit PCM WAV read/write — shared by the mic recorder and every local DSP path. */
object WavIo {
    fun readPcm16MonoWav(file: File): PcmWav? {
        val bytes = file.readBytes()
        if (bytes.size < 44) return null
        if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null
        var offset = 12
        var sampleRate = 22050
        var channels = 1
        var bits = 16
        var dataOffset = -1
        var dataSize = 0
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4)
            val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val next = offset + 8 + size
            when (id) {
                "fmt " -> {
                    val bb = ByteBuffer.wrap(bytes, offset + 8, size).order(ByteOrder.LITTLE_ENDIAN)
                    val format = bb.short.toInt() and 0xffff
                    channels = bb.short.toInt() and 0xffff
                    sampleRate = bb.int
                    bb.int // byte rate
                    bb.short // block align
                    bits = bb.short.toInt() and 0xffff
                    if (format != 1 || channels != 1 || bits != 16) return null
                }
                "data" -> {
                    dataOffset = offset + 8
                    dataSize = size
                }
            }
            offset = next + (size % 2) // word align
            if (dataOffset >= 0 && id == "data") break
        }
        if (dataOffset < 0 || dataSize <= 0) return null
        val sampleCount = dataSize / 2
        val samples = ShortArray(sampleCount)
        val bb = ByteBuffer.wrap(bytes, dataOffset, sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) samples[i] = bb.short
        return PcmWav(samples, sampleRate)
    }

    /**
     * Permissive counterpart to [readPcm16MonoWav] — accepts PCM 8/16/24/32-bit and IEEE-float
     * 32-bit, at any channel count, instead of hard-rejecting anything that isn't already mono
     * 16-bit. Cloud TTS providers commonly save stereo or 24/32-bit WAV; use this + [toMono16]
     * as a fallback so on-device DSP paths that require mono 16-bit (e.g. the voice changer)
     * can still process that audio instead of failing outright.
     */
    fun readAnyWav(file: File): RawWav? {
        val bytes = file.readBytes()
        if (bytes.size < 44) return null
        if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null
        var offset = 12
        var sampleRate = 22050
        var channels = 1
        var bits = 16
        var format = 1
        var sawFmt = false
        var dataOffset = -1
        var dataSize = 0
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4)
            val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (size < 0 || offset + 8 + size > bytes.size) break
            val next = offset + 8 + size
            when (id) {
                "fmt " -> {
                    // A truncated/malformed fmt chunk (size < the 16-byte fixed fields this reads)
                    // would otherwise overrun the wrapped ByteBuffer and throw instead of failing
                    // gracefully like every other rejection path here.
                    if (size < 16) return null
                    val bb = ByteBuffer.wrap(bytes, offset + 8, size).order(ByteOrder.LITTLE_ENDIAN)
                    format = bb.short.toInt() and 0xffff
                    channels = bb.short.toInt() and 0xffff
                    sampleRate = bb.int
                    bb.int // byte rate
                    bb.short // block align
                    bits = bb.short.toInt() and 0xffff
                    sawFmt = true
                }
                "data" -> {
                    dataOffset = offset + 8
                    dataSize = size
                }
            }
            offset = next + (size % 2) // word align
            // Some encoders write "data" before "fmt " — keep walking chunks until both are
            // found (or the file runs out) instead of stopping at the first "data" and silently
            // parsing with the un-set defaults above.
            if (sawFmt && dataOffset >= 0) break
        }
        if (!sawFmt || dataOffset < 0 || dataSize <= 0 || channels <= 0) return null
        // Only uncompressed PCM (1) and IEEE-float (3) are supported — A-law/mu-law/ADPCM etc.
        // are rare enough from cloud TTS output that surfacing the existing error for them is fine.
        if (format != 1 && format != 3) return null
        if (format == 3 && bits != 32) return null
        if (format == 1 && bits != 8 && bits != 16 && bits != 24 && bits != 32) return null
        val bytesPerSample = bits / 8
        val sampleCount = dataSize / bytesPerSample
        if (sampleCount <= 0) return null
        val bb = ByteBuffer.wrap(bytes, dataOffset, sampleCount * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)
        val samples = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            samples[i] = when {
                format == 3 -> bb.float
                bits == 8 -> ((bb.get().toInt() and 0xff) - 128) / 128f
                bits == 16 -> bb.short / 32768f
                bits == 24 -> {
                    val b0 = bb.get().toInt() and 0xff
                    val b1 = bb.get().toInt() and 0xff
                    val b2 = bb.get().toInt() and 0xff
                    var v = b0 or (b1 shl 8) or (b2 shl 16)
                    if (v and 0x800000 != 0) v = v or -0x1000000 // sign-extend to negative Int
                    v / 8388608f
                }
                else -> bb.int / 2147483648f // bits == 32, format == 1
            }
        }
        return RawWav(samples, sampleRate, channels, bits, format)
    }

    /** Downmixes (averages channels) and normalizes to mono 16-bit PCM — pairs with [readAnyWav]. */
    fun toMono16(raw: RawWav): PcmWav {
        if (raw.channels <= 1) {
            val out = ShortArray(raw.samples.size) { i -> raw.samples[i].toPcm16() }
            return PcmWav(out, raw.sampleRate)
        }
        val frames = raw.samples.size / raw.channels
        val out = ShortArray(frames)
        for (frame in 0 until frames) {
            var sum = 0f
            for (ch in 0 until raw.channels) sum += raw.samples[frame * raw.channels + ch]
            out[frame] = (sum / raw.channels).toPcm16()
        }
        return PcmWav(out, raw.sampleRate)
    }

    private fun Float.toPcm16(): Short =
        (this * 32767f).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    fun writePcm16MonoWav(file: File, samples: ShortArray, sampleRate: Int) =
        writePcm16Wav(file, samples, sampleRate, channels = 1)

    /** Interleaved 16-bit PCM WAV writer at any channel count — pairs with [readAnyWav]/[toMono16]. */
    fun writePcm16Wav(file: File, samples: ShortArray, sampleRate: Int, channels: Int) {
        val dataSize = samples.size * 2
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            fun writeString(s: String) = dos.writeBytes(s)
            fun writeIntLE(v: Int) {
                dos.write(v and 0xff)
                dos.write((v shr 8) and 0xff)
                dos.write((v shr 16) and 0xff)
                dos.write((v shr 24) and 0xff)
            }
            fun writeShortLE(v: Int) {
                dos.write(v and 0xff)
                dos.write((v shr 8) and 0xff)
            }
            writeString("RIFF")
            writeIntLE(36 + dataSize)
            writeString("WAVE")
            writeString("fmt ")
            writeIntLE(16)
            writeShortLE(1)
            writeShortLE(channels)
            writeIntLE(sampleRate)
            writeIntLE(sampleRate * channels * 2)
            writeShortLE(channels * 2)
            writeShortLE(16)
            writeString("data")
            writeIntLE(dataSize)
            for (s in samples) writeShortLE(s.toInt())
        }
        file.parentFile?.mkdirs()
        file.writeBytes(out.toByteArray())
    }

    fun writePcm16MonoWav(file: File, pcm: ByteArray, sampleRate: Int) {
        val dataSize = pcm.size
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            fun writeString(s: String) = dos.writeBytes(s)
            fun writeIntLE(v: Int) {
                dos.write(v and 0xff)
                dos.write((v shr 8) and 0xff)
                dos.write((v shr 16) and 0xff)
                dos.write((v shr 24) and 0xff)
            }
            fun writeShortLE(v: Int) {
                dos.write(v and 0xff)
                dos.write((v shr 8) and 0xff)
            }
            writeString("RIFF")
            writeIntLE(36 + dataSize)
            writeString("WAVE")
            writeString("fmt ")
            writeIntLE(16)
            writeShortLE(1)
            writeShortLE(1)
            writeIntLE(sampleRate)
            writeIntLE(sampleRate * 2)
            writeShortLE(2)
            writeShortLE(16)
            writeString("data")
            writeIntLE(dataSize)
            dos.write(pcm)
        }
        file.parentFile?.mkdirs()
        file.writeBytes(out.toByteArray())
    }
}
