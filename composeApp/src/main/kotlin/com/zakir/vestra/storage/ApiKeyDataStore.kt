package com.zakir.vestra.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zakir.vestra.shared.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

val Context.apiKeysDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_api_keys")

/**
 * DataStore-backed repository for secure local persistence of AI Provider API keys
 * (HuggingFace, OpenRouter, Groq, Gemini / Google AI Studio) and session usage telemetry.
 */
class ApiKeyDataStore(
    private val context: Context,
) {
    companion object {
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")
        val KEY_GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_SESSION_HISTORY = stringPreferencesKey("api_session_history_json")
        val KEY_LIFETIME_TOKENS_IN = stringPreferencesKey("lifetime_tokens_in")
        val KEY_LIFETIME_TOKENS_OUT = stringPreferencesKey("lifetime_tokens_out")
        val KEY_LIFETIME_REQUESTS = stringPreferencesKey("lifetime_requests_count")

        private const val MAX_SESSION_EVENTS = 60
    }

    data class StoredApiKeys(
        val hfToken: String? = null,
        val groqApiKey: String? = null,
        val openRouterApiKey: String? = null,
        val geminiApiKey: String? = null,
    )

    data class SessionUsageRecord(
        val id: String,
        val timestampMs: Long,
        val serviceKey: String, // "GEMINI", "GROQ", "OPENROUTER", "HF", "ON_DEVICE"
        val serviceName: String,
        val modelId: String,
        val modelName: String,
        val capability: String,
        val tokensIn: Int = 0,
        val tokensOut: Int = 0,
        val estCostUsd: Double = 0.0,
        val success: Boolean = true,
        val latencyMs: Long = 0L,
        val note: String = "",
    )

    data class ServiceUsageSummary(
        val serviceKey: String,
        val serviceName: String,
        val isConfigured: Boolean,
        val requestCount: Int = 0,
        val successCount: Int = 0,
        val tokensIn: Int = 0,
        val tokensOut: Int = 0,
        val totalTokens: Int = 0,
        val estCostUsd: Double = 0.0,
        val lastUsedMs: Long? = null,
        /** Mean round-trip over this service's recorded runs; 0 when it has none. */
        val avgLatencyMs: Long = 0L,
    ) {
        /** 0f..1f over recorded runs. Null when nothing ran — "no data" is not "0% success". */
        val successRate: Float? get() = if (requestCount == 0) null else successCount.toFloat() / requestCount
    }

    data class ApiUsageDashboardData(
        val totalRequests: Int = 0,
        val successfulRequests: Int = 0,
        val totalTokensIn: Int = 0,
        val totalTokensOut: Int = 0,
        val totalTokens: Int = 0,
        val totalEstCostUsd: Double = 0.0,
        val services: List<ServiceUsageSummary> = emptyList(),
        val sessionHistory: List<SessionUsageRecord> = emptyList(),
        /** Mean round-trip across every recorded run; 0 when there are none. */
        val avgLatencyMs: Long = 0L,
    ) {
        /**
         * 0f..1f over recorded runs, or null when nothing has run yet. Deliberately nullable:
         * rendering a fresh install as "0% success" is a lie, and the caller needs to be able
         * to tell "no data" from "everything failed".
         */
        val successRate: Float? get() = if (totalRequests == 0) null else successfulRequests.toFloat() / totalRequests
    }

    private fun parseRecordsFromJson(rawJson: String?): List<SessionUsageRecord> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<SessionUsageRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                list.add(
                    SessionUsageRecord(
                        id = obj.optString("id", ""),
                        timestampMs = obj.optLong("timestampMs", 0L),
                        serviceKey = obj.optString("serviceKey", "OTHER"),
                        serviceName = obj.optString("serviceName", ""),
                        modelId = obj.optString("modelId", ""),
                        modelName = obj.optString("modelName", ""),
                        capability = obj.optString("capability", ""),
                        tokensIn = obj.optInt("tokensIn", 0),
                        tokensOut = obj.optInt("tokensOut", 0),
                        estCostUsd = obj.optDouble("estCostUsd", 0.0),
                        success = obj.optBoolean("success", true),
                        latencyMs = obj.optLong("latencyMs", 0L),
                        note = obj.optString("note", ""),
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun serializeRecordsToJson(records: List<SessionUsageRecord>): String {
        val jsonArray = JSONArray()
        for (item in records) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestampMs", item.timestampMs)
            obj.put("serviceKey", item.serviceKey)
            obj.put("serviceName", item.serviceName)
            obj.put("modelId", item.modelId)
            obj.put("modelName", item.modelName)
            obj.put("capability", item.capability)
            obj.put("tokensIn", item.tokensIn)
            obj.put("tokensOut", item.tokensOut)
            obj.put("estCostUsd", item.estCostUsd)
            obj.put("success", item.success)
            obj.put("latencyMs", item.latencyMs)
            obj.put("note", item.note)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    val apiKeysFlow: Flow<StoredApiKeys> = context.apiKeysDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            StoredApiKeys(
                hfToken = prefs[KEY_HF_TOKEN],
                groqApiKey = prefs[KEY_GROQ_API_KEY],
                openRouterApiKey = prefs[KEY_OPENROUTER_API_KEY],
                geminiApiKey = prefs[KEY_GEMINI_API_KEY],
            )
        }

    val usageDashboardFlow: Flow<ApiUsageDashboardData> = context.apiKeysDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val hfConfigured = !prefs[KEY_HF_TOKEN].isNullOrBlank()
            val groqConfigured = !prefs[KEY_GROQ_API_KEY].isNullOrBlank()
            val openRouterConfigured = !prefs[KEY_OPENROUTER_API_KEY].isNullOrBlank()
            val geminiConfigured = !prefs[KEY_GEMINI_API_KEY].isNullOrBlank()

            val rawHistory = prefs[KEY_SESSION_HISTORY]
            val events: List<SessionUsageRecord> = parseRecordsFromJson(rawHistory)

            // Aggregate by service
            val serviceConfigs = listOf(
                Triple("GEMINI", "Google Gemini", geminiConfigured),
                Triple("HF", "Hugging Face", hfConfigured),
                Triple("GROQ", "Groq", groqConfigured),
                Triple("OPENROUTER", "OpenRouter", openRouterConfigured),
                Triple("ON_DEVICE", "On-Device", true),
            )

            val serviceSummaries = serviceConfigs.map { (key, name, configured) ->
                val serviceEvents = events.filter { it.serviceKey.equals(key, ignoreCase = true) }
                val reqs = serviceEvents.size
                val success = serviceEvents.count { it.success }
                val tokIn = serviceEvents.sumOf { it.tokensIn }
                val tokOut = serviceEvents.sumOf { it.tokensOut }
                val cost = serviceEvents.sumOf { it.estCostUsd }
                val lastUsed = serviceEvents.maxOfOrNull { it.timestampMs }
                // Only runs that actually reported a latency count toward the mean; a run
                // recorded with latencyMs = 0 never measured, and averaging those in would
                // drag every provider's number toward zero.
                val timed = serviceEvents.filter { it.latencyMs > 0L }
                val avgLatency = if (timed.isEmpty()) 0L else timed.sumOf { it.latencyMs } / timed.size

                ServiceUsageSummary(
                    serviceKey = key,
                    serviceName = name,
                    isConfigured = configured,
                    requestCount = reqs,
                    successCount = success,
                    tokensIn = tokIn,
                    tokensOut = tokOut,
                    totalTokens = tokIn + tokOut,
                    estCostUsd = cost,
                    lastUsedMs = lastUsed,
                    avgLatencyMs = avgLatency,
                )
            }

            val totalReqs = events.size
            val successfulReqs = events.count { it.success }
            val totalIn = events.sumOf { it.tokensIn }
            val totalOut = events.sumOf { it.tokensOut }
            val totalCost = events.sumOf { it.estCostUsd }
            val timedEvents = events.filter { it.latencyMs > 0L }
            val avgLatency = if (timedEvents.isEmpty()) 0L else timedEvents.sumOf { it.latencyMs } / timedEvents.size

            ApiUsageDashboardData(
                totalRequests = totalReqs,
                successfulRequests = successfulReqs,
                totalTokensIn = totalIn,
                totalTokensOut = totalOut,
                totalTokens = totalIn + totalOut,
                totalEstCostUsd = totalCost,
                services = serviceSummaries,
                sessionHistory = events,
                avgLatencyMs = avgLatency,
            )
        }

    suspend fun recordSessionUsage(
        serviceKey: String,
        serviceName: String,
        modelId: String,
        modelName: String,
        capability: String,
        tokensIn: Int = 0,
        tokensOut: Int = 0,
        estCostUsd: Double = 0.0,
        success: Boolean = true,
        latencyMs: Long = 0L,
        note: String = "",
    ) {
        val event = SessionUsageRecord(
            id = "${System.currentTimeMillis()}-${modelId.take(12)}",
            timestampMs = System.currentTimeMillis(),
            serviceKey = serviceKey,
            serviceName = serviceName,
            modelId = modelId,
            modelName = modelName,
            capability = capability,
            tokensIn = tokensIn.coerceAtLeast(0),
            tokensOut = tokensOut.coerceAtLeast(0),
            estCostUsd = estCostUsd,
            success = success,
            latencyMs = latencyMs,
            note = note,
        )

        context.apiKeysDataStore.edit { prefs ->
            val rawHistory = prefs[KEY_SESSION_HISTORY]
            val currentList = parseRecordsFromJson(rawHistory)
            val updatedList = (listOf(event) + currentList).take(MAX_SESSION_EVENTS)
            prefs[KEY_SESSION_HISTORY] = serializeRecordsToJson(updatedList)

            val currentIn = (prefs[KEY_LIFETIME_TOKENS_IN]?.toLongOrNull() ?: 0L) + tokensIn
            val currentOut = (prefs[KEY_LIFETIME_TOKENS_OUT]?.toLongOrNull() ?: 0L) + tokensOut
            val currentReqs = (prefs[KEY_LIFETIME_REQUESTS]?.toIntOrNull() ?: 0) + 1

            prefs[KEY_LIFETIME_TOKENS_IN] = currentIn.toString()
            prefs[KEY_LIFETIME_TOKENS_OUT] = currentOut.toString()
            prefs[KEY_LIFETIME_REQUESTS] = currentReqs.toString()
        }
    }

    suspend fun clearSessionUsageHistory() {
        context.apiKeysDataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_HISTORY)
        }
    }

    suspend fun saveHfToken(token: String?) {
        context.apiKeysDataStore.edit { prefs ->
            if (token.isNullOrBlank()) prefs.remove(KEY_HF_TOKEN)
            else prefs[KEY_HF_TOKEN] = token.trim()
        }
    }

    suspend fun saveGroqApiKey(key: String?) {
        context.apiKeysDataStore.edit { prefs ->
            if (key.isNullOrBlank()) prefs.remove(KEY_GROQ_API_KEY)
            else prefs[KEY_GROQ_API_KEY] = key.trim()
        }
    }

    suspend fun saveOpenRouterApiKey(key: String?) {
        context.apiKeysDataStore.edit { prefs ->
            if (key.isNullOrBlank()) prefs.remove(KEY_OPENROUTER_API_KEY)
            else prefs[KEY_OPENROUTER_API_KEY] = key.trim()
        }
    }

    suspend fun saveGeminiApiKey(key: String?) {
        context.apiKeysDataStore.edit { prefs ->
            if (key.isNullOrBlank()) prefs.remove(KEY_GEMINI_API_KEY)
            else prefs[KEY_GEMINI_API_KEY] = key.trim()
        }
    }

    suspend fun saveAll(
        hfToken: String?,
        groqApiKey: String?,
        openRouterApiKey: String?,
        geminiApiKey: String?,
    ) {
        context.apiKeysDataStore.edit { prefs ->
            if (hfToken.isNullOrBlank()) prefs.remove(KEY_HF_TOKEN) else prefs[KEY_HF_TOKEN] = hfToken.trim()
            if (groqApiKey.isNullOrBlank()) prefs.remove(KEY_GROQ_API_KEY) else prefs[KEY_GROQ_API_KEY] = groqApiKey.trim()
            if (openRouterApiKey.isNullOrBlank()) prefs.remove(KEY_OPENROUTER_API_KEY) else prefs[KEY_OPENROUTER_API_KEY] = openRouterApiKey.trim()
            if (geminiApiKey.isNullOrBlank()) prefs.remove(KEY_GEMINI_API_KEY) else prefs[KEY_GEMINI_API_KEY] = geminiApiKey.trim()
        }
    }

    suspend fun clearAll() {
        context.apiKeysDataStore.edit { prefs ->
            prefs.remove(KEY_HF_TOKEN)
            prefs.remove(KEY_GROQ_API_KEY)
            prefs.remove(KEY_OPENROUTER_API_KEY)
            prefs.remove(KEY_GEMINI_API_KEY)
        }
    }

    /**
     * Synchronizes DataStore keys into AppSettings and vice versa if newly initialized.
     */
    suspend fun syncWithAppSettings(appSettings: AppSettings) {
        val stored = apiKeysFlow.firstOrNull() ?: StoredApiKeys()
        if (!stored.hfToken.isNullOrBlank() && appSettings.hfToken.value.isNullOrBlank()) {
            appSettings.setHfToken(stored.hfToken)
        } else if (stored.hfToken.isNullOrBlank() && !appSettings.hfToken.value.isNullOrBlank()) {
            saveHfToken(appSettings.hfToken.value)
        }

        if (!stored.groqApiKey.isNullOrBlank() && appSettings.groqApiKey.value.isNullOrBlank()) {
            appSettings.setGroqApiKey(stored.groqApiKey)
        } else if (stored.groqApiKey.isNullOrBlank() && !appSettings.groqApiKey.value.isNullOrBlank()) {
            saveGroqApiKey(appSettings.groqApiKey.value)
        }

        if (!stored.openRouterApiKey.isNullOrBlank() && appSettings.openRouterApiKey.value.isNullOrBlank()) {
            appSettings.setOpenRouterApiKey(stored.openRouterApiKey)
        } else if (stored.openRouterApiKey.isNullOrBlank() && !appSettings.openRouterApiKey.value.isNullOrBlank()) {
            saveOpenRouterApiKey(appSettings.openRouterApiKey.value)
        }

        if (!stored.geminiApiKey.isNullOrBlank() && appSettings.geminiApiKey.value.isNullOrBlank()) {
            appSettings.setGeminiApiKey(stored.geminiApiKey)
        } else if (stored.geminiApiKey.isNullOrBlank() && !appSettings.geminiApiKey.value.isNullOrBlank()) {
            saveGeminiApiKey(appSettings.geminiApiKey.value)
        }
    }

    /**
     * Synchronizes historical events from UsageLedger if DataStore history is unpopulated.
     */
    suspend fun syncWithUsageLedger(usageLedger: com.zakir.vestra.shared.usage.UsageLedger) {
        val current = usageDashboardFlow.firstOrNull() ?: ApiUsageDashboardData()
        if (current.sessionHistory.isEmpty() && usageLedger.events.value.isNotEmpty()) {
            usageLedger.events.value.reversed().forEach { event ->
                val serviceKey = when {
                    event.providerId.startsWith("local-") -> "ON_DEVICE"
                    event.platform.contains("GEMINI", ignoreCase = true) -> "GEMINI"
                    event.platform.contains("GROQ", ignoreCase = true) -> "GROQ"
                    event.platform.contains("OPENROUTER", ignoreCase = true) -> "OPENROUTER"
                    event.platform.contains("HF", ignoreCase = true) -> "HF"
                    else -> "GEMINI"
                }
                recordSessionUsage(
                    serviceKey = serviceKey,
                    serviceName = event.providerName,
                    modelId = event.providerId,
                    modelName = event.providerName,
                    capability = event.capability,
                    tokensIn = event.tokensIn,
                    tokensOut = event.tokensOut,
                    estCostUsd = event.estCostUsd,
                    success = event.success,
                    note = event.note,
                )
            }
        }
    }
}
