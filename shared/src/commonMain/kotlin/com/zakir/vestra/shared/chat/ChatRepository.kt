package com.zakir.vestra.shared.chat

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val timestampMs: Long,
    val providerId: String? = null,
)

/**
 * Local conversation memory for News & Chat tab.
 * Last [MAX_MESSAGES] turns persisted on device.
 */
class ChatRepository(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _messages = MutableStateFlow(load())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun append(role: String, text: String, providerId: String? = null) {
        val msg = ChatMessage(
            id = "${System.currentTimeMillis()}-$role",
            role = role,
            text = text.trim(),
            timestampMs = System.currentTimeMillis(),
            providerId = providerId,
        )
        val updated = (_messages.value + msg).takeLast(MAX_MESSAGES)
        _messages.value = updated
        settings.putString(KEY, json.encodeToString(updated))
    }

    fun contextForLlm(maxTurns: Int = 12): List<Pair<String, String>> =
        _messages.value.takeLast(maxTurns).map { it.role to it.text }

    fun clear() {
        _messages.value = emptyList()
        settings.remove(KEY)
    }

    private fun load(): List<ChatMessage> =
        settings.getStringOrNull(KEY)?.let { raw ->
            runCatching { json.decodeFromString<List<ChatMessage>>(raw) }.getOrNull()
        }.orEmpty()

    companion object {
        const val KEY = "chat_history_v1"
        private const val MAX_MESSAGES = 80
    }
}
