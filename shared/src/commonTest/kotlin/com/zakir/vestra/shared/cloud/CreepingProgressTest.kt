package com.zakir.vestra.shared.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreepingProgressTest {

    @Test
    fun forPoll_firstPollIsAboveFloor() {
        val f = CreepingProgress.forPoll(pollIndex = 0, maxPolls = 10, floor = 0.2f, span = 0.65f)
        assertTrue(f > 0.2f)
    }

    @Test
    fun forPoll_lastPollReachesButNeverExceedsCeiling() {
        val f = CreepingProgress.forPoll(pollIndex = 9, maxPolls = 10, floor = 0.2f, span = 0.65f)
        assertEquals(0.2f + 0.65f, f, 0.0001f)
    }

    @Test
    fun forPoll_beyondLastPollStillClampsToCeiling() {
        // A poll index past maxPolls (the loop's last iteration can exceed the estimate) must
        // not push the fraction over the ceiling.
        val f = CreepingProgress.forPoll(pollIndex = 15, maxPolls = 10, floor = 0.2f, span = 0.65f)
        assertEquals(0.2f + 0.65f, f, 0.0001f)
    }

    @Test
    fun forPoll_isMonotonicallyIncreasing() {
        var previous = 0f
        for (i in 0 until 10) {
            val current = CreepingProgress.forPoll(pollIndex = i, maxPolls = 10, floor = 0.2f, span = 0.65f)
            assertTrue(current > previous, "poll $i ($current) should exceed previous ($previous)")
            previous = current
        }
    }

    @Test
    fun forPoll_staysWithinFloorAndCeiling() {
        for (i in 0 until 20) {
            val f = CreepingProgress.forPoll(pollIndex = i, maxPolls = 5, floor = 0.35f, span = 0.5f)
            assertTrue(f in 0.35f..0.85f)
        }
    }

    @Test
    fun forPoll_zeroMaxPolls_doesNotDivideByZero() {
        val f = CreepingProgress.forPoll(pollIndex = 0, maxPolls = 0, floor = 0.2f, span = 0.65f)
        assertEquals(0.85f, f, 0.0001f)
    }

    @Test
    fun forPoll_matchesOriginalVideoFormula() {
        // Exact regression check against the inline formula this replaced in
        // GenerativeCloudService.generateVideo — floor 0.2, span 0.65, coerced to [0.2, 0.9].
        for (pollIndex in 0 until 24) {
            val maxPolls = 24
            val expected = (0.2f + 0.65f * (pollIndex + 1).toFloat() / maxPolls).coerceIn(0.2f, 0.9f)
            val actual = CreepingProgress.forPoll(pollIndex, maxPolls, floor = 0.2f, span = 0.65f)
            assertEquals(expected, actual, 0.0001f)
        }
    }

    @Test
    fun forPoll_matchesOriginalAudioFormula() {
        // Exact regression check against the inline formula this replaced in
        // GenerativeCloudService.generateAudio — floor 0.35, span 0.5, coerced to [0.35, 0.88].
        for (pollIndex in 0 until 20) {
            val maxPolls = 20
            val expected = (0.35f + 0.5f * (pollIndex + 1).toFloat() / maxPolls).coerceIn(0.35f, 0.88f)
            val actual = CreepingProgress.forPoll(pollIndex, maxPolls, floor = 0.35f, span = 0.5f)
            assertEquals(expected, actual, 0.0001f)
        }
    }
}
