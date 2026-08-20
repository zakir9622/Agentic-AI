package com.zakir.vestra

import android.content.Context
import java.io.File

/**
 * Makes the app generate images out-of-the-box for manual testing, before the
 * Hugging Face packs repo is set up. The Lite pack is bundled in the debug
 * APK's assets (`src/debug/assets/packs/lite-v1/`); on first launch this copies
 * it into the pack directory and seeds a local manifest so the pack manager
 * reports it INSTALLED.
 *
 * In release builds the asset is absent, so [seedLitePack] no-ops — production
 * still downloads packs from Hugging Face as normal.
 */
object DebugPackBootstrap {

    private const val PACK_ID = "lite-v1"
    private const val VERSION = 1
    private val FILES = listOf("garment_seg.onnx" to 1_321_751L, "human_parse.onnx" to 67_287_788L)

    fun seedLitePack(context: Context, packsRoot: File = File(context.filesDir, "packs")) {
        val versionDir = File(packsRoot, "$PACK_ID/$VERSION")
        val completeMarker = File(versionDir, ".complete")
        if (completeMarker.exists()) return

        runCatching {
            // Probe: absent in release builds → IOException → skip entirely.
            context.assets.open("packs/$PACK_ID/garment_seg.onnx").close()

            versionDir.mkdirs()
            for ((name, _) in FILES) {
                context.assets.open("packs/$PACK_ID/$name").use { input ->
                    File(versionDir, name).outputStream().use { input.copyTo(it) }
                }
            }
            completeMarker.writeText(VERSION.toString())

            // Seed the offline manifest cache so refresh(networkAllowed=false)
            // recognizes the pack without contacting Hugging Face.
            File(packsRoot, "manifest.cache.json").writeText(localManifest())
        }
    }

    private fun localManifest(): String {
        val files = FILES.joinToString(",") { (name, bytes) ->
            """{"path":"$name","url":"bundled","sha256":"bundled","bytes":$bytes}"""
        }
        return """
            {"schemaVersion":1,"packs":[
              {"id":"$PACK_ID","version":$VERSION,"tier":"LITE",
               "displayName":"Lite engine","description":"Bundled for testing.",
               "totalBytes":68609539,"files":[$files],
               "minSpec":{"minRamMb":0,"requiresNpu":false,"minSdk":26}}
            ]}
        """.trimIndent()
    }
}
