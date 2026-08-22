package com.zakir.vestra.shared.packs

import com.zakir.vestra.shared.domain.ModelPack
import com.zakir.vestra.shared.domain.PackKind
import com.zakir.vestra.shared.engine.lite.LiteEngine
import com.zakir.vestra.shared.engine.lite.OrtModel
import com.zakir.vestra.shared.quality.AndroidQualityPostProcessor
import java.io.File

/**
 * Android ONNX + file integrity checks for installed model packs.
 *
 * Startup / re-verify must stay cheap: never smoke-load Pro companion graphs or
 * run Real-ESRGAN inference here — those paths caused native OOM/SIGSEGV process
 * deaths on Pixel 9 (see troubleshooting bundles @ v3.0.10).
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
        pack.id == AndroidQualityPostProcessor.REALESRGAN_PACK ||
            pack.id.contains("realesrgan", ignoreCase = true) -> verifyRealesrganPack(dir)
        pack.id == AndroidQualityPostProcessor.BIREFNET_PACK ||
            pack.id.contains("birefnet", ignoreCase = true) -> verifyBirefnetPack(dir)
        else -> verifyManifestOnnxFiles(pack, dir)
    }

    private fun verifyLitePack(dir: String): String? {
        val required = listOf("garment_seg.onnx", "human_parse.onnx")
        for (name in required) {
            // Always CPU — NNAPI during verify has killed the process.
            loadOnnxSessionCpu("$dir/$name")?.let { return it }
        }
        return null
    }

    /**
     * Pro graphs (VAE / ControlNet / IP-Adapter) are hundreds of MB each.
     * File-size checks in [verifyFiles] are the safe startup gate; skip session create.
     */
    private fun verifyProPack(dir: String): String? {
        if (!File(dir, "config.json").exists()) return "Pro config.json missing"
        if (!File(dir, "unet.onnx").exists()) return "No ONNX files in Pro pack"
        return null
    }

    /** Presence + byte length already checked; avoid NNAPI smoke inference. */
    private fun verifyRealesrganPack(dir: String): String? {
        val onnx = File(dir).listFiles()?.firstOrNull { it.name.endsWith(".onnx") }
            ?: return "No ONNX file found"
        if (!onnx.isFile || onnx.length() < 1_000L) {
            return "Real-ESRGAN ONNX missing or empty"
        }
        return null
    }

    /** BiRefNet — CPU session open only (no 1024 inference). */
    private fun verifyBirefnetPack(dir: String): String? {
        val onnx = File(dir).listFiles()?.firstOrNull { it.name.endsWith(".onnx") }
            ?: return "No ONNX file found"
        return loadOnnxSessionCpu(onnx.absolutePath)
    }

    private fun verifyManifestOnnxFiles(pack: ModelPack, dir: String): String? {
        for (file in pack.files) {
            if (!file.path.endsWith(".onnx")) continue
            // Skip huge graphs by name heuristic.
            val name = file.path.substringAfterLast('/').lowercase()
            if (name == "unet.onnx" || name.contains("vae") || name.contains("controlnet")) {
                continue
            }
            loadOnnxSessionCpu("$dir/${file.path}")?.let { return "${file.path}: $it" }
        }
        return null
    }

    private fun loadOnnxSessionCpu(modelPath: String): String? = runCatching {
        OrtModel(modelPath, useNnapi = false).use { /* session created in constructor */ }
        null
    }.getOrElse { error ->
        error.message?.take(120) ?: error::class.simpleName ?: "ONNX load failed"
    }
}
