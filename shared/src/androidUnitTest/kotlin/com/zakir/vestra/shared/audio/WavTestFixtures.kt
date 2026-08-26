package com.zakir.vestra.shared.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Writes a WAV with an arbitrary channel count/bit depth/format — [WavIo] only ever writes mono
 * 16-bit PCM, so tests that need other shapes (stereo, 24-bit, float) to exercise
 * [WavIo.readAnyWav] build their own fixture with this. Shared by [WavIoTest] and
 * [AudioDspVerificationTest] rather than duplicated in each.
 */
internal fun writeTestWav(
    outFile: File,
    pcm: ByteArray,
    sampleRate: Int,
    channels: Int,
    bits: Int,
    format: Int,
) {
    val blockAlign = channels * (bits / 8)
    val out = ByteArrayOutputStream()
    DataOutputStream(out).use { dos ->
        fun writeString(s: String) = dos.writeBytes(s)
        fun writeIntLE(v: Int) {
            dos.write(v and 0xff); dos.write((v shr 8) and 0xff)
            dos.write((v shr 16) and 0xff); dos.write((v shr 24) and 0xff)
        }
        fun writeShortLE(v: Int) {
            dos.write(v and 0xff); dos.write((v shr 8) and 0xff)
        }
        writeString("RIFF")
        writeIntLE(36 + pcm.size)
        writeString("WAVE")
        writeString("fmt ")
        writeIntLE(16)
        writeShortLE(format)
        writeShortLE(channels)
        writeIntLE(sampleRate)
        writeIntLE(sampleRate * blockAlign)
        writeShortLE(blockAlign)
        writeShortLE(bits)
        writeString("data")
        writeIntLE(pcm.size)
        dos.write(pcm)
    }
    outFile.writeBytes(out.toByteArray())
}
