package com.zakir.vestra.shared.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Real request/response tests against a mock HTTP engine — no live network calls in this suite,
 * but every branch here is exercised against an actual HTTP status code and header, matching the
 * requests [ProviderConnectivityChecker] really sends, not a simulated delay/random result.
 */
class ProviderConnectivityCheckerTest {

    private fun client(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(HttpTimeout)
    }

    @Test
    fun noKeyReturnsNoKeyWithoutMakingARequest() = runTest {
        var requested = false
        val engine = MockEngine { requested = true; respond("", HttpStatusCode.OK) }
        val checker = ProviderConnectivityChecker(client(engine))

        val result = checker.checkGroq(null)

        assertEquals(ConnectivityResult.NoKey, result)
        assertTrue(!requested, "should not call the network with no key")
    }

    @Test
    fun blankKeyReturnsNoKey() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val result = ProviderConnectivityChecker(client(engine)).checkGroq("   ")
        assertEquals(ConnectivityResult.NoKey, result)
    }

    @Test
    fun successfulResponseReturnsConnectedWithRealLatency() = runTest {
        val engine = MockEngine { respond("""{"data":[]}""", HttpStatusCode.OK) }
        val result = ProviderConnectivityChecker(client(engine)).checkGroq("gsk_real_key")

        val connected = assertIs<ConnectivityResult.Connected>(result)
        assertTrue(connected.latencyMs >= 0, "latency must be a real non-negative measurement")
    }

    @Test
    fun unauthorizedResponseReturnsUnauthorized() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }
        val result = ProviderConnectivityChecker(client(engine)).checkOpenRouter("bad-key")
        assertIs<ConnectivityResult.Unauthorized>(result)
    }

    @Test
    fun forbiddenResponseReturnsUnauthorized() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Forbidden) }
        val result = ProviderConnectivityChecker(client(engine)).checkHuggingFace("revoked-token")
        assertIs<ConnectivityResult.Unauthorized>(result)
    }

    @Test
    fun rateLimitedResponseReturnsRateLimited() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.TooManyRequests) }
        val result = ProviderConnectivityChecker(client(engine)).checkGroq("gsk_real_key")
        assertIs<ConnectivityResult.RateLimited>(result)
    }

    @Test
    fun serverErrorReturnsUnreachable() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val result = ProviderConnectivityChecker(client(engine)).checkGroq("gsk_real_key")
        assertIs<ConnectivityResult.Unreachable>(result)
    }

    @Test
    fun networkExceptionReturnsUnreachable() = runTest {
        val engine = MockEngine { throw java.io.IOException("Connection refused") }
        val result = ProviderConnectivityChecker(client(engine)).checkGroq("gsk_real_key")
        val unreachable = assertIs<ConnectivityResult.Unreachable>(result)
        assertTrue(unreachable.detail.contains("Connection refused"))
    }

    @Test
    fun sendsBearerAuthorizationHeaderWithTheGivenKey() = runTest {
        var authHeader: String? = null
        val engine = MockEngine { request ->
            authHeader = request.headers["Authorization"]
            respond("{}", HttpStatusCode.OK, headersOf("Content-Type", listOf("application/json")))
        }
        ProviderConnectivityChecker(client(engine)).checkHuggingFace("hf_abc123")
        assertEquals("Bearer hf_abc123", authHeader)
    }

    @Test
    fun eachProviderHitsItsOwnRealHost() = runTest {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls.add(request.url.host)
            respond("{}", HttpStatusCode.OK)
        }
        val checker = ProviderConnectivityChecker(client(engine))
        checker.checkGroq("k")
        checker.checkOpenRouter("k")
        checker.checkHuggingFace("k")

        assertEquals(listOf("api.groq.com", "openrouter.ai", "huggingface.co"), urls)
    }
}
