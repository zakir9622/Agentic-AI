package com.zakir.vestra.data

import android.content.Context
import com.zakir.vestra.BuildConfig
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
import com.zakir.vestra.shared.usage.UsageLedger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DiagnosticsExport {
    fun writeToFilesDir(
        context: Context,
        diagnostics: RunDiagnostics,
        usage: UsageLedger? = null,
    ): File {
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        val bundle = diagnostics.exportBundle(
            usage = usage?.summary?.value,
            logcatSnippet = captureLogcatSnippet(),
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
        val history = File(dir, "run_history.json")
        history.writeText(bundle)
        val dated = File(
            dir,
            "lookbook-diagnostics-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json",
        )
        dated.writeText(bundle)
        return dated
    }

    /**
     * Best-effort recent logcat (warnings+). Returns null if the process cannot run
     * (restricted devices / missing READ_LOGS). Never throws into the share path.
     */
    fun captureLogcatSnippet(maxLines: Int = 200, maxChars: Int = 48_000): String? =
        runCatching {
            val proc = ProcessBuilder("logcat", "-d", "-t", maxLines.toString(), "*:W")
                .redirectErrorStream(true)
                .start()
            val finished = proc.waitFor(4, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@runCatching null
            }
            val text = proc.inputStream.bufferedReader().use { it.readText() }.trim()
            if (text.isBlank()) null else text.take(maxChars)
        }.getOrNull()
}
