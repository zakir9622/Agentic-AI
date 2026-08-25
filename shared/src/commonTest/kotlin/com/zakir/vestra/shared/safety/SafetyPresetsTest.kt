package com.zakir.vestra.shared.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyPresetsTest {

    @Test
    fun offPresetHasNoGuardClause() {
        assertEquals("", SafetyPresets.OFF.promptGuard)
        assertEquals(false, SafetyPresets.OFF.confirm)
    }

    @Test
    fun standardIsTheDefaultPreset() {
        assertEquals(SafetyPresets.DEFAULT_ID, SafetyPresets.STANDARD.id)
        assertEquals(SafetyPresets.STANDARD, SafetyPresets.byId(SafetyPresets.DEFAULT_ID))
    }

    @Test
    fun byIdFallsBackToStandardForUnknownId() {
        assertEquals(SafetyPresets.STANDARD, SafetyPresets.byId("not-a-real-preset"))
    }

    @Test
    fun blurIdentitiesAndRedactBothRequireConfirmation() {
        assertTrue(SafetyPresets.BLUR_IDENTITIES.confirm)
        assertTrue(SafetyPresets.REDACT_DETAILS.confirm)
    }

    @Test
    fun applyGuardWithOffPresetReturnsPromptUnchanged() {
        assertEquals("a modest abaya", SafetyPresets.applyGuard("a modest abaya", "off"))
    }

    @Test
    fun applyGuardAppendsGuardClauseAfterPrompt() {
        val result = SafetyPresets.applyGuard("a modest abaya", "standard")
        assertTrue(result.startsWith("a modest abaya"))
        assertTrue(result.contains(SafetyPresets.STANDARD.promptGuard))
    }

    @Test
    fun applyGuardWithBlankPromptReturnsJustTheGuard() {
        assertEquals(SafetyPresets.STANDARD.promptGuard, SafetyPresets.applyGuard("", "standard"))
    }

    @Test
    fun applyGuardWithUnknownPresetIdFallsBackToStandardGuard() {
        val result = SafetyPresets.applyGuard("test", "bogus")
        assertTrue(result.contains(SafetyPresets.STANDARD.promptGuard))
    }

    @Test
    fun allPresetsHaveUniqueIds() {
        val ids = SafetyPresets.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
