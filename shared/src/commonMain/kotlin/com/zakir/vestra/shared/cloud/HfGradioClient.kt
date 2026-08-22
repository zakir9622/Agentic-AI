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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Minimal Gradio HTTP client for Hugging Face Spaces.
 *
 * Two things vary per Space and both used to break generation silently:
 *  - Gradio 5 serves the queue under `/gradio_api/call/…` while Gradio 4 serves `/call/…`.
 *  - Image inputs must be [SpacePayloads.fileData] objects; a bare data-URL string is
 *    rejected by pydantic and streams back an *empty* error (`event: error`/`data: null`).
 */
class HfGradioClient(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Remembers which queue prefix a host answered on, so we probe at most once per host. */
    private val prefixLock = Any()
    private val prefixCache = mutableMapOf<String, String>()

    suspend fun predict(
        spaceHost: String,
        apiName: String,
        data: List<JsonElement>,
        hfToken: String?,
        maxPolls: Int = 45,
        pollDelayMs: Long = 2_000,
        wakeRetries: Int = 1,
        wakeDelayMs: Long = 8_000,
    ): JsonElement {
        require(spaceHost.isNotBlank()) { "Space host is empty" }
        require(apiName.isNotBlank()) { "Gradio api name is empty" }
        require(data.isNotEmpty()) { "Gradio payload is empty" }

        val token = hfToken?.takeIf { it.isNotBlank() }
        // Curated Spaces are public — try anonymous first so a spent token allowance does not
        // block the shared ZeroGPU queue. Fall back to the token for priority when available.
        val credentials = if (token == null) listOf(null) else listOf(null, token)

        var lastEmptyError: String? = null
        var quotaExhausted = false
        repeat(wakeRetries + 1) { attempt ->
            if (attempt > 0) delay(wakeDelayMs)
            for (credential in credentials) {
                if (quotaExhausted && credential == null) continue
                when (
                    val outcome =
                        attemptPredict(spaceHost, apiName, data, credential, maxPolls, pollDelayMs)
                ) {
                    is PredictOutcome.Success -> return outcome.value
                    is PredictOutcome.QuotaExhausted -> {
                        quotaExhausted = true
                        lastEmptyError = outcome.message
                    }
                    is PredictOutcome.EmptyError -> lastEmptyError = outcome.message
                }
            }
        }
        error(lastEmptyError ?: "Hugging Face Space $spaceHost did not return a result")
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

    private sealed interface PredictOutcome {
        data class Success(val value: JsonElement) : PredictOutcome
        data class EmptyError(val message: String) : PredictOutcome
        data class QuotaExhausted(val message: String) : PredictOutcome
    }

    private suspend fun attemptPredict(
        spaceHost: String,
        apiName: String,
        data: List<JsonElement>,
        hfToken: String?,
        maxPolls: Int,
        pollDelayMs: Long,
    ): PredictOutcome {
        val base = "https://$spaceHost"
        val payload = buildJsonObject {
            put("data", buildJsonArray { data.forEach { add(it) } })
        }

        val prefixes = synchronized(prefixLock) {
            prefixCache[spaceHost]?.let { listOf(it) } ?: QUEUE_PREFIXES
        }
        var eventId: String? = null
        var usedPrefix = prefixes.first()
        var lastFailure: String? = null

        for (prefix in prefixes) {
            val response = http.post("$base$prefix/call/$apiName") {
                contentType(ContentType.Application.Json)
                hfToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
                setBody(payload)
            }
            val raw = response.bodyAsText()
            if (!response.status.isSuccess()) {
                lastFailure =
                    "Hugging Face Space $spaceHost/$apiName HTTP ${response.status.value}: ${raw.take(240)}"
                // 404 just means this Space speaks the other Gradio dialect — try the next prefix.
                if (response.status.value == 404) continue
                error(lastFailure)
            }
            val id = runCatching {
                json.parseToJsonElement(raw).jsonObject["event_id"]?.jsonPrimitive?.content
            }.getOrNull()
            if (id.isNullOrBlank()) {
                lastFailure = "Gradio did not return an event_id (${raw.take(200)})"
                continue
            }
            eventId = id
            usedPrefix = prefix
            break
        }

        val id = eventId ?: error(lastFailure ?: "Hugging Face Space $spaceHost/$apiName is unreachable")
        synchronized(prefixLock) { prefixCache[spaceHost] = usedPrefix }

        repeat(maxPolls) {
            val poll = http.get("$base$usedPrefix/call/$apiName/$id") {
                hfToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }
            val body = poll.bodyAsText()
            if (!poll.status.isSuccess()) {
                error("Hugging Face Space poll HTTP ${poll.status.value}: ${body.take(240)}")
            }
            if (body.contains("event: error") || body.contains("\"event\":\"error\"")) {
                val detail = gradioErrorDetail(body)
                val message = formatGradioError(detail, spaceHost, apiName, body)
                return when {
                    !detail.isNullOrBlank() && detail.isQuotaExhausted() ->
                        PredictOutcome.QuotaExhausted(message)
                    detail.isNullOrBlank() || detail.isTransient() ->
                        PredictOutcome.EmptyError(message)
                    else -> error(message)
                }
            }
            if (body.contains("event: complete") || body.contains("\"event\":\"complete\"")) {
                return PredictOutcome.Success(parseCompletePayload(body))
            }
            delay(pollDelayMs)
        }
        error(
            "Timed out waiting for Hugging Face Space ($spaceHost). " +
                "Try again off-peak or pick a faster free model.",
        )
    }

    private fun parseCompletePayload(raw: String): JsonElement {
        val dataLine = raw.lines()
            .map { it.trim() }
            .lastOrNull { it.startsWith("data:") && it.removePrefix("data:").trim() != "null" }
            ?: raw.lines().firstOrNull { it.trimStart().startsWith("{") || it.trimStart().startsWith("[") }
            ?: error("Unexpected Gradio response")
        val jsonText = dataLine.removePrefix("data:").trim()
        val element = json.parseToJsonElement(jsonText)
        return firstMediaElement(element) ?: error("Empty Gradio output")
    }

    /**
     * Finds the first file-bearing item in a Gradio output tuple. Outputs are not always at
     * index 0 — InstructPix2Pix returns `[seed, text_cfg, image_cfg, image]` and gallery
     * Spaces nest the file one or two levels deep.
     */
    private fun firstMediaElement(element: JsonElement, depth: Int = 0): JsonElement? {
        if (depth > 4) return null
        return when (element) {
            is JsonPrimitive -> element.takeIf { it.isString && looksLikeMediaRef(it.content) }
            is JsonObject -> when {
                element.containsFileRef() -> element
                else -> MEDIA_KEYS.firstNotNullOfOrNull { key ->
                    element[key]?.let { firstMediaElement(it, depth + 1) }
                }
            }
            is JsonArray -> element.firstNotNullOfOrNull { firstMediaElement(it, depth + 1) }
        }
    }

    private fun JsonObject.containsFileRef(): Boolean =
        (this["url"] as? JsonPrimitive)?.isString == true ||
            (this["path"] as? JsonPrimitive)?.isString == true

    private fun looksLikeMediaRef(value: String): Boolean =
        value.startsWith("http", ignoreCase = true) ||
            value.startsWith("data:", ignoreCase = true) ||
            value.startsWith("/") ||
            MEDIA_EXTENSIONS.any { value.endsWith(it, ignoreCase = true) }

    /** Returns the Space-reported error text, or null when Gradio streamed an empty error. */
    private fun gradioErrorDetail(body: String): String? {
        val dataLine = body.lines()
            .map { it.trim() }
            .lastOrNull { it.startsWith("data:") }
            ?.removePrefix("data:")
            ?.trim()
            .orEmpty()
        if (dataLine.isBlank() || dataLine == "null") return null
        return runCatching {
            when (val el = json.parseToJsonElement(dataLine)) {
                is JsonObject -> when (val err = el["error"] ?: el["message"]) {
                    null -> null
                    is JsonPrimitive -> err.content.takeIf { it.isNotBlank() && it != "null" }
                    else -> err.toString().take(200).takeIf { it != "null" }
                }
                is JsonPrimitive -> el.content.takeIf { it.isNotBlank() && it != "null" }
                else -> null
            }
        }.getOrNull()
    }

    /** Errors that mean "the Space is restarting", not "this request is wrong". */
    private fun String.isTransient(): Boolean =
        contains("session not found", ignoreCase = true) ||
            contains("queue is full", ignoreCase = true) ||
            contains("worker error", ignoreCase = true) ||
            contains("gpu task aborted", ignoreCase = true) ||
            contains("no gpu is currently available", ignoreCase = true) ||
            contains("No GPU was available", ignoreCase = true) ||
            contains("queue timeout", ignoreCase = true)

    private fun String.isQuotaExhausted(): Boolean =
        contains("exceeded your free ZeroGPU", ignoreCase = true) ||
            contains("ZeroGPU quota", ignoreCase = true) ||
            contains("quota exceeded", ignoreCase = true) ||
            contains("0s left", ignoreCase = true)

    private fun formatGradioError(
        detail: String?,
        spaceHost: String,
        apiName: String,
        body: String,
    ): String = when {
        !detail.isNullOrBlank() && detail.isTransient() ->
            "Hugging Face Space $spaceHost is restarting ($detail). " +
                "Wait ~30s and retry, or switch model in the composer."
        !detail.isNullOrBlank() -> "Hugging Face Space $spaceHost/$apiName failed: $detail"
        body.isNotBlank() ->
            "Hugging Face Space $spaceHost returned an empty error — usually the free ZeroGPU " +
                "allowance is spent, or the Space is still waking. The daily allowance refills; " +
                "until then try again later or switch model in the composer."
        else -> "Hugging Face Space $spaceHost/$apiName error: ${body.take(280)}"
    }

    private companion object {
        /** Gradio 5 serves `/gradio_api/call`; Gradio 4 serves `/call`. */
        val QUEUE_PREFIXES = listOf("/gradio_api", "")
        val MEDIA_KEYS = listOf("image", "video", "value", "data", "output", "file")
        val MEDIA_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".mp4", ".webm")
    }
}
