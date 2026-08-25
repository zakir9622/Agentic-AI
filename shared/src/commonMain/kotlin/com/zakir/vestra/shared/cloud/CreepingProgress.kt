package com.zakir.vestra.shared.cloud

/**
 * Progress-fraction math for generation paths with no real per-step signal from the underlying
 * work — a poll-count-bounded remote job (HF Space video/audio polling), the case this ports
 * from lookbookweb's `src/lib/jobs.ts` "ease a progress estimate toward a ceiling" idea. Local
 * on-device generators with a real per-step signal (image diffusion steps, code token
 * streaming) don't use this — they already emit a genuine fraction from the underlying work.
 */
object CreepingProgress {
    /**
     * Fraction for poll [pollIndex] (0-based) out of [maxPolls] expected polls, eased from
     * [floor] toward [floor] + [span] as polling proceeds — reaching the ceiling only once the
     * expected poll count is used up. The real completion state (or a genuine failure) takes
     * over as soon as the underlying call actually returns, whichever poll that happens on.
     */
    fun forPoll(pollIndex: Int, maxPolls: Int, floor: Float, span: Float): Float {
        val ratio = (pollIndex + 1).toFloat() / maxPolls.coerceAtLeast(1)
        return (floor + span * ratio).coerceIn(floor, floor + span)
    }
}
