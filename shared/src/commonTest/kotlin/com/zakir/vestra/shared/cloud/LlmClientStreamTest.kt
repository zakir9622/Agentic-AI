package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * The SSE reader behind streamed cloud replies.
 *
 * One implementation serves Groq, OpenRouter, Gemini and the Hugging Face router, because all
 * four speak the OpenAI chat-completions dialect — so a parsing bug here is a bug on every
 * provider at once, and these cases are the only thing standing between that and a device.
 *
 * The invariant worth pinning is that **the assembled text equals the concatenated deltas**: a
 * reader that drops a chunk produces a reply that is subtly missing a clause, which is far worse
 * than one that fails loudly.
 */
class LlmClientStreamTest {

    private fun streamingClient(body: String): HttpClient = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        },
    ) { install(HttpTimeout); install(ContentNegotiation) { json() } }

    private fun sse(vararg lines: String) = lines.joinToString("\n") + "\n"

    private fun chunk(content: String) =
        """data: {"choices":[{"delta":{"content":"$content"}}]}"""

    @Test
    fun deltasAreEmittedInOrderAndAssembledIntoTheFullReply() = runTest {
        val client = LlmClient(streamingClient(sse(chunk("Hello"), chunk(", "), chunk("world"), "data: [DONE]")))
        val seen = mutableListOf<String>()

        val result = client.chatStream(
            platform = CloudPlatform.GROQ,
            model = "llama-3.3-70b",
            prompt = "hi",
            apiKey = "k",
            onDelta = { seen += it },
        )

        assertEquals(listOf("Hello", ", ", "world"), seen)
        assertEquals("Hello, world", result.text)
        assertEquals(seen.joinToString(""), result.text, "assembled text must equal the deltas")
    }

    @Test
    fun theDoneSentinelTerminatesWithoutBeingParsedAsJson() = runTest {
        // "[DONE]" is a literal, not JSON. Parsing it would throw once per completed stream.
        val client = LlmClient(streamingClient(sse(chunk("done"), "data: [DONE]", chunk("after"))))
        val result = client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = {})
        assertEquals("done", result.text, "nothing after the sentinel should be read")
    }

    @Test
    fun keepAliveAndCommentLinesAreIgnored() = runTest {
        val client = LlmClient(
            streamingClient(sse("", ": ping", chunk("a"), "", ": keep-alive", chunk("b"), "data: [DONE]")),
        )
        assertEquals("ab", client.chatStream(CloudPlatform.OPENROUTER, "m", "p", "k", onDelta = {}).text)
    }

    @Test
    fun aMalformedChunkIsSkippedRatherThanKillingTheStream() = runTest {
        // A truncated frame must cost one token run, not the whole reply.
        val client = LlmClient(streamingClient(sse(chunk("good"), "data: {not json", chunk(" tail"), "data: [DONE]")))
        assertEquals("good tail", client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = {}).text)
    }

    @Test
    fun aRoleOnlyOpeningChunkEmitsNothing() = runTest {
        // Every OpenAI-compatible stream opens with a delta carrying only {"role":"assistant"}.
        val client = LlmClient(
            streamingClient(
                sse("""data: {"choices":[{"delta":{"role":"assistant"}}]}""", chunk("body"), "data: [DONE]"),
            ),
        )
        val seen = mutableListOf<String>()
        val result = client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = { seen += it })
        assertEquals(listOf("body"), seen)
        assertEquals("body", result.text)
    }

    @Test
    fun reasoningOnlyModelsStillProduceAReply() = runTest {
        // Some free thinking models emit reasoning_content and never content; dropping it renders
        // an empty bubble on a request the user was billed for.
        val client = LlmClient(
            streamingClient(
                sse("""data: {"choices":[{"delta":{"reasoning_content":"thinking aloud"}}]}""", "data: [DONE]"),
            ),
        )
        assertEquals("thinking aloud", client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = {}).text)
    }

    @Test
    fun usageChunkPopulatesTokenCounts() = runTest {
        val client = LlmClient(
            streamingClient(
                sse(
                    chunk("hi"),
                    """data: {"choices":[],"usage":{"prompt_tokens":11,"completion_tokens":4}}""",
                    "data: [DONE]",
                ),
            ),
        )
        val result = client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = {})
        assertEquals(11, result.tokensIn)
        assertEquals(4, result.tokensOut)
    }

    @Test
    fun anEmptyStreamFailsLoudlyRatherThanReturningABlankReply() = runTest {
        val client = LlmClient(streamingClient(sse("data: [DONE]")))
        val error = assertFailsWith<IllegalStateException> {
            client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = {})
        }
        assertTrue(error.message.orEmpty().contains("Empty LLM response"))
    }

    @Test
    fun anHttpErrorCarriesTheProviderSpecificMessage() = runTest {
        val client = LlmClient(
            HttpClient(
                MockEngine {
                    respond(
                        content = """{"error":{"message":"invalid api key"}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) { install(HttpTimeout); install(ContentNegotiation) { json() } },
        )
        val error = assertFailsWith<IllegalStateException> {
            client.chatStream(CloudPlatform.GROQ, "m", "p", "k", onDelta = {})
        }
        assertTrue(
            error.message.orEmpty().contains("token rejected"),
            "a 401 must reuse the blocking path's guidance, got: ${error.message}",
        )
    }

    @Test
    fun anUnsupportedPlatformIsRejectedBeforeAnyRequest() = runTest {
        var requested = false
        val client = LlmClient(
            HttpClient(MockEngine { requested = true; respond("", HttpStatusCode.OK) }) {
                install(HttpTimeout); install(ContentNegotiation) { json() }
            },
        )
        assertFailsWith<IllegalStateException> {
            client.chatStream(CloudPlatform.HF_SPACE, "m", "p", "k", onDelta = {})
        }
        assertTrue(!requested, "an unroutable platform must not reach the network")
    }
}
