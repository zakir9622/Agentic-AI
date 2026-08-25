package com.zakir.vestra.shared.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogStateManagerTest {

    @Test
    fun logAppendsAFormattedEntry() {
        val manager = LogStateManager()
        manager.info(LogSource.LITERT, "Streaming tokens from local model...")

        assertEquals(1, manager.entries.value.size)
        val line = manager.formattedLines.value.single()
        assertTrue(line.contains("[LiteRT]"), "expected the source label in the formatted line, got: $line")
        assertTrue(line.contains("Streaming tokens"), "expected the message in the formatted line, got: $line")
    }

    @Test
    fun levelsRouteToDistinctLogLevels() {
        val manager = LogStateManager()
        manager.warn(LogSource.SYSTEM, "Preflight blocked")
        manager.error(LogSource.CLOUD_API, "Chat execution error")
        manager.debug(LogSource.LITERT, "Streaming output: 42 chars")

        val levels = manager.entries.value.map { it.level }
        assertEquals(listOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.DEBUG), levels)
    }

    @Test
    fun capacityIsBoundedAndKeepsMostRecentEntries() {
        val manager = LogStateManager(maxCapacity = 3)
        repeat(5) { i -> manager.info(LogSource.SYSTEM, "entry $i") }

        assertEquals(3, manager.entries.value.size)
        assertEquals(listOf("entry 2", "entry 3", "entry 4"), manager.entries.value.map { it.message })
    }

    @Test
    fun clearEmptiesBothStreams() {
        val manager = LogStateManager()
        manager.info(LogSource.SYSTEM, "one")
        manager.clear()

        assertTrue(manager.entries.value.isEmpty())
        assertTrue(manager.formattedLines.value.isEmpty())
    }

    @Test
    fun longMessagesAreTruncatedToThreeHundredChars() {
        val manager = LogStateManager()
        manager.info(LogSource.SYSTEM, "x".repeat(500))

        assertEquals(300, manager.entries.value.single().message.length)
    }
}
