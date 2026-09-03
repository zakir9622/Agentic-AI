package com.zakir.vestra.shared.chat

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The conversation store, with one invariant above all others: **starting a new chat never
 * destroys an old one.**
 *
 * That is not a hypothetical. The previous version of this class stored a single list under one
 * key, and the app's "New chat" button called `clear()`, which did `settings.remove(KEY)` — an
 * unconfirmed permanent delete, one tap from the top bar, of the only conversation the app could
 * hold. Every test below exists because of that.
 */
class ChatRepositoryTest {

    /**
     * In-memory [Settings]. Matches the fake `CatalogInvariantsTest` already uses rather than
     * pulling in `multiplatform-settings-test` for one type.
     */
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

    private fun repo() = ChatRepository(MemorySettings())

    @Test
    fun `new conversation files the old one instead of deleting it`() {
        val repo = repo()
        repo.append("user", "capsule wardrobe for a week of travel")
        repo.append("assistant", "Here are five pieces…")

        repo.newConversation()

        assertTrue(repo.messages.value.isEmpty(), "the new conversation starts empty")
        assertEquals(1, repo.conversations.value.size, "the old conversation must still exist")
        assertEquals("capsule wardrobe for a week of travel", repo.conversations.value.first().title)
    }

    @Test
    fun `a filed conversation can be reopened with its messages intact`() {
        val repo = repo()
        repo.append("user", "first thread")
        repo.append("assistant", "reply one")
        val firstId = repo.activeId.value
        repo.newConversation()
        repo.append("user", "second thread")

        repo.openConversation(firstId)

        assertEquals(listOf("first thread", "reply one"), repo.messages.value.map { it.text })
        assertEquals(2, repo.conversations.value.size, "switching must not drop the other thread")
    }

    @Test
    fun `switching away persists what was typed in the conversation being left`() {
        val repo = repo()
        repo.append("user", "alpha")
        val alpha = repo.activeId.value
        repo.newConversation()
        repo.append("user", "beta")
        val beta = repo.activeId.value

        repo.openConversation(alpha)
        repo.openConversation(beta)

        assertEquals(listOf("beta"), repo.messages.value.map { it.text })
    }

    @Test
    fun `an empty conversation is never filed`() {
        val repo = repo()
        repo.newConversation()
        repo.newConversation()
        assertTrue(repo.conversations.value.isEmpty(), "New chat tapped twice must not leave empty rows")
    }

    @Test
    fun `new conversation on an already-empty thread is a no-op`() {
        val repo = repo()
        val before = repo.activeId.value
        assertEquals(before, repo.newConversation(), "should reuse the empty conversation, not mint another")
    }

    @Test
    fun `the title comes from the first user turn and never drifts`() {
        val repo = repo()
        repo.append("user", "what colours flatter a warm skin tone?")
        repo.append("assistant", "Warm tones…")
        repo.append("user", "now tell me about winter coats")

        assertEquals("what colours flatter a warm skin tone?", repo.conversations.value.first().title)
    }

    @Test
    fun `deleting the active conversation leaves the app on a fresh empty one`() {
        val repo = repo()
        repo.append("user", "doomed")
        val id = repo.activeId.value

        repo.deleteConversation(id)

        assertTrue(repo.messages.value.isEmpty())
        assertTrue(repo.conversations.value.none { it.id == id })
        // Not silently switched into someone else's thread.
        assertTrue(repo.conversations.value.isEmpty())
    }

    @Test
    fun `deleting a background conversation leaves the active one alone`() {
        val repo = repo()
        repo.append("user", "keep me")
        val keep = repo.activeId.value
        repo.newConversation()
        repo.append("user", "delete me")
        val doomed = repo.activeId.value

        repo.deleteConversation(keep)

        assertEquals(doomed, repo.activeId.value)
        assertEquals(listOf("delete me"), repo.messages.value.map { it.text })
    }

    @Test
    fun `truncateFrom drops the edited turn and everything after it`() {
        val repo = repo()
        repo.append("user", "original question")
        repo.append("assistant", "answer to the original")
        repo.append("user", "follow up")
        val target = repo.messages.value.first().id

        repo.truncateFrom(target)

        assertTrue(repo.messages.value.isEmpty(), "an edit at the root clears the whole thread")
    }

    @Test
    fun `truncateFrom on an unknown id changes nothing`() {
        val repo = repo()
        repo.append("user", "intact")
        repo.truncateFrom("no-such-id")
        assertEquals(1, repo.messages.value.size)
    }

    @Test
    fun `retagMessage rewrites only the provider`() {
        val repo = repo()
        val id = repo.appendPlaceholder("assistant", "selected-provider")
        repo.updateMessage(id, "streamed text", persist = true)

        repo.retagMessage(id, "provider-that-actually-answered")

        val message = repo.messages.value.single()
        assertEquals("streamed text", message.text)
        assertEquals("provider-that-actually-answered", message.providerId)
    }

    // ── Migration ────────────────────────────────────────────────────────────────────────

    @Test
    fun `a pre-conversations history is migrated into one conversation`() {
        val settings = MemorySettings()
        // Exactly the shape the old single-key store wrote.
        settings.putString(
            ChatRepository.LEGACY_KEY,
            """[{"id":"1-user","role":"user","text":"old question","timestampMs":1000},""" +
                """{"id":"2-assistant","role":"assistant","text":"old answer","timestampMs":2000}]""",
        )

        val repo = ChatRepository(settings)

        assertEquals(1, repo.conversations.value.size)
        assertEquals("old question", repo.conversations.value.first().title)
        assertEquals(listOf("old question", "old answer"), repo.messages.value.map { it.text })
    }

    @Test
    fun `migration leaves the legacy key in place so a rollback still finds it`() {
        val settings = MemorySettings()
        settings.putString(
            ChatRepository.LEGACY_KEY,
            """[{"id":"1-user","role":"user","text":"keep me readable","timestampMs":1000}]""",
        )

        ChatRepository(settings)

        assertNotNull(
            settings.getStringOrNull(ChatRepository.LEGACY_KEY),
            "a downgrade must not cost the user their history",
        )
    }

    @Test
    fun `a corrupt store falls back to empty rather than throwing`() {
        val settings = MemorySettings()
        settings.putString(ChatRepository.STORE_KEY, "{ this is not json")
        // No legacy key either, so there is genuinely nothing to load.
        assertTrue(ChatRepository(settings).conversations.value.isEmpty())
    }

    @Test
    fun `an empty legacy history does not create a blank conversation`() {
        val settings = MemorySettings()
        settings.putString(ChatRepository.LEGACY_KEY, "[]")
        assertTrue(ChatRepository(settings).conversations.value.isEmpty())
    }

    @Test
    fun `conversations survive a restart`() {
        val settings = MemorySettings()
        ChatRepository(settings).apply {
            append("user", "written before the restart")
            append("assistant", "and its reply")
        }

        val reopened = ChatRepository(settings)

        assertEquals(1, reopened.conversations.value.size)
        assertEquals(
            listOf("written before the restart", "and its reply"),
            reopened.messages.value.map { it.text },
        )
    }

    @Test
    fun `clearAllConversations removes both stores`() {
        val settings = MemorySettings()
        val repo = ChatRepository(settings)
        repo.append("user", "everything")

        repo.clearAllConversations()

        assertTrue(repo.conversations.value.isEmpty())
        assertTrue(repo.messages.value.isEmpty())
        assertFalse(settings.hasKey(ChatRepository.STORE_KEY))
    }

    @Test
    fun `summaries carry the newest turn as the preview`() {
        val repo = repo()
        repo.append("user", "question")
        repo.append("assistant", "the latest reply")

        val summary = repo.summaries().single()
        assertEquals("question", summary.title)
        assertEquals("the latest reply", summary.preview)
        assertEquals(2, summary.messageCount)
    }
}
