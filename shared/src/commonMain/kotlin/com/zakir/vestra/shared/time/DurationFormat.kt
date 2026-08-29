package com.zakir.vestra.shared.time

/**
 * Human-readable duration formatting — "45s" under a minute, "1m" on an exact minute boundary,
 * "1m 30s" otherwise. Pure integer math with no platform API (unlike [formatHms]), so it lives
 * directly in commonMain with no expect/actual split.
 */
fun formatDurationSeconds(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0L)
    val minutes = s / 60
    val seconds = s % 60
    return when {
        minutes <= 0L -> "${seconds}s"
        seconds == 0L -> "${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}

/**
 * Millisecond-precision variant — sub-second durations render as "230ms" (not "0s", which would
 * lose the precision callers like Diagnostics rely on), otherwise delegates to
 * [formatDurationSeconds].
 */
fun formatDurationMs(totalMs: Long): String {
    val ms = totalMs.coerceAtLeast(0L)
    return if (ms < 1000L) "${ms}ms" else formatDurationSeconds(ms / 1000L)
}
