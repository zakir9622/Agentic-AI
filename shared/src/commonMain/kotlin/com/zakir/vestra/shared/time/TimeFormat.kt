package com.zakir.vestra.shared.time

/**
 * Formats an epoch-millis timestamp as a local "HH:mm:ss" clock string for display (e.g. the
 * live generation console). `expect`/`actual` for the same reason as [wallClockMs]: the JVM
 * implementation (`java.text.SimpleDateFormat`) doesn't exist on Kotlin/Native, so the formatting
 * itself — not just the epoch-millis source — needs to stay out of commonMain directly.
 */
expect fun formatHms(epochMs: Long): String
