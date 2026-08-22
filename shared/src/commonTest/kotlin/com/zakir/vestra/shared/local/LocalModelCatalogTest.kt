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
    fun imageEditStudioPickerHasNoQualityPacks() {
        assertTrue(LocalModelCatalog.forStudioPicker(AiCapability.IMAGE_EDIT).isEmpty())
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
    fun videoStudioIsHonestResearchOnly() {
        val video = LocalModelCatalog.forStudioPicker(AiCapability.VIDEO)
        assertEquals(1, video.size)
        assertFalse(video.first().runnable)
        assertTrue(
            LocalModelCatalog.studioStatusLabel(video.first(), false)
                .contains("no on-device weights", ignoreCase = true),
        )
    }

    @Test
    fun imageStudioPickerShowsEngineReadyStatusWithoutPack() {
        val entry = LocalModelCatalog.entries.first { it.id == "local-sdturbo-v1" }
        assertTrue(
            LocalModelCatalog.studioStatusLabel(entry, packReady = false)
                .contains("Engine ready", ignoreCase = true),
        )
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
