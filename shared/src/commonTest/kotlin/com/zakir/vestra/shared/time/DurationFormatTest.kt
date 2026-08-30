package com.zakir.vestra.shared.time

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatTest {

    @Test
    fun formatDurationSecondsUnderAMinute() {
        assertEquals("0s", formatDurationSeconds(0))
        assertEquals("45s", formatDurationSeconds(45))
        assertEquals("59s", formatDurationSeconds(59))
    }

    @Test
    fun formatDurationSecondsExactMinuteBoundary() {
        assertEquals("1m", formatDurationSeconds(60))
        assertEquals("2m", formatDurationSeconds(120))
    }

    @Test
    fun formatDurationSecondsMinutesAndSeconds() {
        assertEquals("1m 30s", formatDurationSeconds(90))
        assertEquals("2m 5s", formatDurationSeconds(125))
    }

    @Test
    fun formatDurationSecondsNeverNegative() {
        assertEquals("0s", formatDurationSeconds(-10))
    }

    @Test
    fun formatDurationMsSubSecondPrecision() {
        assertEquals("0ms", formatDurationMs(0))
        assertEquals("999ms", formatDurationMs(999))
    }

    @Test
    fun formatDurationMsDelegatesAtOneSecond() {
        assertEquals("1s", formatDurationMs(1_000))
        assertEquals("1m 1s", formatDurationMs(61_000))
    }
}
