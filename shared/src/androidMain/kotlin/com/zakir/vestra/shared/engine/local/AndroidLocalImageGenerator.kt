package com.zakir.vestra.shared.engine.local

import com.zakir.vestra.shared.packs.ModelPackManager
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android local Create Studio generator.
 *
 * Ready when `local-sdturbo-v1` graphs are real **and** [Txt2ImgPipeline.SAMPLER_WIRED].
 * Runs [AndroidTxt2ImgEngine] (4-ch SD-Turbo / LCM) — never Pro try-on packs.
 */
class AndroidLocalImageGenerator(
    private val packs: ModelPackManager,
    private val outputDir: File,
    private val packId: String = PACK_ID,
) : LocalImageGenerator {

    override fun isReady(): Boolean {
        if (!Txt2ImgPipeline.SAMPLER_WIRED) return false
        return packGraphsReady()
    }

    override fun generate(prompt: String, seed: Long?): LocalImageResult {
        if (!Txt2ImgPipeline.SAMPLER_WIRED) {
            return LocalImageResult.Unavailable(
                "On-device Create Studio sampler not wired in this build.",
            )
        }
        if (!packs.isReady(packId)) {
            return LocalImageResult.Unavailable(
                "Local image pack not installed — download $packId from Model packs when published " +
                    "(export via ml/export_image_gen_pack.py / Colab).",
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
                    "Export real ONNX graphs (see ml/export_image_gen_pack.py) then re-publish.",
            )
        }
        return AndroidTxt2ImgEngine(dir, config).use { engine ->
            engine.generate(prompt, seed, outputDir)
        }
    }

    fun packGraphsReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dirPath = packs.installedDir(packId) ?: return false
        val dir = File(dirPath)
        val config = loadConfig(dir) ?: return false
        return missingOrTinyGraphs(dir, config).isEmpty() &&
            File(dir, "vocab.json").isFile &&
            File(dir, "merges.txt").isFile
    }

    companion object {
        const val PACK_ID = "local-sdturbo-v1"
        const val MIN_GRAPH_BYTES = 1_000_000L

        private val json = Json { ignoreUnknownKeys = true }

        internal fun loadConfig(dir: File): LocalImagePackConfig? {
            val file = File(dir, "config.json")
            if (!file.isFile) return null
            return runCatching { json.decodeFromString<LocalImagePackConfig>(file.readText()) }.getOrNull()
        }

        internal fun missingOrTinyGraphs(dir: File, config: LocalImagePackConfig): List<String> {
            val names = listOfNotNull(
                config.graphs?.textEncoder,
                config.graphs?.unet,
                config.graphs?.vaeDecoder,
            )
            if (names.isEmpty()) return listOf("graphs")
            return names.filter { name ->
                val f = File(dir, name)
                !f.isFile || f.length() < MIN_GRAPH_BYTES
            }
        }
    }
}
