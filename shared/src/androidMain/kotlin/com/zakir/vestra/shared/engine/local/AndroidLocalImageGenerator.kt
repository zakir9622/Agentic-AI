package com.zakir.vestra.shared.engine.local

import android.graphics.Bitmap
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.engine.lite.OrtSessionCache
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android local Create / Edit Studio generator.
 *
 * Ready when `local-sdturbo-v1` graphs are real **and** [Txt2ImgPipeline.SAMPLER_WIRED].
 * Edit ready when `vae_encoder.onnx` is also present (pack v3+).
 * Runs [AndroidTxt2ImgEngine] (4-ch SD-Turbo / LCM) — never Pro try-on packs.
 */
class AndroidLocalImageGenerator(
    private val packs: ModelPackManager,
    private val outputDir: File,
    private val loadReferenceBitmap: (uri: String) -> Bitmap? = { null },
    private val packId: String = PACK_ID,
) : LocalImageGenerator {

    override fun isReady(): Boolean {
        if (!Txt2ImgPipeline.SAMPLER_WIRED) return false
        return packGraphsReady()
    }

    override fun isEditReady(): Boolean {
        if (!isReady()) return false
        val dirPath = packs.installedDir(packId) ?: return false
        val dir = File(dirPath)
        val config = loadConfig(dir) ?: return false
        val name = config.graphs?.vaeEncoder ?: "vae_encoder.onnx"
        val enc = File(dir, name)
        return enc.isFile && enc.length() >= MIN_GRAPH_BYTES
    }

    override fun generate(prompt: String, seed: Long?, referenceImageUri: String?): LocalImageResult {
        if (!Txt2ImgPipeline.SAMPLER_WIRED) {
            return LocalImageResult.Unavailable(
                "On-device Create Studio sampler not wired in this build.",
            )
        }
        if (!packs.isReady(packId)) {
            return LocalImageResult.Unavailable(
                "Local image pack not installed — download $packId from Model packs " +
                    "(~1 GB). Then Create and Edit work offline.",
            )
        }
        val dirPath = packs.installedDir(packId)
            ?: return LocalImageResult.Unavailable("Local image pack directory missing.")
        val dir = File(dirPath)
        val config = loadConfig(dir)
            ?: return LocalImageResult.Unavailable(
                "Pack config.json missing or invalid — re-download $packId.",
            )
        val missing = missingOrTinyGraphs(dir, config)
        if (missing.isNotEmpty()) {
            return LocalImageResult.Unavailable(
                "Local SD-Turbo weights incomplete (${missing.joinToString()}). " +
                    "Re-download $packId from Model packs.",
            )
        }
        val wantsEdit = !referenceImageUri.isNullOrBlank()
        val referenceBitmap = if (wantsEdit) {
            loadReferenceBitmap(referenceImageUri!!)
                ?: return LocalImageResult.Unavailable(
                    "Couldn't read the reference image for local edit.",
                )
        } else {
            null
        }
        if (wantsEdit) {
            val encName = config.graphs?.vaeEncoder ?: "vae_encoder.onnx"
            val enc = File(dir, encName)
            if (!enc.isFile || enc.length() < MIN_GRAPH_BYTES) {
                referenceBitmap?.recycle()
                return LocalImageResult.Unavailable(
                    "Local image edit needs vae_encoder.onnx — re-download $packId (v3+).",
                )
            }
        }
        return try {
            packs.markPackInUse(packId)
            OrtSessionCache.enterInference()
            AndroidTxt2ImgEngine(dir, config).use { engine ->
                engine.generate(prompt, seed, outputDir, referenceBitmap = referenceBitmap)
            }
        } finally {
            OrtSessionCache.leaveInference()
            packs.markPackIdle(packId)
            if (referenceBitmap != null && !referenceBitmap.isRecycled) {
                referenceBitmap.recycle()
            }
        }
    }

    fun packGraphsReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dirPath = packs.installedDir(packId) ?: return false
        val dir = File(dirPath)
        val config = loadConfig(dir) ?: return false
        return packComplete(dir, config)
    }

    companion object {
        const val PACK_ID = LocalSdturboPackValidator.PACK_ID
        const val MIN_GRAPH_BYTES = LocalSdturboPackValidator.MIN_GRAPH_BYTES

        private val json = Json { ignoreUnknownKeys = true }

        internal fun loadConfig(dir: File): LocalImagePackConfig? {
            val file = File(dir, "config.json")
            if (!file.isFile) return null
            return runCatching { json.decodeFromString<LocalImagePackConfig>(file.readText()) }.getOrNull()
        }

        internal fun missingOrTinyGraphs(dir: File, config: LocalImagePackConfig): List<String> =
            LocalSdturboPackValidator.missingGraphs(config) { name ->
                val f = File(dir, name)
                if (!f.isFile) null else f.length()
            }

        internal fun packComplete(dir: File, config: LocalImagePackConfig): Boolean =
            LocalSdturboPackValidator.isComplete(
                config,
                fileBytes = { name ->
                    val f = File(dir, name)
                    if (!f.isFile) null else f.length()
                },
                fileExists = { name -> File(dir, name).isFile },
            )
    }
}
