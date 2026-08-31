package com.zakir.vestra.shared.cloud

import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.engine.local.LocalCodeGenerator
import com.zakir.vestra.shared.engine.local.LocalCodeResult
import com.zakir.vestra.shared.usage.UsageLedger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression guard for a real device-reported leak: with no credential configured for the
 * selected cloud provider and an on-device model selected, a local pack that failed at runtime
 * silently fell through to the cloud fallback chain and generated an image over the network
 * anyway.
 *
 * preflight() alone could not catch this — it deliberately lets a local selection through, so
 * the enforcement has to live at the point of the network call itself, gated on
 * [com.zakir.vestra.shared.settings.AppSettings.cloudUsable] (there is no separate on/off
 * switch — reachability is decided per-request from whether the provider actually has a usable
 * credential). These tests assert on the MockEngine request count, so "no cloud request was
 * made" is proven, not inferred from the emitted state.
 */
class CloudToggleEnforcementTest {

    private class MemorySettings : Settings {
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

    private class TestIo : CloudImageIo {
        override suspend fun loadImageBytes(person: com.zakir.vestra.shared.domain.PersonSource): ByteArray? =
            byteArrayOf(1, 2, 3)
        override suspend fun loadImageBytes(uri: String): ByteArray? = byteArrayOf(1, 2, 3)
        override fun toDataUrl(jpegBytes: ByteArray): String = "data:image/jpeg;base64,abc"
        override suspend fun downloadResult(urlOrPath: String, spaceHost: String?): String = "/tmp/out.png"
        override fun resolveLocalPath(uri: String): String? = null
    }

    /**
     * Reports ready, then fails at generate — exactly the device case that leaked. CODE is used
     * (not IMAGE_GEN) because its default cloud fallback, llama33-70b-groq, actually requires a
     * credential — IMAGE_GEN's default (flux-schnell-hf) is a genuinely free HF Space that's
     * correctly reachable with zero configuration under the new credential-based gate.
     */
    private object ReadyButFailingLocalCode : LocalCodeGenerator {
        const val REASON = "Local Gemma weights incomplete (gemma3.tflite)."
        override fun providerId(): String = "local-gemma-4-e2b-v1"
        override fun isReady(): Boolean = true
        override fun generate(prompt: String, system: String): LocalCodeResult =
            LocalCodeResult.Unavailable(REASON)
    }

    private object AppSettingsFactory {
        /** [withGroqKey] false leaves the selected provider's credential unset. */
        fun build(withGroqKey: Boolean): com.zakir.vestra.shared.settings.AppSettings =
            com.zakir.vestra.shared.settings.AppSettings(MemorySettings()).apply {
                setLocalGenerator(AiCapability.CODE, "local-gemma-4-e2b-v1")
                if (withGroqKey) {
                    setGroqApiKey("groq_test")
                    confirmCloudConsentFromApiKeyEntry()
                }
            }
    }

    @Test
    fun localFailureDoesNotReachCloudWithNoCredentialConfigured() = runTest {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("{}", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settings = AppSettingsFactory.build(withGroqKey = false)
        val service = GenerativeCloudService(
            http,
            TestIo(),
            settings,
            UsageLedger(MemorySettings()),
            localCode = ReadyButFailingLocalCode,
        )

        val states = service.generateCode("write a hello world").toList()

        assertEquals(0, requests, "cloud was contacted despite no credential being configured")
        assertTrue(states.none { it is GenerativeState.CodeReady }, "produced code via cloud")
        val failed = states.filterIsInstance<GenerativeState.Failed>().single()
        // The user must be told what actually broke on-device, not a generic message.
        assertTrue(failed.message.contains(ReadyButFailingLocalCode.REASON), failed.message)
        assertTrue(failed.message.contains("API key"), failed.message)
    }

    @Test
    fun addingTheCredentialRestoresTheCloudFallback() = runTest {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("{}", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val settings = AppSettingsFactory.build(withGroqKey = true)
        val service = GenerativeCloudService(
            http,
            TestIo(),
            settings,
            UsageLedger(MemorySettings()),
            localCode = ReadyButFailingLocalCode,
        )

        service.generateCode("write a hello world").toList()

        // The point is only that the gate stops blocking; the mock response is not real code.
        assertTrue(requests > 0, "cloud fallback should run once a credential is configured")
    }
}
