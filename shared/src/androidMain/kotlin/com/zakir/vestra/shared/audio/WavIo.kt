package com.zakir.vestra.shared.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PcmWav(val samples: ShortArray, val sampleRate: Int)

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

    fun writePcm16MonoWav(file: File, samples: ShortArray, sampleRate: Int) {
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
            writeShortLE(1)
            writeIntLE(sampleRate)
            writeIntLE(sampleRate * 2)
            writeShortLE(2)
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
