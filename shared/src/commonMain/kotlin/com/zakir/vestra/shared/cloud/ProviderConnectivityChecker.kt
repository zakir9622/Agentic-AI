package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.time.EpochClock
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * Result of an actual network round-trip to a cloud provider — never simulated. Every branch here
 * corresponds to a real HTTP outcome (a real status code or a real thrown exception), not a
 * `delay()` followed by a random number. See `docs/DRAWBACKS.md`'s note on why the source repo's
 * `ModelConfigScreen.kt` (GoogleLookBookUI) was not ported as-is: this class exists to provide
 * the real check that screen was missing.
 */
sealed interface ConnectivityResult {
    /** [latencyMs] is a real measured wall-clock round-trip, not a random placeholder. */
    data class Connected(val latencyMs: Long) : ConnectivityResult
    data class Unauthorized(val detail: String) : ConnectivityResult
    data class RateLimited(val detail: String) : ConnectivityResult
    data class Unreachable(val detail: String) : ConnectivityResult
    data object NoKey : ConnectivityResult
}

/**
 * Real, read-only connectivity probes against the same hosts this app's actual generation code
 * calls (see [LlmClient], [FreeCloudDiscovery]) — a cheap "list models" / "who am I" request per
 * provider, not a generation call, so testing a key costs nothing and has no side effects.
 */
class ProviderConnectivityChecker(
    private val http: HttpClient,
    private val clock: EpochClock = EpochClock.System,
) {
    suspend fun checkGroq(apiKey: String?): ConnectivityResult =
        probe(apiKey) { key ->
            http.get("https://api.groq.com/openai/v1/models") {
                header(HttpHeaders.Authorization, "Bearer $key")
            }
        }

    suspend fun checkOpenRouter(apiKey: String?): ConnectivityResult =
        probe(apiKey) { key ->
            http.get("https://openrouter.ai/api/v1/auth/key") {
                header(HttpHeaders.Authorization, "Bearer $key")
            }
        }

    suspend fun checkHuggingFace(token: String?): ConnectivityResult =
        probe(token) { key ->
            http.get("https://huggingface.co/api/whoami-v2") {
                header(HttpHeaders.Authorization, "Bearer $key")
            }
        }

    private suspend fun probe(
        key: String?,
        call: suspend (String) -> HttpResponse,
    ): ConnectivityResult {
        if (key.isNullOrBlank()) return ConnectivityResult.NoKey
        val startedAt = clock.nowMs()
        return runCatching {
            val response = call(key)
            val elapsed = clock.nowMs() - startedAt
            when {
                response.status.isSuccess() -> ConnectivityResult.Connected(elapsed)
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ->
                    ConnectivityResult.Unauthorized("Rejected (${response.status.value}) — check the key")
                response.status == HttpStatusCode.TooManyRequests ->
                    ConnectivityResult.RateLimited("Rate limited (429) — try again shortly")
                else -> ConnectivityResult.Unreachable("Unexpected response (${response.status.value})")
            }
        }.getOrElse { err ->
            ConnectivityResult.Unreachable(err.message?.take(160) ?: "Network error")
        }
    }
}
