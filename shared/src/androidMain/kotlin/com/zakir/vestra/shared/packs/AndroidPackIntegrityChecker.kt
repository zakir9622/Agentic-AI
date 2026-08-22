package com.zakir.vestra.shared.packs

import com.zakir.vestra.shared.domain.ModelPack
import com.zakir.vestra.shared.domain.PackKind
import com.zakir.vestra.shared.engine.lite.LiteEngine
import com.zakir.vestra.shared.engine.lite.OrtModel
import java.io.File

/**
 * Android ONNX + file integrity checks for installed model packs.
 * Mirrors the smoke tests in ml/export_lite_pack.py and ml/convert_pro_pack.py.
 */
class AndroidPackIntegrityChecker : PackIntegrityChecker {

    override fun verifyFiles(pack: ModelPack, dir: String): String? {
        for (file in pack.files) {
            val path = File(dir, file.path)
            if (!path.exists()) {
                return "Missing ${file.path} — re-download ${pack.displayName}"
            }
            if (path.length() != file.bytes) {
                return "${file.path} is incomplete (${path.length()} / ${file.bytes} bytes)"
            }
        }
        if (pack.id.startsWith("pro-")) {
            val config = File(dir, "config.json")
            if (!config.exists()) {
                return "Missing config.json — Pro pack is incomplete"
            }
        }
        return null
    }

    override fun verifyOnnx(pack: ModelPack, dir: String): String? = when {
        pack.kind == PackKind.MODELS -> null
        pack.id == LiteEngine.PACK_ID -> verifyLitePack(dir)
        pack.id.startsWith("pro-") -> verifyProPack(dir)
        pack.id.endsWith("-v1") && pack.id.contains("realesrgan") ||
            pack.id.contains("birefnet") -> verifyAnyOnnx(dir)
        else -> verifyManifestOnnxFiles(pack, dir)
    }

    private fun verifyLitePack(dir: String): String? {
        val required = listOf("garment_seg.onnx", "human_parse.onnx")
        for (name in required) {
            loadOnnxSession("$dir/$name")?.let { return it }
        }
        return null
    }

    private fun verifyProPack(dir: String): String? {
        val configFile = File(dir, "config.json")
        if (!configFile.exists()) return "Pro config.json missing"
        if (!File(dir, "unet.onnx").exists()) return "No ONNX files in Pro pack"
        // Skip unet.onnx — ~2 GB with external weights; file sizes are checked in verifyFiles.
        // Smoke-load smaller companion graphs only to avoid OOM during startup verify.
        val skip = setOf("unet.onnx")
        val onnxFiles = File(dir).listFiles()
            ?.filter { it.name.endsWith(".onnx") && it.name !in skip }
            .orEmpty()
            .sortedBy { it.length() }
        for (file in onnxFiles) {
            loadOnnxSession(file.absolutePath)?.let { return "${file.name}: $it" }
        }
        return null
    }

    private fun verifyAnyOnnx(dir: String): String? {
        val onnx = File(dir).listFiles()?.firstOrNull { it.name.endsWith(".onnx") }
            ?: return "No ONNX file found"
        return loadOnnxSession(onnx.absolutePath)
    }

    private fun verifyManifestOnnxFiles(pack: ModelPack, dir: String): String? {
        for (file in pack.files) {
            if (!file.path.endsWith(".onnx")) continue
            loadOnnxSession("$dir/${file.path}")?.let { return "${file.path}: $it" }
        }
        return null
    }

    private fun loadOnnxSession(modelPath: String): String? = runCatching {
        OrtModel(modelPath).use { /* session created in constructor */ }
        null
    }.getOrElse { error ->
        error.message?.take(120) ?: error::class.simpleName ?: "ONNX load failed"
    }
}
