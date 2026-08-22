package com.zakir.vestra.diagnostics

import android.app.Application
import android.os.Build
import android.util.Log
import com.zakir.vestra.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Auto-troubleshooting: catches fatal crashes, appends them to durable files
 * (never auto-clears), and keeps a rotating app-trace log for context.
 *
 * Files under `filesDir/diagnostics/`:
 * - `crash_log.txt` — append-only crash dump history
 * - `last_crash.json` — structured latest crash + likelyCause
 * - `app_trace.log` — continuous breadcrumbs / warnings (rotated at ~1.5 MB)
 */
object CrashReporter {
    private const val TAG = "LookbookCrash"
    private const val DIR = "diagnostics"
    private const val CRASH_LOG = "crash_log.txt"
    private const val LAST_CRASH = "last_crash.json"
    private const val APP_TRACE = "app_trace.log"
    private const val APP_TRACE_BAK = "app_trace.log.1"
    private const val MAX_TRACE_BYTES = 1_500_000L
    private const val MAX_CRASH_LOG_BYTES = 4_000_000L

    private val appRef = AtomicReference<Application?>(null)
    private val breadcrumb = AtomicReference("boot")
    private val lock = Any()

    fun install(app: Application) {
        appRef.set(app)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { recordFatal(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
                ?: run {
                    // No prior handler — terminate after flush.
                    try {
                        android.os.Process.killProcess(android.os.Process.myPid())
                    } catch (_: Throwable) {
                    }
                }
        }
        i("CrashReporter", "installed v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        // Note a prior crash on cold start (do not clear).
        lastCrashJson()?.let { json ->
            val cause = json.optString("likelyCause", "unknown")
            val at = json.optString("isoTime", "?")
            w("CrashReporter", "Previous crash still on disk: $cause @ $at — open Settings → Diagnostics")
        }
    }

    fun breadcrumb(screen: String) {
        breadcrumb.set(screen)
        i("Nav", "screen=$screen")
    }

    fun recordNonFatal(tag: String, throwable: Throwable, detail: String = "") {
        w(tag, "nonfatal ${throwable.javaClass.simpleName}: ${throwable.message} $detail")
        runCatching {
            appendCrashFile(
                header = "NONFATAL",
                threadName = Thread.currentThread().name,
                throwable = throwable,
                detail = detail,
                fatal = false,
            )
        }
    }

    fun i(tag: String, message: String) = appendTrace("I", tag, message).also {
        Log.i(tag, message)
    }

    fun w(tag: String, message: String) = appendTrace("W", tag, message).also {
        Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        appendTrace("E", tag, message + (throwable?.let { " · ${it.javaClass.simpleName}: ${it.message}" } ?: ""))
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }

    fun hasPendingCrash(): Boolean = lastCrashFile()?.isFile == true

    fun lastCrashSummary(): String? {
        val json = lastCrashJson() ?: return null
        val cause = json.optString("likelyCause", "Crash")
        val msg = json.optString("message", "")
        val at = json.optString("isoTime", "")
        val screen = json.optString("breadcrumb", "")
        return buildString {
            append(cause)
            if (at.isNotBlank()) append(" · $at")
            if (screen.isNotBlank()) append(" · screen=$screen")
            if (msg.isNotBlank()) append("\n$msg")
        }
    }

    fun lastCrashLikelyCause(): String? = lastCrashJson()?.optString("likelyCause")?.takeIf { it.isNotBlank() }

    fun readCrashLog(maxChars: Int = 120_000): String {
        val file = crashLogFile() ?: return ""
        if (!file.isFile) return ""
        return readTail(file, maxChars)
    }

    fun readAppTrace(maxChars: Int = 80_000): String {
        val file = appTraceFile() ?: return ""
        if (!file.isFile) return ""
        return readTail(file, maxChars)
    }

    /** User-initiated only — never called automatically. */
    fun clearCrashHistory() {
        synchronized(lock) {
            crashLogFile()?.delete()
            lastCrashFile()?.delete()
        }
        i("CrashReporter", "crash history cleared by user")
    }

    fun clearAppTrace() {
        synchronized(lock) {
            appTraceFile()?.delete()
            File(diagDir() ?: return, APP_TRACE_BAK).delete()
        }
        i("CrashReporter", "app trace cleared by user")
    }

    fun troubleshootingText(
        runHistoryJson: String? = null,
        logcatSnippet: String? = null,
    ): String = buildString {
        appendLine("=== The Lookbook troubleshooting bundle ===")
        appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} api=${Build.VERSION.SDK_INT}")
        appendLine("breadcrumb=${breadcrumb.get()}")
        appendLine()
        appendLine("--- LAST CRASH (likelyCause) ---")
        appendLine(lastCrashSummary() ?: "(none on disk)")
        appendLine()
        appendLine("--- CRASH LOG (append-only, not auto-cleared) ---")
        appendLine(readCrashLog().ifBlank { "(empty)" })
        appendLine()
        appendLine("--- APP TRACE (tail) ---")
        appendLine(readAppTrace().ifBlank { "(empty)" })
        if (!logcatSnippet.isNullOrBlank()) {
            appendLine()
            appendLine("--- LOGCAT ---")
            appendLine(logcatSnippet)
        }
        if (!runHistoryJson.isNullOrBlank()) {
            appendLine()
            appendLine("--- RUN HISTORY JSON ---")
            appendLine(runHistoryJson.take(60_000))
        }
    }

    // ── internals ──────────────────────────────────────────────────────────

    private fun recordFatal(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "FATAL on ${thread.name}", throwable)
        appendCrashFile(
            header = "FATAL",
            threadName = thread.name,
            throwable = throwable,
            detail = "",
            fatal = true,
        )
    }

    private fun appendCrashFile(
        header: String,
        threadName: String,
        throwable: Throwable,
        detail: String,
        fatal: Boolean,
    ) {
        val dir = diagDir() ?: return
        val iso = isoNow()
        val stack = stackString(throwable)
        val cause = CrashClassifier.classify(throwable, stack)
        val block = buildString {
            appendLine()
            appendLine("======== $header $iso ========")
            appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("thread=$threadName")
            appendLine("breadcrumb=${breadcrumb.get()}")
            appendLine("likelyCause=$cause")
            if (detail.isNotBlank()) appendLine("detail=$detail")
            appendLine("exception=${throwable.javaClass.name}: ${throwable.message}")
            appendLine(stack)
            appendLine("======== END ========")
        }
        synchronized(lock) {
            val crashLog = File(dir, CRASH_LOG)
            rotateIfHuge(crashLog, MAX_CRASH_LOG_BYTES)
            crashLog.appendText(block)
            if (fatal) {
                File(dir, LAST_CRASH).writeText(
                    JSONObject()
                        .put("isoTime", iso)
                        .put("fatal", true)
                        .put("thread", threadName)
                        .put("breadcrumb", breadcrumb.get())
                        .put("exception", throwable.javaClass.name)
                        .put("message", throwable.message ?: "")
                        .put("likelyCause", cause)
                        .put("stackTop", stack.lineSequence().take(12).joinToString("\n"))
                        .put("version", BuildConfig.VERSION_NAME)
                        .put("versionCode", BuildConfig.VERSION_CODE)
                        .toString(2),
                )
            }
        }
    }

    private fun appendTrace(level: String, tag: String, message: String) {
        val dir = diagDir() ?: return
        val line = "${isoNow()} $level/$tag: ${message.take(500)}\n"
        synchronized(lock) {
            val file = File(dir, APP_TRACE)
            if (file.length() > MAX_TRACE_BYTES) {
                val bak = File(dir, APP_TRACE_BAK)
                bak.delete()
                file.renameTo(bak)
            }
            file.appendText(line)
        }
    }

    private fun stackString(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString().take(20_000)
    }

    private fun diagDir(): File? {
        val app = appRef.get() ?: return null
        return File(app.filesDir, DIR).also { it.mkdirs() }
    }

    private fun crashLogFile(): File? = diagDir()?.let { File(it, CRASH_LOG) }
    private fun lastCrashFile(): File? = diagDir()?.let { File(it, LAST_CRASH) }
    private fun appTraceFile(): File? = diagDir()?.let { File(it, APP_TRACE) }

    private fun lastCrashJson(): JSONObject? {
        val f = lastCrashFile() ?: return null
        if (!f.isFile) return null
        return runCatching { JSONObject(f.readText()) }.getOrNull()
    }

    private fun readTail(file: File, maxChars: Int): String {
        val text = file.readText()
        return if (text.length <= maxChars) text else text.takeLast(maxChars)
    }

    private fun rotateIfHuge(file: File, maxBytes: Long) {
        if (!file.isFile || file.length() <= maxBytes) return
        val bak = File(file.parentFile, file.name + ".1")
        bak.delete()
        file.renameTo(bak)
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
}
