package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.time.EpochClock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [ProviderModelDirectory] against a mock engine — the four providers' real response shapes, no
 * live network.
 *
 * The behaviour worth protecting is the runnable/not-runnable split. Every model a key returns is
 * listed, but only the ones this app has a payload route for may be selected; getting that
 * backwards either hides what a key buys or defers a failure to generation time.
 */
class ProviderModelDirectoryTest {

    private fun client(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(HttpTimeout)
    }

    private fun json(body: String) = MockEngine {
        respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    }

    /** A catalog entry that really exists, so "runnable" has something true to match against. */
    private val groqCatalogEntry = CloudModelCatalog.providers.first { it.platform == CloudPlatform.GROQ }

    @Test
    fun noKeyShortCircuitsWithoutARequest() = runTest {
        var requested = false
        val engine = MockEngine { requested = true; respond("{}", HttpStatusCode.OK) }
        val directory = ProviderModelDirectory(client(engine))

        assertEquals(DirectoryResult.NoKey, directory.refresh(CloudPlatform.GROQ, null))
        assertFalse(requested, "must not call the network without a key")
    }

    @Test
    fun groqListParsesIdsOwnerAndContextWindow() = runTest {
        val body = """
            {"object":"list","data":[
              {"id":"${groqCatalogEntry.endpoint}","owned_by":"Meta","context_window":131072,
               "max_completion_tokens":32768,"active":true},
              {"id":"some-other/model","owned_by":"Other","context_window":8192,"active":true}
            ]}
        """.trimIndent()
        val directory = ProviderModelDirectory(client(json(body)))

        val result = directory.refresh(CloudPlatform.GROQ, "gsk_test")

        val loaded = assertIs<DirectoryResult.Loaded>(result)
        assertEquals(2, loaded.models.size)
        val known = loaded.models.first { it.id == groqCatalogEntry.endpoint }
        assertEquals(131_072, known.contextTokens)
        assertEquals("Meta", known.owner)
        assertTrue(known.runnable, "a model matching a catalog endpoint must be selectable")
        assertEquals(groqCatalogEntry.id, known.catalogProviderId)
    }

    @Test
    fun modelWithNoCatalogRouteIsListedButNotRunnable() = runTest {
        val body = """{"data":[{"id":"unknown/model-v9","owned_by":"Nobody","context_window":4096}]}"""
        val directory = ProviderModelDirectory(client(json(body)))

        val loaded = assertIs<DirectoryResult.Loaded>(directory.refresh(CloudPlatform.GROQ, "gsk_test"))

        val model = loaded.models.single()
        assertFalse(model.runnable)
        assertNull(model.catalogProviderId)
        assertTrue(model.unsupportedReason.isNotBlank(), "an unusable row must say why")
    }

    @Test
    fun groqDropsInactiveModels() = runTest {
        val body = """{"data":[{"id":"a/b","active":false},{"id":"c/d","active":true}]}"""
        val directory = ProviderModelDirectory(client(json(body)))

        val loaded = assertIs<DirectoryResult.Loaded>(directory.refresh(CloudPlatform.GROQ, "gsk_test"))

        assertEquals(listOf("c/d"), loaded.models.map { it.id })
    }

    @Test
    fun openRouterKeepsOnlyFreeModelsAndReadsModalities() = runTest {
        val body = """
            {"data":[
              {"id":"meta/llama-3.3-70b:free","name":"Llama 3.3 70B","description":"A model.",
               "context_length":131072,
               "architecture":{"input_modalities":["text","image"],"output_modalities":["text"]}},
              {"id":"anthropic/paid-model","name":"Paid","context_length":200000}
            ]}
        """.trimIndent()
        val directory = ProviderModelDirectory(client(json(body)))

        val loaded = assertIs<DirectoryResult.Loaded>(directory.refresh(CloudPlatform.OPENROUTER, "sk-or-test"))

        val model = loaded.models.single()
        assertEquals("meta/llama-3.3-70b:free", model.id)
        assertEquals(listOf("text", "image"), model.inputModalities)
        assertEquals(listOf("text"), model.outputModalities)
        assertEquals("Free", model.pricingLabel)
        // The app is free-tier only by policy — a paid model must never reach the picker.
        assertTrue(loaded.models.none { it.id == "anthropic/paid-model" })
    }

    @Test
    fun geminiDropsModelsThatCannotGenerateContent() = runTest {
        val body = """
            {"models":[
              {"name":"models/gemini-2.5-flash","displayName":"Gemini 2.5 Flash",
               "inputTokenLimit":1048576,"outputTokenLimit":8192,
               "supportedGenerationMethods":["generateContent","countTokens"]},
              {"name":"models/text-embedding-004","displayName":"Embedding",
               "supportedGenerationMethods":["embedContent"]}
            ]}
        """.trimIndent()
        val directory = ProviderModelDirectory(client(json(body)))

        val loaded = assertIs<DirectoryResult.Loaded>(directory.refresh(CloudPlatform.GEMINI, "AIzaSyTest"))

        assertEquals(listOf("gemini-2.5-flash"), loaded.models.map { it.id })
        assertEquals(1_048_576, loaded.models.single().contextTokens)
    }

    @Test
    fun unauthorizedIsDistinctFromAnEmptyList() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val directory = ProviderModelDirectory(client(engine))

        val result = directory.refresh(CloudPlatform.GROQ, "gsk_bad")

        assertIs<DirectoryResult.Unauthorized>(result)
    }

    @Test
    fun serverErrorSurfacesAsFailedNotAsAnEmptyList() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        val directory = ProviderModelDirectory(client(engine))

        assertIs<DirectoryResult.Failed>(directory.refresh(CloudPlatform.GROQ, "gsk_test"))
    }

    @Test
    fun malformedBodyDegradesToAnEmptyListRatherThanThrowing() = runTest {
        val directory = ProviderModelDirectory(client(json("not json at all")))

        val loaded = assertIs<DirectoryResult.Loaded>(directory.refresh(CloudPlatform.GROQ, "gsk_test"))

        assertTrue(loaded.models.isEmpty())
    }

    @Test
    fun secondRefreshInsideTheTtlIsServedFromCache() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"data":[{"id":"a/b","active":true}]}""", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val directory = ProviderModelDirectory(client(engine), clock = { 1_000L })

        directory.refresh(CloudPlatform.GROQ, "gsk_test")
        directory.refresh(CloudPlatform.GROQ, "gsk_test")

        assertEquals(1, calls, "a fresh cache entry must not re-hit the network")
        assertNotNull(directory.cached(CloudPlatform.GROQ))
    }

    @Test
    fun forceRefreshBypassesTheCache() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"data":[{"id":"a/b","active":true}]}""", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val directory = ProviderModelDirectory(client(engine), clock = { 1_000L })

        directory.refresh(CloudPlatform.GROQ, "gsk_test")
        directory.refresh(CloudPlatform.GROQ, "gsk_test", force = true)

        assertEquals(2, calls)
    }

    @Test
    fun cacheExpiresAfterTheTtl() = runTest {
        var now = 0L
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"data":[{"id":"a/b","active":true}]}""", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val directory = ProviderModelDirectory(client(engine), clock = EpochClock { now }, ttlMs = 1_000L)

        directory.refresh(CloudPlatform.GROQ, "gsk_test")
        now = 2_000L
        assertNull(directory.cached(CloudPlatform.GROQ), "an entry past its TTL must not be served")
        directory.refresh(CloudPlatform.GROQ, "gsk_test")

        assertEquals(2, calls)
    }

    @Test
    fun invalidateDropsTheCachedListing() = runTest {
        val directory = ProviderModelDirectory(
            client(json("""{"data":[{"id":"a/b","active":true}]}""")),
            clock = { 1_000L },
        )

        directory.refresh(CloudPlatform.GROQ, "gsk_test")
        directory.invalidate(CloudPlatform.GROQ)

        assertNull(directory.cached(CloudPlatform.GROQ))
    }

    @Test
    fun specLineSummarisesWhatTheProviderReported() {
        val model = DirectoryModel(
            id = "meta/llama",
            displayName = "Llama",
            owner = "Meta",
            contextTokens = 131_072,
            inputModalities = listOf("text"),
            outputModalities = listOf("text"),
            pricingLabel = "Free",
        )

        assertEquals("131K ctx · text→text · Meta · Free", model.specLine)
    }
}
