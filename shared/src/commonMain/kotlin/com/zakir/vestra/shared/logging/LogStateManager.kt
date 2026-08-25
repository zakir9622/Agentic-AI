package com.zakir.vestra.shared.logging

import com.zakir.vestra.shared.time.EpochClock
import com.zakir.vestra.shared.time.formatHms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Log source identifier indicating origin engine/provider. */
enum class LogSource(val label: String) {
    LITERT("LiteRT"),
    CLOUD_API("Cloud API"),
    SYSTEM("System"),
}

/** Log severity level. */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/** Individual real-time log entry captured across LiteRT local inference and cloud AI calls. */
data class LogEntry(
    val timestampMs: Long = EpochClock.System.nowMs(),
    val source: LogSource,
    val level: LogLevel = LogLevel.INFO,
    val tag: String,
    val message: String,
) {
    fun formatDisplay(): String {
        return "[${formatHms(timestampMs)}] [${source.label}] $message"
    }
}

/**
 * Collects a real-time, in-memory, capped event stream for a single generation flow (e.g. one
 * chat session) — which engine ran, cloud fallback transitions, and errors, timestamped and
 * leveled. Distinct from [com.zakir.vestra.shared.diagnostics.RunDiagnostics]: that's the
 * persistent, correlation-ID-first record of *runs* used for support/debugging across app
 * launches; this is a lightweight, transient "what's happening right now" console feed meant for
 * a live UI view (see [com.zakir.vestra.ui.components.LiveGenConsole]), cleared per session.
 */
class LogStateManager(
    private val maxCapacity: Int = 100,
) {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val _formattedLines = MutableStateFlow<List<String>>(emptyList())
    val formattedLines: StateFlow<List<String>> = _formattedLines.asStateFlow()

    fun log(
        source: LogSource,
        message: String,
        level: LogLevel = LogLevel.INFO,
        tag: String = "Gen",
    ) {
        val entry = LogEntry(
            source = source,
            level = level,
            tag = tag,
            message = message.trim().take(300),
        )
        val updated = (_entries.value + entry).takeLast(maxCapacity)
        _entries.value = updated
        _formattedLines.value = updated.map { it.formatDisplay() }
    }

    fun info(source: LogSource, message: String, tag: String = "Gen") {
        log(source, message, LogLevel.INFO, tag)
    }

    fun warn(source: LogSource, message: String, tag: String = "Gen") {
        log(source, message, LogLevel.WARN, tag)
    }

    fun error(source: LogSource, message: String, tag: String = "Gen") {
        log(source, message, LogLevel.ERROR, tag)
    }

    fun debug(source: LogSource, message: String, tag: String = "Gen") {
        log(source, message, LogLevel.DEBUG, tag)
    }

    fun clear() {
        _entries.value = emptyList()
        _formattedLines.value = emptyList()
    }
}
