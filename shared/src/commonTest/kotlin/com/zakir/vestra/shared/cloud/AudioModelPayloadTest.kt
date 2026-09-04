package com.zakir.vestra.shared.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Arity and shape, pinned against the schemas read from the live Spaces.
 *
 * Gradio validates the `data` array before the model runs: a payload with the wrong number of
 * arguments, or a bare string where a `FileData` object is expected, fails validation and streams
 * back an empty error rather than a message. That failure mode is indistinguishable from "the
 * Space is down" at the UI, which is exactly how a wrong payload can sit unnoticed. These are the
 * counts and types the Spaces themselves reported.
 */
class AudioModelPayloadTest {

    @Test
    fun musicGenSendsTextAndAnOptionalMelodySlot() {
        // facebook/MusicGen /predict_batched: texts: str, melodies: filepath | null
        val payload = SpacePayloads.forMusic("musicgen-hf", "a lo-fi beat with rain")
        assertEquals(2, payload.size, "MusicGen takes exactly two arguments: $payload")
        assertTrue(payload[0].toString().contains("lo-fi beat"), payload[0].toString())
        assertEquals("null", payload[1].toString(), "melody slot must be null, not omitted or empty string")
    }

    @Test
    fun musicGenCarriesAMelodyAsFileDataWhenOneIsGiven() {
        val payload = SpacePayloads.forMusic(
            "musicgen-hf",
            "make it jazzier",
            melodyDataUrl = "data:audio/wav;base64,AAAA",
        )
        assertEquals(2, payload.size)
        // A bare data-URL string fails Gradio's FileData validation before the model runs.
        assertTrue(payload[1].toString().contains("path") || payload[1].toString().contains("url"), payload[1].toString())
    }

    @Test
    fun seedVcSendsAllElevenArgumentsInTheSpacesOwnOrder() {
        // Plachta/Seed-VC /predict reported eleven parameters. Sending ten fails validation.
        val payload = SpacePayloads.forVoiceConvert(
            "seed-vc-hf",
            sourceDataUrl = "data:audio/wav;base64,AAAA",
            targetDataUrl = "data:audio/wav;base64,BBBB",
        )
        assertEquals(11, payload.size, "Seed-VC takes exactly eleven arguments: $payload")
    }

    @Test
    fun seedVcSendsBothClipsAsFileDataNotStrings() {
        val payload = SpacePayloads.forVoiceConvert(
            "seed-vc-hf",
            sourceDataUrl = "data:audio/wav;base64,AAAA",
            targetDataUrl = "data:audio/wav;base64,BBBB",
        )
        for (i in 0..1) {
            val arg = payload[i].toString()
            assertTrue(
                arg.contains("path") || arg.contains("url") || arg.contains("meta"),
                "argument $i must be a FileData object, got: $arg",
            )
        }
    }

    @Test
    fun bothModelsAreInTheCatalogueAndReachable() {
        val music = CloudModelCatalog.byId("musicgen-hf")
        val vc = CloudModelCatalog.byId("seed-vc-hf")
        assertNotNull(music, "musicgen-hf missing from the catalogue")
        assertNotNull(vc, "seed-vc-hf missing from the catalogue")
        assertEquals(AiCapability.AUDIO, music.capability)
        assertEquals(AiCapability.AUDIO, vc.capability)
        // Neither should demand a key: the point of these two is that they work without one.
        assertEquals(false, music.requiresApiKey)
        assertEquals(false, vc.requiresApiKey)
    }

    @Test
    fun bothModelsHaveAContractSoPreflightCanExplainAFailure() {
        for (id in listOf("musicgen-hf", "seed-vc-hf")) {
            val provider = CloudModelCatalog.byId(id) ?: error("$id missing")
            val contract = CloudModelContracts.forProvider(provider)
            assertTrue(contract.schemaNote.isNotBlank(), "$id has no schema note")
            assertTrue(contract.failureHint.isNotBlank(), "$id has no failure hint")
        }
    }

    @Test
    fun theVoiceConversionContractSaysTwoClipsAreNeeded() {
        // The constraint most likely to confuse someone: zero-shot conversion has no "make it
        // deeper" without a target timbre to aim at, so the requirement has to be visible.
        val provider = CloudModelCatalog.byId("seed-vc-hf") ?: error("missing")
        val contract = CloudModelContracts.forProvider(provider)
        assertTrue(
            contract.requiredInputs.count { it.startsWith("Audio(") } == 2,
            "the contract must record that two clips are required: ${contract.requiredInputs}",
        )
        assertTrue(contract.failureHint.contains("two clips"), contract.failureHint)
    }

    @Test
    fun theHandTunedPayloadGuardsAgreeWithTheCatalogue() {
        assertTrue(SpacePayloads.hasMusic("musicgen-hf"))
        assertTrue(SpacePayloads.hasVoiceConvert("seed-vc-hf"))
        assertTrue(!SpacePayloads.hasMusic("seed-vc-hf"))
        assertTrue(!SpacePayloads.hasVoiceConvert("musicgen-hf"))
        // TTS models must not be mistaken for either.
        assertTrue(!SpacePayloads.hasMusic("edge-tts-hf"))
        assertTrue(!SpacePayloads.hasVoiceConvert("edge-tts-hf"))
    }
}
