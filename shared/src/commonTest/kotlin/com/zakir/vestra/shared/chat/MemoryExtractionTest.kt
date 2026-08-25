package com.zakir.vestra.shared.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryExtractionTest {

    @Test
    fun buildPrompt_includesBothTurns() {
        val prompt = MemoryExtraction.buildPrompt("I prefer dark mode", "Noted, I'll remember that")
        assertTrue(prompt.contains("USER: I prefer dark mode"))
        assertTrue(prompt.contains("ASSISTANT: Noted, I'll remember that"))
    }

    @Test
    fun parseFacts_cleanJsonArray() {
        val facts = MemoryExtraction.parseFacts("""["Prefers dark mode", "Works with Kotlin"]""")
        assertEquals(listOf("Prefers dark mode", "Works with Kotlin"), facts)
    }

    @Test
    fun parseFacts_emptyArray_returnsEmptyList() {
        assertTrue(MemoryExtraction.parseFacts("[]").isEmpty())
    }

    @Test
    fun parseFacts_toleratesSurroundingProse() {
        val output = "Here are the facts I found:\n```json\n[\"Uses Android Studio\"]\n```\nDone."
        assertEquals(listOf("Uses Android Studio"), MemoryExtraction.parseFacts(output))
    }

    @Test
    fun parseFacts_noBracketsAtAll_returnsEmptyList() {
        assertTrue(MemoryExtraction.parseFacts("I don't see any durable facts here.").isEmpty())
    }

    @Test
    fun parseFacts_malformedJson_returnsEmptyListNotGuess() {
        assertTrue(MemoryExtraction.parseFacts("[\"unterminated string, oops").isEmpty())
    }

    @Test
    fun parseFacts_nonStringArrayElements_returnsEmptyList() {
        // A model returning [1, 2, 3] or objects instead of strings must not be coerced into
        // fabricated fact text.
        assertTrue(MemoryExtraction.parseFacts("[1, 2, 3]").isEmpty())
    }

    @Test
    fun parseFacts_capsAtMaxNewPerCall() {
        val many = (1..10).joinToString(",", "[", "]") { "\"Fact $it\"" }
        val facts = MemoryExtraction.parseFacts(many)
        assertEquals(MemoryRepository.MAX_NEW_PER_CALL, facts.size)
    }

    @Test
    fun parseFacts_blankStringsFilteredOut() {
        val facts = MemoryExtraction.parseFacts("""["", "  ", "Real fact"]""")
        assertEquals(listOf("Real fact"), facts)
    }
}
