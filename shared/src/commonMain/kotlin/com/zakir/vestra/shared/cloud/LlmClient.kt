package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
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

        val (url, authHeader) = when (platform) {
            CloudPlatform.GROQ ->
                "https://api.groq.com/openai/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.OPENROUTER ->
                "https://openrouter.ai/api/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.HF_INFERENCE ->
                "https://router.huggingface.co/v1/chat/completions" to "Bearer $apiKey"
            else -> error("LLM not supported on $platform — pick Groq, OpenRouter, or HF Inference in Settings")
        }

        // JsonObject (not Map) — serializes without kotlin-reflect on Android.
        val body = buildJsonObject {
            put("model", model)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put("content", system)
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        },
                    )
                },
            )
            put("temperature", temperature.coerceIn(0.0, 1.5))
            put("max_tokens", 2048)
        }

        val response = http.post(url) {
            header("Authorization", authHeader)
            if (platform == CloudPlatform.OPENROUTER) {
                header("HTTP-Referer", "https://github.com/zakir9622/Agentic-AI")
                header("X-Title", "The Lookbook")
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<JsonObject>()

        val content = response["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: error("Empty LLM response from $platform")

        val usage = response["usage"]?.jsonObject
        val tokensIn = usage?.get("prompt_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val tokensOut = usage?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        return LlmResult(text = content, tokensIn = tokensIn, tokensOut = tokensOut)
    }
}
