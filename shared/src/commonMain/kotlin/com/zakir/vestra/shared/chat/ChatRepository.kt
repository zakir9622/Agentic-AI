package com.zakir.vestra.shared.chat

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.zakir.vestra.shared.time.EpochClock

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val timestampMs: Long,
    val providerId: String? = null,
)

/** One saved conversation. [title] is derived from the first user turn, never asked for. */
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val messages: List<ChatMessage> = emptyList(),
)

/** A conversation without its messages — what the history list needs to render a row. */
data class ConversationSummary(
    val id: String,
    val title: String,
    val updatedAtMs: Long,
    val messageCount: Int,
    val preview: String,
)

/**
 * On-device conversation store.
 *
 * This used to be a single list of messages under one key, and "New chat" called [clear], which
 * did `settings.remove(KEY)`. That made the app's most prominent button an unconfirmed, permanent
 * delete of the only conversation that existed — and turn 81 silently evicted turn 1 on top of
 * it. Both are fixed by the same change: conversations are records, and starting a new one files
 * the old one rather than destroying it.
 *
 * The active conversation's messages stay exposed as [messages] with exactly the API the view
 * model already used ([append], [appendPlaceholder], [updateMessage], [removeMessage],
 * [contextForLlm]), so switching conversations is the only new concept anything upstream has to
 * learn.
 *
 * **Nothing leaves the device.** This is [Settings]-backed like the old store, and the migration
 * below preserves the existing history rather than starting anyone from zero.
 */
class ChatRepository(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var idCounter = 0

    private val _conversations = MutableStateFlow(loadConversations())
    private val _activeId = MutableStateFlow(_conversations.value.firstOrNull()?.id ?: newId())

    /** Every conversation, newest activity first. */
    val conversations: StateFlow<List<Conversation>> = _conversations

    /** Which conversation the composer is writing into. */
    val activeId: StateFlow<String> = _activeId

    private val _messages = MutableStateFlow(activeConversation()?.messages.orEmpty())
    val messages: StateFlow<List<ChatMessage>> = _messages

    /** Rows for the history list. Cheap to recompute; the store is bounded to [MAX_CONVERSATIONS]. */
    fun summaries(): List<ConversationSummary> = _conversations.value.map { conversation ->
        ConversationSummary(
            id = conversation.id,
            title = conversation.title,
            updatedAtMs = conversation.updatedAtMs,
            messageCount = conversation.messages.size,
            preview = conversation.messages.lastOrNull()?.text?.take(PREVIEW_CHARS).orEmpty(),
        )
    }

    fun append(role: String, text: String, providerId: String? = null) {
        val msg = ChatMessage(
            id = "${EpochClock.System.nowMs()}-$role",
            role = role,
            text = text.trim(),
            timestampMs = EpochClock.System.nowMs(),
            providerId = providerId,
        )
        _messages.value = (_messages.value + msg).takeLast(MAX_MESSAGES)
        persistActive()
    }

    /**
     * Appends an empty message and returns its id, for a caller that will fill it in live as a
     * response streams — [updateMessage] moves the text forward on every chunk without writing
     * to disk each time; only the final [updateMessage] with `persist = true` does.
     */
    fun appendPlaceholder(role: String, providerId: String? = null): String {
        val msg = ChatMessage(
            id = "${EpochClock.System.nowMs()}-$role-${_messages.value.size}",
            role = role,
            text = "",
            timestampMs = EpochClock.System.nowMs(),
            providerId = providerId,
        )
        _messages.value = (_messages.value + msg).takeLast(MAX_MESSAGES)
        return msg.id
    }

    /** Updates an existing message's text in place (e.g. a streaming assistant reply). */
    fun updateMessage(id: String, text: String, persist: Boolean = false) {
        _messages.value = _messages.value.map { if (it.id == id) it.copy(text = text.trim()) else it }
        if (persist) persistActive()
    }

    /**
     * Re-attaches a message to the provider that actually served it.
     *
     * A streamed reply's placeholder is created before the fallback chain has settled, so it
     * carries the *selected* provider; by the end, a different candidate may have answered. Without
     * this the bubble would name the wrong model and the diagnostics record would disagree with
     * the screen.
     */
    fun retagMessage(id: String, providerId: String) {
        _messages.value = _messages.value.map { if (it.id == id) it.copy(providerId = providerId) else it }
        persistActive()
    }

    /** Replaces a user turn's text — the composer's "edit and re-send". */
    fun editMessage(id: String, text: String) {
        _messages.value = _messages.value.map { if (it.id == id) it.copy(text = text.trim()) else it }
        persistActive()
    }

    /** Removes a message by id — e.g. an empty streaming placeholder that never got a result. */
    fun removeMessage(id: String) {
        _messages.value = _messages.value.filterNot { it.id == id }
    }

    /**
     * Removes one message and persists the result.
     *
     * Distinct from [removeMessage], which drops an in-flight streaming placeholder and must
     * *not* write — that one runs on a failure path where the store still holds the last good
     * state.
     */
    fun deleteMessage(id: String) {
        _messages.value = _messages.value.filterNot { it.id == id }
        persistActive()
    }

    /** Drops [id] and every turn after it — what "edit this prompt and re-run" needs. */
    fun truncateFrom(id: String) {
        val index = _messages.value.indexOfFirst { it.id == id }
        if (index < 0) return
        _messages.value = _messages.value.take(index)
        persistActive()
    }

    fun contextForLlm(maxTurns: Int = 12): List<Pair<String, String>> =
        _messages.value.takeLast(maxTurns).map { it.role to it.text }

    /**
     * Starts a fresh conversation, keeping the current one.
     *
     * An empty active conversation is reused rather than filed: tapping New chat twice should not
     * leave an untitled empty row in the history list.
     */
    fun newConversation(): String {
        if (_messages.value.isEmpty()) return _activeId.value
        persistActive()
        val id = newId()
        _activeId.value = id
        _messages.value = emptyList()
        return id
    }

    /** Switches to a saved conversation. A no-op for an id that is not in the store. */
    fun openConversation(id: String) {
        if (id == _activeId.value) return
        val target = _conversations.value.firstOrNull { it.id == id } ?: return
        persistActive()
        _activeId.value = target.id
        _messages.value = target.messages
    }

    /**
     * Deletes one conversation. Deleting the active one leaves the app on a fresh empty
     * conversation rather than silently switching to someone else's thread.
     */
    fun deleteConversation(id: String) {
        _conversations.value = _conversations.value.filterNot { it.id == id }
        writeStore()
        if (id == _activeId.value) {
            _activeId.value = newId()
            _messages.value = emptyList()
        }
    }

    /** Empties the active conversation without touching the rest of the history. */
    fun clear() {
        _messages.value = emptyList()
        _conversations.value = _conversations.value.filterNot { it.id == _activeId.value }
        writeStore()
    }

    /** Wipes every conversation. Only reachable behind an explicit confirm in Storage & privacy. */
    fun clearAllConversations() {
        _conversations.value = emptyList()
        _activeId.value = newId()
        _messages.value = emptyList()
        settings.remove(STORE_KEY)
        settings.remove(LEGACY_KEY)
    }

    /** Folds the in-memory active messages back into the store and writes it. */
    private fun persistActive() {
        val active = _messages.value
        val id = _activeId.value
        val now = EpochClock.System.nowMs()
        val existing = _conversations.value.firstOrNull { it.id == id }
        val updated = if (active.isEmpty()) {
            // An empty conversation is not a record. This is what keeps a New-chat tap that the
            // user immediately backs out of from littering the history list.
            _conversations.value.filterNot { it.id == id }
        } else {
            val conversation = Conversation(
                id = id,
                title = titleFor(active, existing?.title),
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
                messages = active,
            )
            (listOf(conversation) + _conversations.value.filterNot { it.id == id })
                .sortedByDescending { it.updatedAtMs }
                .take(MAX_CONVERSATIONS)
        }
        _conversations.value = updated
        writeStore()
    }

    private fun writeStore() {
        settings.putString(STORE_KEY, json.encodeToString(_conversations.value))
    }

    private fun activeConversation(): Conversation? =
        _conversations.value.firstOrNull { it.id == _activeId.value }

    /**
     * Loads the store, migrating a pre-conversations history if one is present.
     *
     * The migration runs once and leaves [LEGACY_KEY] in place: if a build carrying this change
     * is rolled back, the old single-thread store is still exactly where the old code looks for
     * it, so a downgrade costs the user nothing.
     */
    private fun loadConversations(): List<Conversation> {
        settings.getStringOrNull(STORE_KEY)?.let { raw ->
            runCatching { json.decodeFromString<List<Conversation>>(raw) }.getOrNull()
                ?.let { return it.sortedByDescending { c -> c.updatedAtMs } }
        }
        val legacy = settings.getStringOrNull(LEGACY_KEY)?.let { raw ->
            runCatching { json.decodeFromString<List<ChatMessage>>(raw) }.getOrNull()
        }.orEmpty()
        if (legacy.isEmpty()) return emptyList()
        val migrated = Conversation(
            id = newId(),
            title = titleFor(legacy, null),
            createdAtMs = legacy.first().timestampMs,
            updatedAtMs = legacy.last().timestampMs,
            messages = legacy,
        )
        settings.putString(STORE_KEY, json.encodeToString(listOf(migrated)))
        return listOf(migrated)
    }

    /**
     * A conversation's name, taken from its first user turn.
     *
     * Once set it never changes — a thread the user recognises as "capsule wardrobe" should not
     * rename itself when the conversation wanders, which is what deriving from the *latest* turn
     * would do.
     */
    private fun titleFor(messages: List<ChatMessage>, existing: String?): String {
        if (!existing.isNullOrBlank() && existing != UNTITLED) return existing
        val firstUser = messages.firstOrNull { it.role.equals("user", ignoreCase = true) }?.text
        return firstUser?.trim()?.takeIf { it.isNotEmpty() }
            ?.replace('\n', ' ')
            ?.take(TITLE_CHARS)
            ?: UNTITLED
    }

    /**
     * A conversation id.
     *
     * The counter is not decoration. A bare millisecond timestamp collides whenever two
     * conversations are created inside the same millisecond — which is exactly what "start a new
     * chat, immediately start another" does — and two conversations sharing an id means deleting
     * one deletes the other. A test caught this; a user would have caught it by losing a thread.
     */
    private fun newId(): String = "c${EpochClock.System.nowMs()}-${idCounter++}"

    companion object {
        /** Retained so a rollback still finds the pre-migration history. */
        const val LEGACY_KEY = "chat_history_v1"
        const val STORE_KEY = "chat_conversations_v2"
        const val UNTITLED = "New chat"
        private const val MAX_MESSAGES = 80
        private const val MAX_CONVERSATIONS = 60
        private const val TITLE_CHARS = 60
        private const val PREVIEW_CHARS = 90
    }
}
