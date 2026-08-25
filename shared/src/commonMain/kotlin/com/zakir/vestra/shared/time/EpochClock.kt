package com.zakir.vestra.shared.time

/**
 * Platform wall-clock epoch millis. `java.lang.System.currentTimeMillis()` doesn't exist on
 * Kotlin/Native, so this stays `expect`/`actual` — mirrors the existing
 * `createQualityPostProcessor` pattern — rather than calling `java.lang.System` directly from
 * commonMain, which would block an iOS target from compiling this file unchanged.
 */
expect fun wallClockMs(): Long

/**
 * Epoch-millis clock abstraction for commonMain (generation-stability finding M).
 * Default uses wall clock; tests may replace [System].
 */
fun interface EpochClock {
    fun nowMs(): Long

    companion object {
        var System: EpochClock = EpochClock { wallClockMs() }
    }
}
