package com.zakir.vestra.storage

import android.content.Context
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.TokenPortals
import org.json.JSONObject

/**
 * Plain JSON sidecar under Documents/TheLookbook so free-tier API tokens
 * can be restored after a fresh install (SharedPreferences are wiped).
 */
object TokenSidecar {

    data class Payload(
        val version: Int = 1,
        val hfToken: String? = null,
        val groqApiKey: String? = null,
        val openRouterApiKey: String? = null,
        val savedAtEpochMs: Long = 0L,
    )

    fun persist(context: Context, settings: AppSettings): Boolean {
        if (!DurableStorage.hasAllFilesAccess()) return false
        return runCatching {
            DurableStorage.lookbookRoot().mkdirs()
            val file = DurableStorage.tokensSidecar()
            val payload = JSONObject()
                .put("version", 1)
                .put("hfToken", settings.hfToken.value)
                .put("groqApiKey", settings.groqApiKey.value)
                .put("openRouterApiKey", settings.openRouterApiKey.value)
                .put("savedAtEpochMs", System.currentTimeMillis())
            file.writeText(payload.toString())
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
            true
        }.getOrDefault(false)
    }

    fun read(context: Context): Payload? {
        val file = DurableStorage.tokensSidecar()
        if (!file.exists()) return null
        return runCatching {
            val o = JSONObject(file.readText())
            Payload(
                version = o.optInt("version", 1),
                hfToken = o.nullableString("hfToken"),
                groqApiKey = o.nullableString("groqApiKey"),
                openRouterApiKey = o.nullableString("openRouterApiKey"),
                savedAtEpochMs = o.optLong("savedAtEpochMs"),
            )
        }.getOrNull()
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).ifBlank { null }

    /** When prefs are empty after reinstall, hydrate from the sidecar. */
    fun restoreIntoPrefsIfEmpty(context: Context, settings: AppSettings): Boolean {
        val hasPrefs = !settings.hfToken.value.isNullOrBlank() ||
            !settings.groqApiKey.value.isNullOrBlank() ||
            !settings.openRouterApiKey.value.isNullOrBlank()
        if (hasPrefs) return false
        val payload = read(context) ?: return false
        var restored = false
        payload.hfToken?.takeIf { it.isNotBlank() }?.let {
            settings.setHfToken(it)
            restored = true
        }
        payload.groqApiKey?.takeIf { it.isNotBlank() }?.let {
            settings.setGroqApiKey(it)
            restored = true
        }
        payload.openRouterApiKey?.takeIf { it.isNotBlank() }?.let {
            settings.setOpenRouterApiKey(it)
            restored = true
        }
        return restored
    }

    fun clearFile() {
        DurableStorage.tokensSidecar().takeIf { it.exists() }?.delete()
    }

    fun detectClipboardToken(raw: String) = TokenPortals.detectClipboardToken(raw)

    object Portal {
        const val HF = TokenPortals.HF
        const val GROQ = TokenPortals.GROQ
        const val OPENROUTER = TokenPortals.OPENROUTER
    }
}
