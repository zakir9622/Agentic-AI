package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Minimal Gradio 4 HTTP client for Hugging Face Spaces.
 * Images are passed as data-URL strings in the [data] payload.
 */
class HfGradioClient(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun predict(
        spaceHost: String,
        apiName: String,
        data: List<String>,
        hfToken: String?,
    ): JsonElement {
        val base = "https://$spaceHost"
        val eventId = http.post("$base/gradio_api/call/$apiName") {
            contentType(ContentType.Application.Json)
            hfToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            setBody(mapOf("data" to data))
        }.body<JsonObject>()["event_id"]?.jsonPrimitive?.content
            ?: error("Gradio did not return an event_id")

        repeat(90) {
            val body = http.get("$base/gradio_api/call/$apiName/$eventId") {
                hfToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }.bodyAsText()
            if (body.contains("event: error") || body.contains("\"event\":\"error\"")) {
                error("Hugging Face Space error: ${body.take(500)}")
            }
            if (body.contains("event: complete") || body.contains("\"event\":\"complete\"")) {
                return parseCompletePayload(body)
            }
            delay(2_000)
        }
        error("Timed out waiting for Hugging Face Space ($spaceHost)")
    }

    private fun parseCompletePayload(raw: String): JsonElement {
        val dataLine = raw.lines().lastOrNull { it.startsWith("data:") }
            ?: raw.lines().firstOrNull { it.trimStart().startsWith("{") }
            ?: error("Unexpected Gradio response")
        val jsonText = dataLine.removePrefix("data:").trim()
        val element = json.parseToJsonElement(jsonText)
        return when (element) {
            is JsonArray -> element.firstOrNull() ?: error("Empty Gradio output")
            is JsonObject -> element["data"]?.jsonArray?.firstOrNull()
                ?: element["output"]?.let { if (it is JsonArray) it.firstOrNull() else it }
                ?: element
            else -> element
        }
    }
}
