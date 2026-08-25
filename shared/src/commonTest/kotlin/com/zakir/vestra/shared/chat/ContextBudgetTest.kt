package com.zakir.vestra.shared.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextBudgetTest {

    @Test
    fun windowFor_knownModel_returnsCatalogedValue() {
        assertEquals(32_768, ContextBudget.windowFor("local-qwen3-06b-v1"))
        assertEquals(128_000, ContextBudget.windowFor("llama33-70b-groq"))
    }

    @Test
    fun windowFor_unknownOrNullModel_returnsFallback() {
        assertEquals(ContextBudget.FALLBACK_WINDOW, ContextBudget.windowFor("openrouter-free"))
        assertEquals(ContextBudget.FALLBACK_WINDOW, ContextBudget.windowFor(null))
    }

    @Test
    fun isKnownModel_reflectsCatalogMembership() {
        assertTrue(ContextBudget.isKnownModel("local-gemma-4-e2b-v1"))
        assertFalse(ContextBudget.isKnownModel("openrouter-free"))
        assertFalse(ContextBudget.isKnownModel(null))
    }

    @Test
    fun estimateTokens_emptyText_isZero() {
        assertEquals(0, ContextBudget.estimateTokens(""))
    }

    @Test
    fun estimateTokens_nonEmptyText_neverZero() {
        assertTrue(ContextBudget.estimateTokens("a") >= 1)
    }

    @Test
    fun estimateTokens_usesCharacterHeuristicByDefault() {
        // 16 chars / 4 chars-per-token = exactly 4.
        assertEquals(4, ContextBudget.estimateTokens("0123456789abcdef"))
    }

    @Test
    fun estimateTokens_usesRealTokenizerWhenSupplied() {
        val fakeTokenizer: (String) -> Int = { it.split(" ").size }
        assertEquals(3, ContextBudget.estimateTokens("one two three", tokenizer = fakeTokenizer))
    }

    @Test
    fun evaluate_underBudget_doesNotTruncate() {
        val budget = ContextBudget.evaluate(usedTokens = 100, modelId = "local-qwen3-06b-v1")
        assertFalse(budget.willTruncate)
        assertEquals(32_768, budget.windowTokens)
        assertTrue(budget.remainingTokens > 0)
        assertTrue(budget.isKnownWindow)
    }

    @Test
    fun evaluate_overBudget_flagsTruncation() {
        val budget = ContextBudget.evaluate(usedTokens = 8_000, modelId = "openrouter-free")
        assertTrue(budget.willTruncate)
        assertEquals(ContextBudget.FALLBACK_WINDOW, budget.windowTokens)
        assertTrue(budget.remainingTokens < 0)
        assertFalse(budget.isKnownWindow)
    }

    @Test
    fun evaluate_reservesHeadroomForReply() {
        // Window 8192, used 7700 -> only 492 left, reserve 512 default -> should already flag.
        val budget = ContextBudget.evaluate(usedTokens = 7_700, modelId = null)
        assertTrue(budget.willTruncate)
    }

    @Test
    fun evaluate_zeroReserve_onlyFlagsAtActualOverflow() {
        val budget = ContextBudget.evaluate(usedTokens = 8_192, modelId = null, reserveForReply = 0)
        assertFalse(budget.willTruncate)
        assertEquals(0, budget.remainingTokens)
    }
}
