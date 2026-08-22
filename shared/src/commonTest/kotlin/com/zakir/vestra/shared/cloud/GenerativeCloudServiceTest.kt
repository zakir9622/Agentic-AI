package com.zakir.vestra.shared.cloud

import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class TestMemorySettings : Settings {
    private val map = mutableMapOf<String, Any?>()
    override val keys: Set<String> get() = map.keys
    override val size: Int get() = map.size
    override fun clear() = map.clear()
    override fun remove(key: String) { map.remove(key) }
    override fun hasKey(key: String): Boolean = map.containsKey(key)
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = map[key] as? Int
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = map[key] as? Long
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = map[key] as? String
    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = map[key] as? Float
    override fun putDouble(key: String, value: Double) { map[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
}

private class FakeIo : CloudImageIo {
    override suspend fun loadImageBytes(person: com.zakir.vestra.shared.domain.PersonSource): ByteArray? =
        byteArrayOf(1, 2, 3)

    override suspend fun loadImageBytes(uri: String): ByteArray? = byteArrayOf(1, 2, 3)

    override fun toDataUrl(jpegBytes: ByteArray): String = "data:image/jpeg;base64,abc"

    override suspend fun downloadResult(urlOrPath: String, spaceHost: String?): String = "/tmp/out.png"
}

class GenerativeCloudServiceTest {

    @Test
    fun imageGenDoesNotFallbackToDegradedSdxlWhenFluxFails() = runTest {
        val hostsCalled = mutableListOf<String>()
        val engine = MockEngine { request ->
            hostsCalled += request.url.host
            if (request.method.value == "POST") {
                respond(
                    """{"event_id":"evt-${hostsCalled.size}"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(
                    "event: error\ndata: null\n\n",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "text/event-stream"),
                )
            }
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settings = AppSettings(TestMemorySettings()).apply {
            setImageGenProvider("flux-schnell-hf")
        }
        val service = GenerativeCloudService(http, FakeIo(), settings, UsageLedger(TestMemorySettings()))
        val states = service.generateImage("abaya lookbook", referenceUri = null).toList()
        val failed = states.filterIsInstance<GenerativeState.Failed>().single()
        assertTrue(failed.message.contains("ZeroGPU", ignoreCase = true), failed.message)
        assertTrue(hostsCalled.all { it.contains("flux") }, "Should only hit FLUX, got $hostsCalled")
        assertTrue(hostsCalled.none { it.contains("sdxl", ignoreCase = true) })
    }

    @Test
    fun codeGenFallsBackFromGroqToHfWhenGroqKeyMissing() = runTest {
        var modelSeen = ""
        val engine = MockEngine { request ->
            val body = (request.body as? TextContent)?.text.orEmpty()
            modelSeen = when {
                body.contains("llama-3.3-70b") -> "groq"
                body.contains("Qwen2.5-Coder") -> "hf"
                else -> "other"
            }
            respond(
                """{"choices":[{"message":{"content":"print('hi')"}}],"usage":{"prompt_tokens":1,"completion_tokens":2}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settings = AppSettings(TestMemorySettings()).apply {
            setCodeProvider("llama33-70b-groq")
            setHfToken("hf_test")
        }
        val service = GenerativeCloudService(http, FakeIo(), settings, UsageLedger(TestMemorySettings()))
        val states = service.generateCode("hello world").toList()
        val ready = states.filterIsInstance<GenerativeState.CodeReady>().single()
        assertEquals("hf", modelSeen)
        assertEquals("qwen25-coder-hf", ready.providerId)
    }
}
