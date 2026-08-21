package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.domain.GarmentCategory
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Typed Gradio `data` arrays matched to live Space `/info` schemas (Aug 2026).
 * Wrong apiName / shape was the root cause of empty/failed generations.
 */
object SpacePayloads {

    fun forImageGen(providerId: String, prompt: String): List<JsonElement> = when (providerId) {
        "flux-schnell-hf" -> listOf(
            JsonPrimitive(prompt),
            JsonPrimitive(0), // seed
            JsonPrimitive(true), // randomize
            JsonPrimitive(1024), // width
            JsonPrimitive(1024), // height
            JsonPrimitive(4), // steps
        )
        "sdxl-lightning-hf" -> listOf(
            JsonPrimitive(prompt),
            JsonPrimitive("4-Step"),
        )
        else -> listOf(JsonPrimitive(prompt))
    }

    fun forImageEdit(providerId: String, prompt: String, imageDataUrl: String): List<JsonElement> =
        when (providerId) {
            "instruct-pix2pix-hf" -> listOf(
                JsonPrimitive(imageDataUrl),
                JsonPrimitive(prompt),
                JsonPrimitive(50), // steps
                JsonPrimitive("Randomize Seed"),
                JsonPrimitive(42),
                JsonPrimitive("Fix CFG"),
                JsonPrimitive(7.5),
                JsonPrimitive(1.5),
            )
            "qwen-image-edit-hf" -> listOf(
                JsonPrimitive(imageDataUrl),
                JsonPrimitive(prompt),
                JsonPrimitive(0),
                JsonPrimitive(true),
                JsonPrimitive(4.0),
                JsonPrimitive(50),
                JsonPrimitive(true),
            )
            else -> listOf(JsonPrimitive(imageDataUrl), JsonPrimitive(prompt))
        }

    fun forVideo(providerId: String, prompt: String): List<JsonElement> = when (providerId) {
        "wan2-video-hf" -> listOf(
            JsonPrimitive(prompt),
            JsonNull, // optional i2v image
            JsonPrimitive(832),
            JsonPrimitive(480),
            JsonPrimitive(33), // frames
            JsonPrimitive(25), // steps
            JsonPrimitive(5.0),
            JsonPrimitive(-1),
        )
        "ltx-zerogpu-hf" -> listOf(
            JsonPrimitive(prompt),
            JsonPrimitive("blurry, low quality, distorted, watermark, text"),
            JsonPrimitive(""), // image_n unused for t2v
            JsonPrimitive(""), // video_n unused for t2v
            JsonPrimitive(512), // height
            JsonPrimitive(768), // width
            JsonPrimitive("text-to-video"),
            JsonPrimitive(3.0), // duration seconds
            JsonPrimitive(0), // frames from input video
            JsonPrimitive(42), // seed
            JsonPrimitive(true), // randomize
            JsonPrimitive(3.0), // cfg
            JsonPrimitive(true), // improve texture
            JsonPrimitive(false), // slow motion
        )
        else -> listOf(JsonPrimitive(prompt))
    }

    /**
     * Virtual try-on payloads. Throws with a model-specific message when the
     * selected Space requires mask/pose inputs the app cannot supply.
     */
    fun forTryOn(
        providerId: String,
        personDataUrl: String,
        garmentDataUrl: String,
        category: GarmentCategory,
    ): List<JsonElement> {
        CloudModelContracts.preflightOrNull(
            CloudModelCatalog.byId(providerId) ?: error("Unknown try-on model: $providerId"),
        )?.let { error(it) }

        return when (providerId) {
            "idm-vton-hf" -> listOf(
                imageEditor(personDataUrl),
                JsonPrimitive(garmentDataUrl),
                JsonPrimitive(category.idmGarmentDesc()),
                JsonPrimitive(true), // auto-generated mask
                JsonPrimitive(false), // auto-crop
                JsonPrimitive(30), // denoising steps
                JsonPrimitive(42), // seed
            )
            "ootd-hf" -> listOf(
                JsonPrimitive(personDataUrl),
                JsonPrimitive(garmentDataUrl),
                JsonPrimitive(1), // number of images
                JsonPrimitive(20), // steps
                JsonPrimitive(2.0), // guidance
                JsonPrimitive(42), // seed
            )
            "catvton-hf" -> listOf(
                imageEditor(personDataUrl),
                JsonPrimitive(garmentDataUrl),
                JsonPrimitive(category.catvtonClothType()),
                JsonPrimitive(50), // inference steps
                JsonPrimitive(2.5), // cfg
                JsonPrimitive(42), // seed
                JsonPrimitive("result only"),
            )
            "leffa-hf" -> listOf(
                JsonPrimitive(personDataUrl),
                JsonPrimitive(garmentDataUrl),
                JsonPrimitive("vton"),
                JsonPrimitive(1.0),
            )
            "catvton-flux-hf" -> listOf(
                imageEditor(personDataUrl),
                JsonPrimitive(garmentDataUrl),
                JsonPrimitive(category.catvtonClothType()),
                JsonPrimitive(28),
                JsonPrimitive(3.5),
                JsonPrimitive(42),
                JsonPrimitive("result only"),
            )
            else -> listOf(JsonPrimitive(personDataUrl), JsonPrimitive(garmentDataUrl))
        }
    }

    /** Gradio ImageEditor value for auto-mask Spaces (background only, empty layers). */
    fun imageEditor(backgroundDataUrl: String): JsonElement = buildJsonObject {
        put("background", backgroundDataUrl)
        put("layers", buildJsonArray { })
        put("composite", JsonNull)
    }
}

private fun GarmentCategory.idmGarmentDesc(): String = when (this) {
    GarmentCategory.LOWER_BODY -> "Lower-body garment"
    GarmentCategory.HIJAB, GarmentCategory.NIQAB, GarmentCategory.DUPATTA, GarmentCategory.HEADSCARF ->
        "Upper-body / head covering"
    else -> "Dress / full garment"
}

private fun GarmentCategory.catvtonClothType(): String = when (this) {
    GarmentCategory.LOWER_BODY -> "lower"
    GarmentCategory.ABAYA, GarmentCategory.JILBAB, GarmentCategory.KAFTAN,
    GarmentCategory.DRESS, GarmentCategory.LEHENGA, GarmentCategory.FULL_COVERAGE,
    GarmentCategory.SHALWAR_KAMEEZ -> "overall"
    else -> "upper"
}
