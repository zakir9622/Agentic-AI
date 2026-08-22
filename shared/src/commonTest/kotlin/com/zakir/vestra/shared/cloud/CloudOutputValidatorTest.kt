package com.zakir.vestra.shared.cloud

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CloudOutputValidatorTest {

    @Test
    fun rejectsEmptyAndTinyPayloads() {
        assertNotNull(CloudOutputValidator.validate(byteArrayOf()))
        assertNotNull(CloudOutputValidator.validate(ByteArray(100)))
    }

    @Test
    fun acceptsMinimalPngHeader() {
        val png = byteArrayOf(
            0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(),
            0x0D, 0x0A, 0x1A, 0x0A,
        ) + ByteArray(80)
        assertNull(CloudOutputValidator.validate(png))
    }

    @Test
    fun acceptsMp4HeaderForVideo() {
        val mp4 = byteArrayOf(0, 0, 0, 0, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()) +
            ByteArray(10_000)
        assertNull(CloudOutputValidator.validate(mp4, isVideo = true))
    }
}
