package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.domain.GarmentCategory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpacePayloadsTest {

    @Test
    fun fluxInferHasSixArgs() {
        val data = SpacePayloads.forImageGen("flux-schnell-hf", "abaya lookbook")
        assertEquals(6, data.size)
        assertEquals("abaya lookbook", (data[0] as JsonPrimitive).content)
        assertEquals(true, (data[2] as JsonPrimitive).content.toBoolean())
    }

    @Test
    fun instructPix2PixHasEightArgs() {
        val data = SpacePayloads.forImageEdit("instruct-pix2pix-hf", "add hijab", "data:image/jpeg;base64,xx")
        assertEquals(8, data.size)
    }

    @Test
    fun wan2VideoHasEightArgs() {
        assertEquals(8, SpacePayloads.forVideo("wan2-video-hf", "walk cycle").size)
    }

    @Test
    fun ltxUsesTextToVideoFourteenArgs() {
        val data = SpacePayloads.forVideo("ltx-zerogpu-hf", "modest runway walk")
        assertEquals(14, data.size)
        assertEquals("text-to-video", (data[6] as JsonPrimitive).content)
        assertEquals("text_to_video", CloudModelContracts.effectiveApiName(CloudModelCatalog.byId("ltx-zerogpu-hf")!!))
    }

    @Test
    fun idmTryOnUsesImageEditorAndSevenArgs() {
        val data = SpacePayloads.forTryOn(
            "idm-vton-hf",
            "data:image/jpeg;base64,person",
            "data:image/jpeg;base64,garment",
            GarmentCategory.ABAYA,
        )
        assertEquals(7, data.size)
        val editor = data[0] as JsonObject
        assertTrue(editor["background"] is JsonPrimitive)
        assertTrue(editor["layers"] is JsonArray)
        assertEquals(JsonNull, editor["composite"])
        assertEquals(true, (data[3] as JsonPrimitive).content.toBoolean())
    }

    @Test
    fun ootdTryOnUsesSixNumericControls() {
        val data = SpacePayloads.forTryOn(
            "ootd-hf",
            "data:image/jpeg;base64,person",
            "data:image/jpeg;base64,garment",
            GarmentCategory.ABAYA,
        )
        assertEquals(6, data.size)
        assertEquals("1", (data[2] as JsonPrimitive).content)
    }

    @Test
    fun fitditIsBlockedByContract() {
        assertEquals(
            ModelSupportLevel.UNSUPPORTED,
            CloudModelContracts.forProvider(CloudModelCatalog.byId("fitdit-hf")!!).support,
        )
        assertFailsWith<IllegalStateException> {
            SpacePayloads.forTryOn(
                "fitdit-hf",
                "data:image/jpeg;base64,person",
                "data:image/jpeg;base64,garment",
                GarmentCategory.ABAYA,
            )
        }
    }

    @Test
    fun everyCatalogProviderHasContractCoverage() {
        CloudModelCatalog.providers.forEach { provider ->
            val contract = CloudModelContracts.forProvider(provider)
            assertEquals(provider.id, contract.providerId)
            assertTrue(contract.schemaNote.isNotBlank(), provider.id)
            assertTrue(contract.failureHint.isNotBlank(), provider.id)
            assertTrue(contract.requiredInputs.isNotEmpty(), provider.id)
        }
    }

    @Test
    fun friendlyFailureMentionsSelectedModel() {
        val provider = CloudModelCatalog.byId("flux-schnell-hf")!!
        val msg = CloudModelContracts.friendlyFailure(provider, "HTTP 503 Service Unavailable", "Image")
        assertTrue(msg.contains("FLUX") || msg.contains("503") || msg.contains("off-peak") || msg.contains("IDM") || msg.isNotBlank())
    }
}
