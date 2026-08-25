package com.zakir.vestra.shared.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceCatalogGroupingTest {

    @Test
    fun everyPersonaAppearsInExactlyOneGroup() {
        val grouped = VoiceCatalog.groupedByVariety()
        val allGrouped = grouped.flatMap { it.second }
        assertEquals(VoiceCatalog.personas.size, allGrouped.size)
        assertEquals(VoiceCatalog.personas.toSet(), allGrouped.toSet())
    }

    @Test
    fun noGroupIsEmpty() {
        VoiceCatalog.groupedByVariety().forEach { (section, personas) ->
            assertTrue(personas.isNotEmpty(), "section '$section' has no personas")
        }
    }

    @Test
    fun sectionsAppearInDisplayOrder() {
        val sections = VoiceCatalog.groupedByVariety().map { it.first }
        val expectedOrder = listOf("Female", "Male", "Neutral & character")
        assertEquals(sections, expectedOrder.filter { it in sections })
    }

    @Test
    fun everyVarietyMapsToOneOfTheThreeSections() {
        VoiceVariety.entries.forEach { variety ->
            assertTrue(
                VoiceCatalog.sectionFor(variety) in setOf("Female", "Male", "Neutral & character"),
                "unmapped variety: $variety",
            )
        }
    }

    @Test
    fun everyVarietyHasAPositiveTypicalHz() {
        VoiceVariety.entries.forEach { variety ->
            assertTrue(VoiceCatalog.typicalHzFor(variety) > 0f, "non-positive Hz for $variety")
        }
    }
}
