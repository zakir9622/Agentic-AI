package com.zakir.vestra.shared.chat

import com.zakir.vestra.shared.testutil.TestMemorySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryRepositoryTest {

    private fun repo() = MemoryRepository(TestMemorySettings())

    @Test
    fun addFacts_emptyList_isNoOp() {
        val r = repo()
        r.addFacts(emptyList())
        assertTrue(r.facts.value.isEmpty())
    }

    @Test
    fun addFacts_blankStrings_areFilteredOut() {
        val r = repo()
        r.addFacts(listOf("  ", "", "\n"))
        assertTrue(r.facts.value.isEmpty())
    }

    @Test
    fun addFacts_storesNewFacts() {
        val r = repo()
        r.addFacts(listOf("Prefers dark mode", "Works with Kotlin"))
        assertEquals(2, r.facts.value.size)
        assertEquals("Prefers dark mode", r.facts.value[0].text)
    }

    @Test
    fun addFacts_dedupesCaseInsensitively() {
        val r = repo()
        r.addFacts(listOf("Prefers dark mode"))
        r.addFacts(listOf("prefers DARK mode", "Uses Android Studio"))
        assertEquals(2, r.facts.value.size)
    }

    @Test
    fun addFacts_capsAtMaxNewPerCall() {
        val r = repo()
        val many = (1..10).map { "Fact number $it" }
        r.addFacts(many)
        assertEquals(MemoryRepository.MAX_NEW_PER_CALL, r.facts.value.size)
    }

    @Test
    fun addFacts_capsAtMaxTotal_dropsOldestFirst() {
        val settings = TestMemorySettings()
        val r = MemoryRepository(settings)
        // Fill beyond the total cap across multiple calls (each call capped at MAX_NEW_PER_CALL).
        var i = 0
        while (r.facts.value.size < MemoryRepository.MAX_TOTAL) {
            r.addFacts((0 until MemoryRepository.MAX_NEW_PER_CALL).map { "Fact ${i++}" })
        }
        assertEquals(MemoryRepository.MAX_TOTAL, r.facts.value.size)
        r.addFacts(listOf("A brand new fact not seen before"))
        assertEquals(MemoryRepository.MAX_TOTAL, r.facts.value.size)
        assertTrue(r.facts.value.any { it.text == "A brand new fact not seen before" })
        assertTrue(r.facts.value.none { it.text == "Fact 0" })
    }

    @Test
    fun remove_deletesById() {
        val r = repo()
        r.addFacts(listOf("Keep me", "Remove me"))
        val toRemove = r.facts.value.first { it.text == "Remove me" }.id
        r.remove(toRemove)
        assertEquals(1, r.facts.value.size)
        assertEquals("Keep me", r.facts.value[0].text)
    }

    @Test
    fun edit_updatesText() {
        val r = repo()
        r.addFacts(listOf("Original text"))
        val id = r.facts.value[0].id
        r.edit(id, "Updated text")
        assertEquals("Updated text", r.facts.value[0].text)
    }

    @Test
    fun edit_blankText_removesFact() {
        val r = repo()
        r.addFacts(listOf("Will be removed"))
        val id = r.facts.value[0].id
        r.edit(id, "   ")
        assertTrue(r.facts.value.isEmpty())
    }

    @Test
    fun clear_removesEverything() {
        val r = repo()
        r.addFacts(listOf("A", "B"))
        r.clear()
        assertTrue(r.facts.value.isEmpty())
    }

    @Test
    fun contextForSystemPrompt_emptyWhenNoFacts() {
        val r = repo()
        assertEquals("", r.contextForSystemPrompt())
    }

    @Test
    fun contextForSystemPrompt_formatsFactsAsBulletList() {
        val r = repo()
        r.addFacts(listOf("Prefers dark mode", "Works with Kotlin"))
        val context = r.contextForSystemPrompt()
        assertTrue(context.contains("- Prefers dark mode"))
        assertTrue(context.contains("- Works with Kotlin"))
    }

    @Test
    fun persistsAcrossInstances() {
        val settings = TestMemorySettings()
        MemoryRepository(settings).addFacts(listOf("Persisted fact"))
        val reloaded = MemoryRepository(settings)
        assertEquals(1, reloaded.facts.value.size)
        assertEquals("Persisted fact", reloaded.facts.value[0].text)
    }
}
