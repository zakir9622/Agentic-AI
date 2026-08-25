package com.zakir.vestra.shared.diagnostics

/**
 * Optional bridge so `shared`-module engine code (which cannot see `composeApp`'s
 * Android-only [CrashReporter][com.zakir.vestra.diagnostics.CrashReporter] — the module
 * dependency runs the other way) can still route its own logging and non-fatal-exception
 * recording through it. Set once from `VestraApp.onCreate()`, mirroring [DiagnosticsHook]'s
 * existing settable-callback pattern.
 *
 * Every call is a no-op until wired — engines must not assume a listener is attached (unit
 * tests, and any future non-Android target, run with everything null here).
 */
object EngineLogHook {
    var logI: ((tag: String, message: String) -> Unit)? = null
    var logW: ((tag: String, message: String) -> Unit)? = null
    var logE: ((tag: String, message: String, throwable: Throwable?) -> Unit)? = null
    var recordNonFatal: ((tag: String, throwable: Throwable, detail: String) -> Unit)? = null

    // `GenerativeViewModel`'s StudioBag design deliberately lets a local generation keep running
    // in the background when the user switches studio tabs ("bindStudio... does not clear
    // sibling tabs") — so more than one local generation's run id can genuinely be active at
    // once. A single mutable var here would misattribute a failure in one tab's generation to
    // whichever id happened to be set last, which is worse than no ref at all. Tracking the set
    // of active ids instead means [nonFatal] only appends a ref when it can be unambiguous
    // (exactly one active) — degrading to "no ref" rather than a wrong one otherwise.
    private val activeRunIds = mutableSetOf<String>()
    private val lock = Any()

    fun addActiveRunId(id: String) {
        synchronized(lock) { activeRunIds.add(id) }
    }

    fun removeActiveRunId(id: String) {
        synchronized(lock) { activeRunIds.remove(id) }
    }

    private fun unambiguousRunId(): String? =
        synchronized(lock) { activeRunIds.singleOrNull() }

    fun i(tag: String, message: String) {
        logI?.invoke(tag, message)
    }

    fun w(tag: String, message: String) {
        logW?.invoke(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logE?.invoke(tag, message, throwable)
    }

    fun nonFatal(tag: String, throwable: Throwable, detail: String = "") {
        val withRef = unambiguousRunId()?.let { id ->
            if (detail.isBlank()) "ref=$id" else "$detail (ref=$id)"
        } ?: detail
        recordNonFatal?.invoke(tag, throwable, withRef)
    }
}
