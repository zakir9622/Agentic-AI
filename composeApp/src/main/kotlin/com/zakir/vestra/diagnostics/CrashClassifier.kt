package com.zakir.vestra.diagnostics

/**
 * Maps stack traces to actionable Lookbook causes for auto-troubleshooting.
 * Pure Kotlin — unit-tested without Android.
 */
object CrashClassifier {
    fun classify(throwable: Throwable, stack: String = throwable.stackTraceToString()): String {
        val msg = (throwable.message ?: "").lowercase()
        val name = throwable.javaClass.name
        val s = stack.lowercase()
        return when {
            throwable is OutOfMemoryError || name.contains("OutOfMemory") || msg.contains("out of memory") ->
                "OutOfMemory — Pro pack / large bitmap; free RAM or use Fast try-on"
            s.contains("onnxruntime") || s.contains("ortsession") || s.contains("ortmodel") ||
                s.contains("ortgraph") ->
                "ONNX Runtime — pack corrupt or in-use; re-download pack / wait for generation to finish"
            s.contains("cancellationexception") || msg.contains("standaloneCoroutine was cancelled") ->
                "Job cancelled — usually back-press during generation (not a hard crash)"
            s.contains("native method") && (s.contains("libc") || msg.contains("signal")) ->
                "Native crash — often ORT/NNAPI; try again or reinstall lite/pro pack"
            name.contains("NullPointerException") ->
                "NullPointerException — missing state; share troubleshooting bundle from Diagnostics"
            name.contains("IllegalStateException") && s.contains("compose") ->
                "Compose IllegalState — UI race; include app_trace + last screen breadcrumb"
            s.contains("sqlite") || s.contains("disk") || msg.contains("enospc") || msg.contains("no space") ->
                "Storage — free space then retry pack download"
            s.contains("unknownhost") || s.contains("sslhandshake") ||
                (s.contains("network") && s.contains("exception")) ->
                "Network — offline or TLS; local packs should still work"
            else -> "Unhandled ${throwable.javaClass.simpleName} — see crash_log.txt stack"
        }
    }
}
