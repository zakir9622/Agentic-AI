package com.zakir.vestra.data

import android.content.Context
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
import com.zakir.vestra.shared.usage.UsageLedger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsExport {
    fun writeToFilesDir(
        context: Context,
        diagnostics: RunDiagnostics,
        usage: UsageLedger? = null,
    ): File {
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        val bundle = diagnostics.exportBundle(usage?.summary?.value)
        val history = File(dir, "run_history.json")
        history.writeText(bundle)
        val dated = File(
            dir,
            "lookbook-diagnostics-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json",
        )
        dated.writeText(bundle)
        return dated
    }
}
