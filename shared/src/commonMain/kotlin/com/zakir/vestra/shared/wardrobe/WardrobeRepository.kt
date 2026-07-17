package com.zakir.vestra.shared.wardrobe

import com.zakir.vestra.shared.domain.EngineTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Generation history. Backed by a JSON index next to the generated images —
 * the data is a small append-mostly list, so a database would be overhead.
 * The storage seam is [TextFileStore] so iOS reuses this class untouched.
 */
@Serializable
data class WardrobeEntry(
    val id: String,
    val createdAtEpochMillis: Long,
    val imagePath: String,
    val garmentUri: String,
    val personLabel: String,
    val tier: EngineTier,
    /** Groups the shots of one photoshoot; null on entries from before shot sets. */
    val shootId: String? = null,
)

/** Minimal platform file seam; androidMain/iosMain provide actuals. */
interface TextFileStore {
    fun read(name: String): String?
    fun write(name: String, content: String)
}

class WardrobeRepository(private val store: TextFileStore) {

    private val json = Json { ignoreUnknownKeys = true }
    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<WardrobeEntry>> = _entries

    fun add(entry: WardrobeEntry) {
        update(listOf(entry) + _entries.value)
    }

    fun remove(id: String) {
        update(_entries.value.filterNot { it.id == id })
    }

    private fun update(entries: List<WardrobeEntry>) {
        _entries.value = entries
        store.write(INDEX_FILE, json.encodeToString(entries))
    }

    private fun load(): List<WardrobeEntry> =
        store.read(INDEX_FILE)?.let { content ->
            runCatching { json.decodeFromString<List<WardrobeEntry>>(content) }.getOrNull()
        } ?: emptyList()

    private companion object {
        const val INDEX_FILE = "wardrobe_index.json"
    }
}
