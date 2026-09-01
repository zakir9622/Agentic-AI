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
import java.io.IOException

val Context.apiKeysDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_api_keys")

/**
 * DataStore-backed repository for secure local persistence of AI Provider API keys
 * (HuggingFace, OpenRouter, Groq, Gemini / Google AI Studio).
 */
class ApiKeyDataStore(
    private val context: Context,
) {
    companion object {
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")
        val KEY_GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    data class StoredApiKeys(
        val hfToken: String? = null,
        val groqApiKey: String? = null,
        val openRouterApiKey: String? = null,
        val geminiApiKey: String? = null,
    )

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
}
