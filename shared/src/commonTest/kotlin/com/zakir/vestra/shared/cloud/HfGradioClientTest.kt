package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HfGradioClientTest {

    @Test
    fun predictSendsJsonObjectNotMap() = runTest {
        var requestBody = ""
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/gradio_api/call/predict") &&
                    request.method.value == "POST" -> {
                    requestBody = (request.body as? TextContent)?.text.orEmpty()
                    respond(
                        """{"event_id":"evt-1"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> respond(
                    "event: complete\ndata: [\"https://example.com/out.png\"]\n\n",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "text/event-stream"),
                )
            }
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val result = HfGradioClient(http).predict(
            spaceHost = "example.hf.space",
            apiName = "predict",
            data = listOf("hello"),
            hfToken = null,
            maxPolls = 2,
            pollDelayMs = 1,
        )
        assertTrue(requestBody.startsWith("{"), requestBody)
        assertTrue(requestBody.contains("\"data\""), requestBody)
        assertEquals("https://example.com/out.png", result.jsonPrimitive.content)
    }

    @Test
    fun predictRejectsEmptyPayload() = runTest {
        val http = HttpClient(MockEngine { respond("", HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        assertFailsWith<IllegalArgumentException> {
            HfGradioClient(http).predict("host", "predict", emptyList(), null)
        }
    }
}
