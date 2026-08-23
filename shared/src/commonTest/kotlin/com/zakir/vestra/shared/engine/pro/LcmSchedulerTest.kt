package com.zakir.vestra.shared.engine.pro

import kotlin.test.Test
import kotlin.test.assertEquals

class LcmSchedulerTest {

    @Test
    fun timestepsDescendFrom999() {
        val steps = LcmScheduler().timesteps(4)
        assertEquals(4, steps.size)
        assertEquals(999, steps[0])
        assertEquals(0, steps[3])
    }

    @Test
    fun stepUpdatesSampleInPlace() {
        val scheduler = LcmScheduler()
        val sample = floatArrayOf(1f, 0f)
        val noise = floatArrayOf(0.5f, -0.5f)
        scheduler.step(sample, noise, 999)
        assertEquals(true, sample[0] != 1f || sample[1] != 0f)
    }
}
