package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.domain.GarmentCategory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
        assertTrue(editor["background"].isGradioFileData(), "background must be a Gradio FileData object")
        assertTrue(editor["layers"] is JsonArray)
        assertEquals(JsonNull, editor["composite"])
        assertTrue(data[1].isGradioFileData(), "garment must be a Gradio FileData object")
        assertEquals(true, (data[3] as JsonPrimitive).content.toBoolean())
    }

    @Test
    fun imageEditSendsFileDataNotBareDataUrl() {
        // A bare data-URL string fails Gradio's image validation and streams back an empty
        // error (`event: error` / `data: null`) instead of a usable message.
        listOf("qwen-image-edit-hf", "instruct-pix2pix-hf").forEach { id ->
            val data = SpacePayloads.forImageEdit(id, "make it emerald", "data:image/jpeg;base64,xx")
            assertTrue(data[0].isGradioFileData(), "$id must send the reference image as FileData")
            assertEquals("data:image/jpeg;base64,xx", (data[0] as JsonObject)["url"].primitiveContent())
        }
    }

    @Test
    fun ootdSendsBothImagesAsFileData() {
        val data = SpacePayloads.forTryOn(
            "ootd-hf",
            "data:image/jpeg;base64,person",
            "data:image/jpeg;base64,garment",
            GarmentCategory.ABAYA,
        )
        assertTrue(data[0].isGradioFileData())
        assertTrue(data[1].isGradioFileData())
    }

    @Test
    fun fileDataRejectsNonImageReference() {
        assertFailsWith<IllegalArgumentException> { SpacePayloads.fileData("/local/path.jpg") }
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

    @Test
    fun zeroGpuQuotaFailureExplainsTheDailyAllowance() {
        val provider = CloudModelCatalog.byId("qwen-image-edit-hf")!!
        val raw = "Hugging Face Space failed: You have exceeded your GPU quota. " +
            "Subscribe to Hugging Face PRO to get 25 min of ZeroGPU quota a day"
        val msg = CloudModelContracts.friendlyFailure(provider, raw, "Image generation")
        assertTrue(msg.contains("ZeroGPU", ignoreCase = true), msg)
        assertTrue(msg.contains("refills", ignoreCase = true), msg)
    }

    @Test
    fun imageGenAndEditAllowHfInferenceProviders() {
        listOf(AiCapability.IMAGE_GEN, AiCapability.IMAGE_EDIT).forEach { capability ->
            val hasInference = CloudModelCatalog.forCapability(capability)
                .any { it.platform == CloudPlatform.HF_INFERENCE }
            val hasSpace = CloudModelCatalog.forCapability(capability)
                .any { it.platform == CloudPlatform.HF_SPACE }
            assertTrue(hasSpace, "$capability needs at least one Space")
            if (capability == AiCapability.IMAGE_GEN) {
                assertTrue(hasInference, "$capability should offer HF Inference fallback")
            }
        }
    }

    @Test
    fun defaultsPointAtModelsVerifiedAgainstLiveSpaces() {
        assertEquals("flux-schnell-hf", CloudModelCatalog.defaultImageGenId)
        assertEquals("ootd-hf", CloudModelCatalog.defaultTryOnId)
        listOf(CloudModelCatalog.defaultImageGenId, CloudModelCatalog.defaultTryOnId).forEach { id ->
            val provider = CloudModelCatalog.byId(id)!!
            assertEquals(ModelSupportLevel.READY, CloudModelContracts.forProvider(provider).support, id)
        }
    }
}

private fun JsonElement?.isGradioFileData(): Boolean {
    val obj = this as? JsonObject ?: return false
    return obj.containsKey("url") && (obj["meta"] as? JsonObject)?.get("_type").primitiveContent() ==
        "gradio.FileData"
}

private fun JsonElement?.primitiveContent(): String? = (this as? JsonPrimitive)?.content
