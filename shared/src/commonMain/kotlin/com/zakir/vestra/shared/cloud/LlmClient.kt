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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    ): LlmResult {
        val (url, authHeader) = when (platform) {
            CloudPlatform.GROQ ->
                "https://api.groq.com/openai/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.OPENROUTER ->
                "https://openrouter.ai/api/v1/chat/completions" to "Bearer $apiKey"
            CloudPlatform.HF_INFERENCE ->
                "https://router.huggingface.co/v1/chat/completions" to "Bearer $apiKey"
            else -> error("LLM not supported on $platform")
        }
        val body = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to system),
                mapOf("role" to "user", "content" to prompt),
            ),
            "temperature" to 0.2,
            "max_tokens" to 2048,
        )
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
            ?: error("Empty LLM response")

        val usage = response["usage"]?.jsonObject
        val tokensIn = usage?.get("prompt_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val tokensOut = usage?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        return LlmResult(text = content, tokensIn = tokensIn, tokensOut = tokensOut)
    }
}
