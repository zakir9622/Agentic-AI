package com.zakir.vestra.data

import android.content.Context
import android.os.Build
import com.zakir.vestra.BuildConfig
import com.zakir.vestra.diagnostics.CrashReporter
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.usage.UsageLedger
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

object DiagnosticsExport {
    data class ShareBundle(
        val troubleshootingText: String,
        val runHistoryJson: String,
        val zipFile: File?,
        val datedJsonFile: File?,
    )

    /**
     * Captures logcat, system specs, crash traces, and pack integrity once, writes files, and
     * builds a comprehensive troubleshooting ZIP archive. Call from a background dispatcher —
     * logcat wait can ANR the main thread.
     */
    fun prepareShareBundle(
        context: Context,
        diagnostics: RunDiagnostics,
        usage: UsageLedger? = null,
        packManager: ModelPackManager? = null,
    ): ShareBundle {
        val logcat = captureLogcatSnippet()
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val bundle = diagnostics.exportBundle(
            usage = usage?.summary?.value,
            logcatSnippet = logcat,
            appVersion = appVersion,
        )
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val datedJson = File(dir, "lookbook-diagnostics-$stamp.json").apply {
            runCatching { writeText(bundle) }
        }
        val text = CrashReporter.troubleshootingText(
            runHistoryJson = bundle,
            logcatSnippet = logcat,
        )
        File(dir, "troubleshooting-$stamp.txt").apply {
            runCatching { writeText(text) }
        }

        val zipFile = File(dir, "lookbook-diagnostics-$stamp.zip")
        runCatching {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val sysInfo = JSONObject().apply {
                    put("appVersion", appVersion)
                    put("buildType", BuildConfig.BUILD_TYPE)
                    put("androidVersion", Build.VERSION.RELEASE)
                    put("sdkInt", Build.VERSION.SDK_INT)
                    put("deviceManufacturer", Build.MANUFACTURER)
                    put("deviceModel", Build.MODEL)
                    put("deviceHardware", Build.HARDWARE)
                    put("availableProcessors", Runtime.getRuntime().availableProcessors())
                    put("maxMemoryMb", Runtime.getRuntime().maxMemory() / (1024 * 1024))
                    put("totalMemoryMb", Runtime.getRuntime().totalMemory() / (1024 * 1024))
                    put("freeMemoryMb", Runtime.getRuntime().freeMemory() / (1024 * 1024))
                    put("timestampEpoch", System.currentTimeMillis())
                }.toString(2)
                addZipEntry(zos, "system_info.json", sysInfo)

                val reportMd = buildString {
                    appendLine("# The Lookbook Diagnostics & Troubleshooting Report")
                    appendLine(
                        "Generated: ${Date()} on ${Build.MANUFACTURER} ${Build.MODEL} " +
                            "(Android ${Build.VERSION.RELEASE})",
                    )
                    appendLine("App Version: $appVersion")
                    appendLine()
                    appendLine("## Summary & Classified Issues")
                    appendLine(text)
                    appendLine()
                    appendLine("## Archive Contents")
                    appendLine("- `logs/logcat_recent.txt`: System logcat warnings & errors")
                    appendLine("- `logs/crash_log.txt`: Last crash and unhandled stacktraces")
                    appendLine("- `run_history.json`: Comprehensive local & cloud generation run events")
                    appendLine("- `packs_status.json`: Local LiteRT & engine pack integrity status")
                    appendLine("- `system_info.json`: Hardware, RAM, and runtime environment")
                }
                addZipEntry(zos, "troubleshooting_report.md", reportMd)
                addZipEntry(zos, "run_history.json", bundle)
                addZipEntry(zos, "logs/logcat_recent.txt", logcat ?: "No recent logcat warnings or errors captured.")

                val crashFile = File(context.filesDir, "crash_log.txt")
                val crashText = if (crashFile.exists()) crashFile.readText() else "No fatal crashes recorded."
                addZipEntry(zos, "logs/crash_log.txt", crashText)

                val packJson = JSONObject().apply {
                    val states = packManager?.states?.value.orEmpty()
                    val array = JSONArray()
                    states.forEach { (id, state) ->
                        array.put(
                            JSONObject().apply {
                                put("id", id)
                                put("displayName", state.pack.displayName)
                                put("status", state.status.name)
                                put("progress", state.progress)
                                put("verifyStatus", state.verifyStatus.name)
                                put("totalBytes", state.pack.totalBytes)
                                put("filesCount", state.pack.files.size)
                            },
                        )
                    }
                    put("modelPacks", array)
                }.toString(2)
                addZipEntry(zos, "packs_status.json", packJson)
            }
        }

        return ShareBundle(
            troubleshootingText = text,
            runHistoryJson = bundle,
            zipFile = if (zipFile.exists() && zipFile.length() > 0) zipFile else null,
            datedJsonFile = datedJson,
        )
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    fun writeToFilesDir(
        context: Context,
        diagnostics: RunDiagnostics,
        usage: UsageLedger? = null,
        packManager: ModelPackManager? = null,
    ): File {
        val bundle = prepareShareBundle(context, diagnostics, usage, packManager)
        return bundle.zipFile ?: bundle.datedJsonFile ?: File(context.filesDir, "diagnostics/run_history.json")
    }

    fun shareTroubleshootingText(
        diagnostics: RunDiagnostics,
        usage: UsageLedger?,
    ): String {
        val logcat = captureLogcatSnippet()
        val bundle = diagnostics.exportBundle(
            usage = usage?.summary?.value,
            logcatSnippet = logcat,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
        return CrashReporter.troubleshootingText(
            runHistoryJson = bundle,
            logcatSnippet = logcat,
        )
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
