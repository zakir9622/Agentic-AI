package com.zakir.vestra.ui.theme

import com.zakir.vestra.shared.cloud.AiCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-JVM guard on [VestraColors.modalityAccent] — each generation surface (Image/Video/
 * Code/Audio) must resolve to its own distinct accent, and the aliased capabilities
 * (IMAGE_EDIT, TRY_ON) must share the Image accent rather than falling through to a default.
 */
class ModalityAccentTest {

    @Test
    fun everyCoreCapabilityHasADistinctAccent() {
        val image = VestraColors.modalityAccent(AiCapability.IMAGE_GEN)
        val video = VestraColors.modalityAccent(AiCapability.VIDEO)
        val code = VestraColors.modalityAccent(AiCapability.CODE)
        val audio = VestraColors.modalityAccent(AiCapability.AUDIO)

        val accents = listOf(image, video, code, audio)
        assertEquals("expected 4 distinct modality accents", accents.toSet().size, accents.size)
    }

    @Test
    fun imageEditAndTryOnAliasToTheImageAccent() {
        val image = VestraColors.modalityAccent(AiCapability.IMAGE_GEN)
        assertEquals(image, VestraColors.modalityAccent(AiCapability.IMAGE_EDIT))
        assertEquals(image, VestraColors.modalityAccent(AiCapability.TRY_ON))
    }

    @Test
    fun accentsMatchTheirNamedTokens() {
        assertEquals(VestraColors.ModalityImage, VestraColors.modalityAccent(AiCapability.IMAGE_GEN))
        assertEquals(VestraColors.ModalityVideo, VestraColors.modalityAccent(AiCapability.VIDEO))
        assertEquals(VestraColors.ModalityCode, VestraColors.modalityAccent(AiCapability.CODE))
        assertEquals(VestraColors.ModalityAudio, VestraColors.modalityAccent(AiCapability.AUDIO))
    }

    @Test
    fun modalityAccentsDifferFromTheGenericAccent() {
        // The whole point of A0 is that studio surfaces stop reaching for the flat brand accent —
        // regression-guard that at least video/code/audio (the non-brass modalities) diverge from it.
        assertNotEquals(VestraColors.Accent, VestraColors.modalityAccent(AiCapability.VIDEO))
        assertNotEquals(VestraColors.Accent, VestraColors.modalityAccent(AiCapability.CODE))
        assertNotEquals(VestraColors.Accent, VestraColors.modalityAccent(AiCapability.AUDIO))
    }
}
