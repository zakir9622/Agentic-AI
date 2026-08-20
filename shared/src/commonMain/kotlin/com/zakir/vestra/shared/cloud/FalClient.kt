package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FalClient(private val http: HttpClient) {
    suspend fun predict(
        endpoint: String,
        input: Map<String, String>,
        apiKey: String,
    ): String {
        val response = http.post("https://fal.run/$endpoint") {
            header("Authorization", "Key $apiKey")
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body<JsonObject>()
        return response["image"]?.jsonObject?.get("url")?.jsonPrimitive?.content
            ?: response["video"]?.jsonObject?.get("url")?.jsonPrimitive?.content
            ?: response["video"]?.jsonPrimitive?.content
            ?: response["output"]?.jsonPrimitive?.content
            ?: response["images"]?.let { arr ->
                val first = (arr as? kotlinx.serialization.json.JsonArray)?.firstOrNull() ?: return@let null
                when (first) {
                    is kotlinx.serialization.json.JsonPrimitive -> first.content
                    is JsonObject -> first["url"]?.jsonPrimitive?.content
                    else -> null
                }
            }
            ?: error("FAL response missing media URL")
    }
}
