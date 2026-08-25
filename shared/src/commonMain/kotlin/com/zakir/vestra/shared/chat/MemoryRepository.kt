package com.zakir.vestra.shared.chat

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.zakir.vestra.shared.time.EpochClock

@Serializable
data class MemoryFact(
    val id: String,
    val text: String,
    val createdMs: Long,
)

/**
 * Durable facts extracted from News/Chat conversations — "what the assistant remembers"
 * (Part B.1), an on-device port of lookbookweb's persistent-memory concept. This repository
 * only owns storage, dedup, and the cap; the caller (`ChatViewModel`) runs its own extraction
 * prompt through the local chat model and passes the results to [addFacts]. Nothing here ever
 * leaves the device — storage is the same local `Settings` backing every other repository in
 * this app uses.
 */
class MemoryRepository(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _facts = MutableStateFlow(load())
    val facts: StateFlow<List<MemoryFact>> = _facts

    // Monotonic within this repository's lifetime — unlike a per-call-reset index, this keeps
    // ids unique across separate addFacts() calls that land in the same clock millisecond
    // (extraction results can plausibly arrive close together).
    private var idSequence = 0

    /**
     * Adds [candidates] as new facts, deduped case-insensitively against what's already
     * stored, capped at [MAX_NEW_PER_CALL] accepted from this call and [MAX_TOTAL] stored
     * overall (oldest dropped first once over cap). A no-op when nothing new survives dedup.
     */
    fun addFacts(candidates: List<String>) {
        val cleaned = candidates.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        val existingLower = _facts.value.map { it.text.lowercase() }.toMutableSet()
        val accepted = mutableListOf<MemoryFact>()
        for (text in cleaned) {
            if (accepted.size >= MAX_NEW_PER_CALL) break
            val lower = text.lowercase()
            if (!existingLower.add(lower)) continue
            accepted += MemoryFact(
                id = "${EpochClock.System.nowMs()}-${idSequence++}",
                text = text,
                createdMs = EpochClock.System.nowMs(),
            )
        }
        if (accepted.isEmpty()) return
        persist((_facts.value + accepted).takeLast(MAX_TOTAL))
    }

    fun remove(id: String) {
        persist(_facts.value.filterNot { it.id == id })
    }

    /** Edits a fact's text in place; removes it instead when [newText] is blank. */
    fun edit(id: String, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isBlank()) {
            remove(id)
            return
        }
        persist(_facts.value.map { if (it.id == id) it.copy(text = trimmed) else it })
    }

    fun clear() {
        _facts.value = emptyList()
        settings.remove(KEY)
    }

    /** Facts formatted for injection into a system prompt, or blank when nothing is stored. */
    fun contextForSystemPrompt(): String {
        if (_facts.value.isEmpty()) return ""
        return _facts.value.joinToString("\n") { "- ${it.text}" }
    }

    private fun persist(list: List<MemoryFact>) {
        _facts.value = list
        settings.putString(KEY, json.encodeToString(list))
    }

    private fun load(): List<MemoryFact> =
        settings.getStringOrNull(KEY)?.let { raw ->
            runCatching { json.decodeFromString<List<MemoryFact>>(raw) }.getOrNull()
        }.orEmpty()

    companion object {
        const val KEY = "chat_memory_facts_v1"
        const val MAX_NEW_PER_CALL = 5
        const val MAX_TOTAL = 50
    }
}
