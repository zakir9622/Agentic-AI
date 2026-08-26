package com.zakir.vestra.shared.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Covers [WavIo.readAnyWav]/[WavIo.toMono16] against synthetic fixtures the strict
 * [WavIo.readPcm16MonoWav] reader would reject — stereo, 24-bit, and 32-bit float WAV — since
 * these are exactly the shapes cloud-TTS output can take (see `AndroidLocalVoiceChanger`).
 */
class WavIoTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeWav(pcm: ByteArray, sampleRate: Int, channels: Int, bits: Int, format: Int): java.io.File {
        val file = tempFolder.newFile("wavio_${System.nanoTime()}.wav")
        writeTestWav(file, pcm, sampleRate, channels, bits, format)
        return file
    }

    @Test
    fun readAnyWavParsesStereoSixteenBitPcm() {
        // Left channel silent, right channel at half scale — verifies channel order/downmix math.
        val frames = 100
        val pcm = ByteBuffer.allocate(frames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { pcm.putShort(0); pcm.putShort(16384) }
        val file = writeWav(pcm.array(), sampleRate = 22050, channels = 2, bits = 16, format = 1)

        val raw = WavIo.readAnyWav(file)
        assertTrue(raw != null, "expected stereo 16-bit WAV to parse")
        assertEquals(2, raw.channels)
        assertEquals(16, raw.bitsPerSample)
        assertEquals(frames * 2, raw.samples.size)
        assertEquals(0f, raw.samples[0], 0.001f)
        assertEquals(0.5f, raw.samples[1], 0.001f)

        val mono = WavIo.toMono16(raw)
        assertEquals(frames, mono.samples.size)
        // (0 + 16384) / 2 normalized ≈ 0.25 of full scale → ~8192
        assertTrue(kotlin.math.abs(mono.samples[0] - 8192) < 50, "expected downmix average, got ${mono.samples[0]}")
    }

    @Test
    fun readAnyWavParsesMonoTwentyFourBitPcmWithSignExtension() {
        // Hand-built 24-bit samples: max positive, max negative, and a small negative value —
        // exercises the sign-extension path explicitly (the easiest place to get this wrong).
        val samples24 = listOf(0x7fffff, -1, -100)
        val pcm = ByteArrayOutputStream()
        for (s in samples24) {
            pcm.write(s and 0xff)
            pcm.write((s shr 8) and 0xff)
            pcm.write((s shr 16) and 0xff)
        }
        val file = writeWav(pcm.toByteArray(), sampleRate = 44100, channels = 1, bits = 24, format = 1)

        val raw = WavIo.readAnyWav(file)
        assertTrue(raw != null, "expected mono 24-bit WAV to parse")
        assertEquals(24, raw.bitsPerSample)
        assertEquals(3, raw.samples.size)
        assertEquals(1f, raw.samples[0], 0.001f, "max positive 24-bit value should normalize to ~1.0")
        assertEquals(-1f / 8388608f, raw.samples[1], 0.0001f, "-1 (all-ones) should sign-extend to a tiny negative value")
        assertEquals(-100f / 8388608f, raw.samples[2], 0.0001f)
    }

    @Test
    fun readAnyWavParsesMonoThirtyTwoBitFloat() {
        val values = floatArrayOf(0f, 0.5f, -0.5f, 1f, -1f)
        val pcm = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { pcm.putFloat(it) }
        val file = writeWav(pcm.array(), sampleRate = 48000, channels = 1, bits = 32, format = 3)

        val raw = WavIo.readAnyWav(file)
        assertTrue(raw != null, "expected mono float32 WAV to parse")
        assertEquals(values.toList(), raw.samples.toList())
    }

    @Test
    fun readAnyWavRejectsUnsupportedCompressedFormat() {
        // format=6 is A-law — deliberately unsupported; must fail gracefully (null), not throw.
        val file = writeWav(ByteArray(10), sampleRate = 8000, channels = 1, bits = 8, format = 6)
        assertNull(WavIo.readAnyWav(file))
    }

    @Test
    fun readAnyWavRejectsTruncatedFmtChunkInsteadOfThrowing() {
        // A "fmt " chunk declaring less than the 16 fixed bytes readAnyWav parses — must fail
        // gracefully (null), not throw BufferUnderflowException.
        val out = ByteArrayOutputStream()
        java.io.DataOutputStream(out).use { dos ->
            fun writeString(s: String) = dos.writeBytes(s)
            fun writeIntLE(v: Int) {
                dos.write(v and 0xff); dos.write((v shr 8) and 0xff)
                dos.write((v shr 16) and 0xff); dos.write((v shr 24) and 0xff)
            }
            writeString("RIFF")
            writeIntLE(28)
            writeString("WAVE")
            writeString("fmt ")
            writeIntLE(4) // declares only 4 bytes — truncated
            dos.write(ByteArray(4))
            writeString("data")
            writeIntLE(0)
        }
        val file = tempFolder.newFile("truncated_fmt_${System.nanoTime()}.wav")
        file.writeBytes(out.toByteArray())
        assertNull(WavIo.readAnyWav(file))
    }

    @Test
    fun readAnyWavParsesFmtChunkEvenWhenDataChunkComesFirst() {
        // Non-standard chunk ordering (data before fmt) — must not stop at the first "data" chunk
        // and silently fall back to the unset defaults.
        val out = ByteArrayOutputStream()
        val pcm = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        pcm.putShort(1000); pcm.putShort(2000)
        java.io.DataOutputStream(out).use { dos ->
            fun writeString(s: String) = dos.writeBytes(s)
            fun writeIntLE(v: Int) {
                dos.write(v and 0xff); dos.write((v shr 8) and 0xff)
                dos.write((v shr 16) and 0xff); dos.write((v shr 24) and 0xff)
            }
            fun writeShortLE(v: Int) {
                dos.write(v and 0xff); dos.write((v shr 8) and 0xff)
            }
            writeString("RIFF")
            writeIntLE(100)
            writeString("WAVE")
            writeString("data")
            writeIntLE(4)
            dos.write(pcm.array())
            writeString("fmt ")
            writeIntLE(16)
            writeShortLE(1) // PCM
            writeShortLE(1) // mono
            writeIntLE(48000)
            writeIntLE(48000 * 2)
            writeShortLE(2)
            writeShortLE(16)
        }
        val file = tempFolder.newFile("data_before_fmt_${System.nanoTime()}.wav")
        file.writeBytes(out.toByteArray())

        val raw = WavIo.readAnyWav(file)
        assertTrue(raw != null, "expected the fmt chunk after data to still be parsed")
        assertEquals(48000, raw.sampleRate, "sampleRate should come from fmt, not the unset 22050 default")
        assertEquals(1, raw.channels)
    }

    @Test
    fun toMono16PassesThroughAlreadyMonoAudioUnchanged() {
        val raw = RawWav(
            samples = floatArrayOf(0f, 0.5f, -0.5f),
            sampleRate = 22050,
            channels = 1,
            bitsPerSample = 16,
            format = 1,
        )
        val mono = WavIo.toMono16(raw)
        assertEquals(0, mono.samples[0])
        assertTrue(kotlin.math.abs(mono.samples[1] - 16384) <= 1, "expected ~16384, got ${mono.samples[1]}")
        assertTrue(kotlin.math.abs(mono.samples[2] + 16384) <= 1, "expected ~-16384, got ${mono.samples[2]}")
    }
}
