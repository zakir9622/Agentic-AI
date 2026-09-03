package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readUTF8Line
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class LlmResult(
    val text: String,
    val tokensIn: Int = 0,
    val tokensOut: Int = 0,
)

/** OpenAI-compatible chat completions for Groq, OpenRouter, and HF Inference. */
class LlmClient(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun chat(
        platform: CloudPlatform,
        model: String,
        prompt: String,
        apiKey: String,
        system: String = "You are a helpful coding assistant. Return clear, working code with brief explanations.",
        temperature: Double = 0.2,
    ): LlmResult {
        require(apiKey.isNotBlank()) { "API key required for $platform" }
        require(model.isNotBlank()) { "Model id required" }
        require(prompt.isNotBlank()) { "Prompt is empty" }

        val (url, authHeader) = endpointFor(platform, apiKey)
        val body = chatBody(model, prompt, system, temperature, stream = false)

        val httpResponse = http.post(url) {
            header("Authorization", authHeader)
            if (platform == CloudPlatform.OPENROUTER) {
                header("HTTP-Referer", "https://github.com/zakir9622/Agentic-AI")
                header("X-Title", "The Lookbook")
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val raw = httpResponse.bodyAsText()
        val element = runCatching { json.parseToJsonElement(raw) }.getOrNull()
        val response = element as? JsonObject

        if (!httpResponse.status.isSuccess()) {
            val detail = extractErrorMessage(response) ?: raw.take(200).ifBlank { null }
            error(
                when (httpResponse.status.value) {
                    401, 403 -> "HF/API token rejected (${httpResponse.status.value}). Use a classic Read/Write token (not fine-grained without Inference), then Save in Settings."
                    404 -> "Model not available on free $platform: $model. Switch model in Settings."
                    402 -> "HF Inference monthly credits used up. Wait for reset or add Groq/OpenRouter in Settings."
                    429 -> "Free-tier rate limit on $platform. Wait a minute or switch model."
                    else -> {
                        val msg = detail.orEmpty()
                        when {
                            msg.contains("not supported by any provider", ignoreCase = true) ->
                                "Model '$model' isn't on HF Inference Providers. In Settings → Coding pick Qwen2.5-Coder 32B, Groq Llama, or OpenRouter :free."
                            detail != null -> "$platform HTTP ${httpResponse.status.value}: $detail"
                            else -> "$platform HTTP ${httpResponse.status.value}"
                        }
                    }
                },
            )
        }

        if (response == null) {
            error("Invalid JSON from $platform. Check your token and model in Settings.")
        }

        val content = extractMessageContent(response)
            ?.takeIf { it.isNotBlank() }
            ?: extractReasoningContent(response)
            ?.takeIf { it.isNotBlank() }
            ?: run {
                val err = extractErrorMessage(response)
                error(
                    err?.let { "Empty LLM response from $platform — $it" }
                        ?: "Empty LLM response from $platform. Confirm HF token in Settings, or switch to Groq / OpenRouter :free.",
                )
            }

        val usage = response["usage"]?.jsonObject
        val tokensIn = usage?.get("prompt_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val tokensOut = usage?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        return LlmResult(text = content, tokensIn = tokensIn, tokensOut = tokensOut)
    }

    private fun extractMessageContent(response: JsonObject): String? {
        val message = response["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?: return null
        val contentEl = message["content"] ?: return null
        return when (contentEl) {
            is JsonPrimitive -> contentEl.contentOrNull
            is JsonArray -> contentEl.mapNotNull { part ->
                when (part) {
                    is JsonPrimitive -> part.contentOrNull
                    is JsonObject -> part["text"]?.jsonPrimitive?.contentOrNull
                        ?: part["content"]?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            }.joinToString("").ifBlank { null }
            else -> null
        }
    }

    private fun extractReasoningContent(response: JsonObject): String? {
        val message = response["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?: return null
        message["reasoning"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        val details = message["reasoning_details"]?.jsonArray ?: return null
        return details.mapNotNull { part ->
            when (part) {
                is JsonObject -> part["text"]?.jsonPrimitive?.contentOrNull
                    ?: part["content"]?.jsonPrimitive?.contentOrNull
                is JsonPrimitive -> part.contentOrNull
                else -> null
            }
        }.joinToString("").trim().ifBlank { null }
    }

    private fun extractErrorMessage(response: JsonObject?): String? {
        if (response == null) return null
        response["error"]?.let { err ->
            when (err) {
                is JsonPrimitive -> return err.contentOrNull
                is JsonObject -> {
                    err["message"]?.jsonPrimitive?.contentOrNull?.let { return it }
                    err["code"]?.jsonPrimitive?.contentOrNull?.let { return it }
                }
                else -> Unit
            }
        }
        return response["message"]?.jsonPrimitive?.contentOrNull
    }

    /**
     * Streaming chat, for every provider at once.
     *
     * All four platforms speak the OpenAI chat-completions dialect — Gemini through its
     * `/v1beta/openai/` compatibility layer — so one SSE reader covers Groq, OpenRouter, Gemini
     * and the Hugging Face router rather than four bespoke ones.
     *
     * [onDelta] receives each token run as it lands. The return value is the same [LlmResult] the
     * blocking [chat] produces, so a caller can stream to the screen and still record real token
     * counts afterwards.
     *
     * Why this exists: the app streamed *local* replies token by token and waited in silence for
     * *cloud* ones, so a 70B cloud model felt slower than a 0.6B on-device one. That is backwards,
     * and it was the largest remaining gap against the reference app.
     */
    @Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    suspend fun chatStream(
        platform: CloudPlatform,
        model: String,
        prompt: String,
        apiKey: String,
        system: String = "You are a helpful coding assistant. Return clear, working code with brief explanations.",
        temperature: Double = 0.2,
        onDelta: (String) -> Unit,
    ): LlmResult {
        require(apiKey.isNotBlank()) { "API key required for $platform" }
        require(model.isNotBlank()) { "Model id required" }
        require(prompt.isNotBlank()) { "Prompt is empty" }

        val (url, authHeader) = endpointFor(platform, apiKey)
        val body = chatBody(model, prompt, system, temperature, stream = true)
        val text = StringBuilder()
        var tokensIn = 0
        var tokensOut = 0

        http.preparePost(url) {
            header("Authorization", authHeader)
            header("Accept", "text/event-stream")
            if (platform == CloudPlatform.OPENROUTER) {
                header("HTTP-Referer", "https://github.com/zakir9622/Agentic-AI")
                header("X-Title", "The Lookbook")
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                // Read the error as a whole body: a failed request is JSON, not an event stream,
                // and reusing the blocking path's messages keeps one set of provider diagnostics.
                val raw = response.bodyAsText()
                val detail = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
                error(httpErrorMessage(platform, model, response.status.value, extractErrorMessage(detail), raw))
            }
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith(DATA_PREFIX)) continue
                val payload = line.removePrefix(DATA_PREFIX).trim()
                if (payload.isEmpty()) continue
                // The terminator is a literal sentinel, not JSON — parsing it would throw once
                // per completed stream.
                if (payload == DONE_SENTINEL) break
                val chunk = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject
                    ?: continue
                chunk["usage"]?.jsonObject?.let { usage ->
                    usage["prompt_tokens"]?.jsonPrimitive?.content?.toIntOrNull()?.let { tokensIn = it }
                    usage["completion_tokens"]?.jsonPrimitive?.content?.toIntOrNull()?.let { tokensOut = it }
                }
                val delta = chunk["choices"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("delta")
                    ?.jsonObject
                    // `reasoning_content` is what a thinking model emits before its answer; some
                    // free models emit *only* that, and dropping it renders an empty reply.
                    ?.let { it["content"] ?: it["reasoning_content"] }
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: continue
                if (delta.isEmpty()) continue
                text.append(delta)
                onDelta(delta)
            }
        }

        val body0 = text.toString()
        if (body0.isBlank()) {
            error("Empty LLM response from $platform. Confirm your key and model in Settings.")
        }
        return LlmResult(text = body0, tokensIn = tokensIn, tokensOut = tokensOut)
    }

    /** Chat-completions endpoint and auth header. Shared so the two paths cannot drift apart. */
    private fun endpointFor(platform: CloudPlatform, apiKey: String): Pair<String, String> =
        when (platform) {
            CloudPlatform.GROQ ->
                "https://api.groq.com/openai/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.OPENROUTER ->
                "https://openrouter.ai/api/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.HF_INFERENCE ->
                "https://router.huggingface.co/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.GEMINI ->
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions" to "Bearer $apiKey"
            else -> error(
                "LLM not supported on $platform — pick Groq, OpenRouter, Gemini, or HF Inference in Settings",
            )
        }

    private fun chatBody(
        model: String,
        prompt: String,
        system: String,
        temperature: Double,
        stream: Boolean,
    ): JsonObject = buildJsonObject {
        put("model", model)
        put(
            "messages",
            buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", prompt) })
            },
        )
        put("temperature", temperature.coerceIn(0.0, 1.5))
        put("max_tokens", 2048)
        if (stream) {
            put("stream", true)
            // Groq and OpenRouter only send a usage chunk when asked; without it a streamed run
            // records zero tokens and the API monitor under-reports every cloud reply.
            put("stream_options", buildJsonObject { put("include_usage", true) })
        }
    }

    /** Provider-specific HTTP failure copy, shared by both paths. */
    internal fun httpErrorMessage(
        platform: CloudPlatform,
        model: String,
        status: Int,
        detail: String?,
        raw: String,
    ): String = when (status) {
        401, 403 -> "HF/API token rejected ($status). Use a classic Read/Write token (not fine-grained without Inference), then Save in Settings."
        404 -> "Model not available on free $platform: $model. Switch model in Settings."
        402 -> "HF Inference monthly credits used up. Wait for reset or add Groq/OpenRouter in Settings."
        429 -> "Free-tier rate limit on $platform. Wait a minute or switch model."
        else -> {
            val msg = detail ?: raw.take(200).ifBlank { null }
            when {
                msg?.contains("not supported by any provider", ignoreCase = true) == true ->
                    "Model '$model' isn't on HF Inference Providers. In Settings → Coding pick Qwen2.5-Coder 32B, Groq Llama, or OpenRouter :free."
                msg != null -> "$platform HTTP $status: $msg"
                else -> "$platform HTTP $status"
            }
        }
    }

    private companion object {
        const val DATA_PREFIX = "data:"
        const val DONE_SENTINEL = "[DONE]"
    }

}
