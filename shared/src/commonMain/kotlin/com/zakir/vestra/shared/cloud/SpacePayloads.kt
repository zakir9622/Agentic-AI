package com.zakir.vestra.shared.cloud

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Typed Gradio `data` arrays matched to live Space schemas (Aug 2026).
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
        else -> listOf(JsonPrimitive(prompt))
    }
}
