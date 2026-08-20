package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ReplicateClient(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun predict(
        version: String,
        input: Map<String, String>,
        apiToken: String,
    ): String {
        val create = http.post("https://api.replicate.com/v1/predictions") {
            header("Authorization", "Bearer $apiToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf("version" to version, "input" to input))
        }.body<JsonObject>()

        val id = create["id"]?.jsonPrimitive?.content ?: error("Replicate: no prediction id")
        repeat(90) {
            val status = http.get("https://api.replicate.com/v1/predictions/$id") {
                header("Authorization", "Bearer $apiToken")
            }.body<JsonObject>()
            when (status["status"]?.jsonPrimitive?.content) {
                "succeeded" -> {
                    val output = status["output"]
                    return when {
                        output is kotlinx.serialization.json.JsonArray ->
                            output.firstOrNull()?.jsonPrimitive?.content
                                ?: error("Empty Replicate output")
                        else -> output?.jsonPrimitive?.content ?: error("Unexpected Replicate output")
                    }
                }
                "failed", "canceled" -> error(status["error"]?.jsonPrimitive?.content ?: "Replicate failed")
                else -> delay(2_000)
            }
        }
        error("Replicate prediction timed out")
    }
}
