package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.time.EpochClock

/**
 * Wall-clock budget for a single generation attempt (finding D).
 * Default: 120s image, 300s video.
 */
class GenerationBudget(
    private val deadlineMs: Long,
) {
    fun remainingMs(nowMs: Long = EpochClock.System.nowMs()): Long =
        (deadlineMs - nowMs).coerceAtLeast(0)

    fun expired(nowMs: Long = EpochClock.System.nowMs()): Boolean = nowMs >= deadlineMs

    fun throwIfExpired(nowMs: Long = EpochClock.System.nowMs()) {
        if (expired(nowMs)) throw CloudFailureException(CloudFailure.Timeout)
    }

    /** Gradio poll count derived from remaining budget. */
    fun maxPolls(pollDelayMs: Long = 2_000, floor: Int = 5, ceiling: Int = 60): Int {
        val rem = remainingMs()
        if (rem <= 0) return 0
        return ((rem / pollDelayMs).toInt() - 1).coerceIn(floor, ceiling)
    }

    companion object {
        const val IMAGE_DEADLINE_MS = 120_000L
        const val VIDEO_DEADLINE_MS = 300_000L

        fun forImage(nowMs: Long = EpochClock.System.nowMs()): GenerationBudget =
            GenerationBudget(nowMs + IMAGE_DEADLINE_MS)

        fun forVideo(nowMs: Long = EpochClock.System.nowMs()): GenerationBudget =
            GenerationBudget(nowMs + VIDEO_DEADLINE_MS)
    }
}
