package com.zakir.vestra.shared.chat

import kotlinx.serialization.json.Json

/**
 * Builds the extraction prompt run through the local chat model after a conversation turn, and
 * parses its (hopefully JSON) output back into a list of fact strings. Pure/testable — no model
 * call happens here, [com.zakir.vestra.ui.ChatViewModel] owns dispatching the prompt and
 * handing the raw output to [parseFacts].
 */
object MemoryExtraction {
    private const val INSTRUCTION =
        "Extract up to 5 durable facts about the user from this conversation turn — stated " +
            "preferences, projects, tools, constraints, names, or recurring goals. Ignore " +
            "one-off requests and small talk. Reply with ONLY a JSON array of short strings, " +
            "nothing else. If there are no durable facts, reply with []."

    fun buildPrompt(userText: String, assistantText: String): String =
        "$INSTRUCTION\n\nUSER: $userText\nASSISTANT: $assistantText"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Best-effort parse of a JSON array of strings out of [modelOutput] — tolerant of a model
     * wrapping the array in prose or a code fence (common local-model behavior) by locating the
     * outermost `[`...`]` span, but never guesses facts when no such span parses cleanly: any
     * failure returns an empty list rather than fabricating something from unstructured text.
     */
    fun parseFacts(modelOutput: String): List<String> {
        val start = modelOutput.indexOf('[')
        val end = modelOutput.lastIndexOf(']')
        if (start == -1 || end == -1 || end < start) return emptyList()
        val slice = modelOutput.substring(start, end + 1)
        return runCatching { json.decodeFromString<List<String>>(slice) }
            .getOrNull()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(MemoryRepository.MAX_NEW_PER_CALL)
            .orEmpty()
    }
}
