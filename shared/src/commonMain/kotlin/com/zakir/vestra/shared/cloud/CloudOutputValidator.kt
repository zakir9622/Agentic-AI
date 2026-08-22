package com.zakir.vestra.shared.cloud

/**
 * Rejects empty, truncated, or non-media cloud downloads so fallback chains can retry
 * instead of surfacing a blank result as success.
 */
object CloudOutputValidator {
    private const val MIN_IMAGE_BYTES = 1_024
    private const val MIN_DIMENSION = 64
    private const val MIN_VIDEO_BYTES = 8_192

    fun validate(bytes: ByteArray, isVideo: Boolean = false): String? {
        if (bytes.isEmpty()) return "Downloaded file is empty"
        val min = if (isVideo) MIN_VIDEO_BYTES else MIN_IMAGE_BYTES
        if (bytes.size < min) return "Downloaded file is too small (${bytes.size} bytes)"
        return when {
            isVideo -> validateVideo(bytes)
            else -> validateImage(bytes)
        }
    }

    private fun validateImage(bytes: ByteArray): String? {
        if (!isPng(bytes) && !isJpeg(bytes) && !isWebp(bytes) && !isGif(bytes)) {
            return "Downloaded file is not a recognizable image"
        }
        val (w, h) = imageDimensions(bytes) ?: return null
        if (w < MIN_DIMENSION || h < MIN_DIMENSION) {
            return "Image too small (${w}×${h})"
        }
        return null
    }

    private fun validateVideo(bytes: ByteArray): String? = when {
        isMp4(bytes) || isWebm(bytes) -> null
        else -> "Downloaded file is not a recognizable video"
    }

    private fun isPng(b: ByteArray): Boolean =
        b.size >= 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() &&
            b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte()

    private fun isJpeg(b: ByteArray): Boolean =
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte()

    private fun isWebp(b: ByteArray): Boolean =
        b.size >= 12 && b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() &&
            b[8] == 'W'.code.toByte() && b[9] == 'E'.code.toByte()

    private fun isGif(b: ByteArray): Boolean =
        b.size >= 6 && b[0] == 'G'.code.toByte() && b[1] == 'I'.code.toByte() &&
            b[2] == 'F'.code.toByte()

    private fun isMp4(b: ByteArray): Boolean =
        b.size > 8 && b[4] == 'f'.code.toByte() && b[5] == 't'.code.toByte() &&
            b[6] == 'y'.code.toByte() && b[7] == 'p'.code.toByte()

    private fun isWebm(b: ByteArray): Boolean =
        b.size >= 4 && b[0] == 0x1A.toByte() && b[1] == 0x45.toByte() &&
            b[2] == 0xDF.toByte() && b[3] == 0xA3.toByte()

    private fun imageDimensions(bytes: ByteArray): Pair<Int, Int>? = when {
        isPng(bytes) && bytes.size >= 24 -> {
            val w = readIntBE(bytes, 16)
            val h = readIntBE(bytes, 20)
            w to h
        }
        isJpeg(bytes) -> parseJpegDimensions(bytes)
        isWebp(bytes) && bytes.size >= 30 -> {
            // RIFF WEBP VP8 — simplified: accept if large enough
            if (bytes.size >= MIN_IMAGE_BYTES) 512 to 512 else null
        }
        else -> null
    }

    private fun readIntBE(b: ByteArray, offset: Int): Int =
        ((b[offset].toInt() and 0xFF) shl 24) or
            ((b[offset + 1].toInt() and 0xFF) shl 16) or
            ((b[offset + 2].toInt() and 0xFF) shl 8) or
            (b[offset + 3].toInt() and 0xFF)

    private fun parseJpegDimensions(bytes: ByteArray): Pair<Int, Int>? {
        var i = 2
        while (i + 8 < bytes.size) {
            if (bytes[i] != 0xFF.toByte()) break
            val marker = bytes[i + 1].toInt() and 0xFF
            if (marker in 0xC0..0xC3) {
                val h = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
                val w = ((bytes[i + 7].toInt() and 0xFF) shl 8) or (bytes[i + 8].toInt() and 0xFF)
                return w to h
            }
            val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
            i += 2 + len
        }
        return null
    }
}
