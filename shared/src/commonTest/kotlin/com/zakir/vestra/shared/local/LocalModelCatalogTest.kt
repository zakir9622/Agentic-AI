package com.zakir.vestra.shared.local

import com.zakir.vestra.shared.cloud.AiCapability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalModelCatalogTest {

    @Test
    fun imageStudioPickerExcludesQualityUpscalers() {
        val ids = LocalModelCatalog.forStudioPicker(AiCapability.IMAGE_GEN).map { it.id }
        assertTrue(ids.contains("local-sdturbo-v1"))
        assertFalse(ids.contains("local-quality-realesrgan"))
        assertFalse(ids.contains("local-quality-gfpgan"))
        assertFalse(ids.contains("local-quality-birefnet"))
    }

    @Test
    fun imageEditStudioPickerOffersLocalImg2Img() {
        val ids = LocalModelCatalog.forStudioPicker(AiCapability.IMAGE_EDIT).map { it.id }
        assertEquals(listOf("local-sdturbo-edit"), ids)
        val entry = LocalModelCatalog.entries.first { it.id == "local-sdturbo-edit" }
        assertTrue(entry.runnable)
        assertEquals("local-sdturbo-v1", entry.packId)
        assertTrue(LocalModelCatalog.studioEntryReady(entry, packReady = true))
        assertFalse(LocalModelCatalog.studioEntryReady(entry, packReady = false))
    }

    @Test
    fun audioStudioShowsTtsScaffoldAndVoiceChanger() {
        val ids = LocalModelCatalog.forStudioPicker(AiCapability.AUDIO).map { it.id }
        assertEquals(listOf("local-tts-system", "local-tts-v1", "local-voice-changer"), ids)
        val system = LocalModelCatalog.entries.first { it.id == "local-tts-system" }
        assertTrue(system.runnable)
        assertEquals("Ready offline", LocalModelCatalog.studioStatusLabel(system, packReady = false))
        val tts = LocalModelCatalog.entries.first { it.id == "local-tts-v1" }
        assertFalse(tts.runnable)
        val changer = LocalModelCatalog.entries.first { it.id == "local-voice-changer" }
        assertTrue(changer.runnable)
        assertEquals("Ready offline", LocalModelCatalog.studioStatusLabel(changer, packReady = false))
    }

    @Test
    fun videoStudioOffersLocalStillClip() {
        val video = LocalModelCatalog.forStudioPicker(AiCapability.VIDEO)
        assertEquals(listOf("local-stillclip-v1"), video.map { it.id })
        assertTrue(video.first().runnable)
        assertEquals("local-sdturbo-v1", video.first().packId)
        assertTrue(
            LocalModelCatalog.studioStatusLabel(video.first(), false)
                .contains("Download", ignoreCase = true),
        )
        assertEquals(
            "Ready offline (still-clip)",
            LocalModelCatalog.studioStatusLabel(video.first(), true),
        )
    }

    @Test
    fun codeStudioOffersLocalGemma() {
        val code = LocalModelCatalog.forStudioPicker(AiCapability.CODE)
        assertEquals(listOf("local-gemma-v1"), code.map { it.id })
        assertTrue(code.first().runnable)
        assertEquals("local-gemma-v1", code.first().packId)
        assertFalse(LocalModelCatalog.studioEntryReady(code.first(), packReady = false))
        assertTrue(LocalModelCatalog.studioEntryReady(code.first(), packReady = true))
    }

    @Test
    fun imageStudioPickerPromptsDownloadWhenPackMissing() {
        val entry = LocalModelCatalog.entries.first { it.id == "local-sdturbo-v1" }
        assertTrue(entry.runnable)
        assertTrue(
            LocalModelCatalog.studioStatusLabel(entry, packReady = false)
                .contains("Download", ignoreCase = true),
        )
        assertFalse(LocalModelCatalog.studioEntryReady(entry, packReady = false))
    }

    @Test
    fun sdturboShowsReadyOfflineWhenPackGraphsInstalled() {
        val entry = LocalModelCatalog.entries.first { it.id == "local-sdturbo-v1" }
        assertEquals("Ready offline", LocalModelCatalog.studioStatusLabel(entry, packReady = true))
        assertTrue(LocalModelCatalog.studioEntryReady(entry, packReady = true))
    }

    @Test
    fun qualityPacksStayInCatalogForSettings() {
        val quality = LocalModelCatalog.entries.filter {
            it.pickerRole == LocalModelPickerRole.QUALITY_POST
        }
        assertTrue(quality.any { it.packId == "realesrgan-v1" })
        assertTrue(quality.any { it.packId == "birefnet-v1" })
        quality.forEach {
            assertTrue(
                it.capability == AiCapability.TRY_ON,
                "${it.id} should be TRY_ON quality post, was ${it.capability}",
            )
        }
    }
}
