package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Minimal Gradio 4+ HTTP client for Hugging Face Spaces.
 * Payloads must match each Space's live `api_name` + typed `data` array.
 */
class HfGradioClient(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun predict(
        spaceHost: String,
        apiName: String,
        data: List<JsonElement>,
        hfToken: String?,
        maxPolls: Int = 90,
        pollDelayMs: Long = 2_000,
    ): JsonElement {
        require(spaceHost.isNotBlank()) { "Space host is empty" }
        require(apiName.isNotBlank()) { "Gradio api name is empty" }
        require(data.isNotEmpty()) { "Gradio payload is empty" }

        val base = "https://$spaceHost"
        val payload = buildJsonObject {
            put("data", buildJsonArray { data.forEach { add(it) } })
        }
        val callResponse = http.post("$base/gradio_api/call/$apiName") {
            contentType(ContentType.Application.Json)
            hfToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            setBody(payload)
        }
        val callRaw = callResponse.bodyAsText()
        if (!callResponse.status.isSuccess()) {
            error(
                "Hugging Face Space $spaceHost/$apiName HTTP ${callResponse.status.value}: ${callRaw.take(240)}",
            )
        }
        val eventId = runCatching {
            json.parseToJsonElement(callRaw).jsonObject["event_id"]?.jsonPrimitive?.content
        }.getOrNull()
            ?: error("Gradio did not return an event_id (${callRaw.take(200)})")

        repeat(maxPolls) {
            val poll = http.get("$base/gradio_api/call/$apiName/$eventId") {
                hfToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }
            val body = poll.bodyAsText()
            if (!poll.status.isSuccess()) {
                error("Hugging Face Space poll HTTP ${poll.status.value}: ${body.take(240)}")
            }
            if (body.contains("event: error") || body.contains("\"event\":\"error\"")) {
                error("Hugging Face Space error: ${body.take(500)}")
            }
            if (body.contains("event: complete") || body.contains("\"event\":\"complete\"")) {
                return parseCompletePayload(body)
            }
            delay(pollDelayMs)
        }
        error("Timed out waiting for Hugging Face Space ($spaceHost). Try again off-peak or pick a faster free model.")
    }

    /** Convenience for all-string payloads (try-on helpers). */
    suspend fun predictStrings(
        spaceHost: String,
        apiName: String,
        data: List<String>,
        hfToken: String?,
        maxPolls: Int = 90,
        pollDelayMs: Long = 2_000,
    ): JsonElement = predict(
        spaceHost = spaceHost,
        apiName = apiName,
        data = data.map { JsonPrimitive(it) },
        hfToken = hfToken,
        maxPolls = maxPolls,
        pollDelayMs = pollDelayMs,
    )

    private fun parseCompletePayload(raw: String): JsonElement {
        val dataLine = raw.lines().lastOrNull { it.startsWith("data:") }
            ?: raw.lines().firstOrNull { it.trimStart().startsWith("{") || it.trimStart().startsWith("[") }
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
