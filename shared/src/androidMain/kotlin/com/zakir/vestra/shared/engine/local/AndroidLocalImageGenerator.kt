package com.zakir.vestra.shared.engine.local

import com.zakir.vestra.shared.packs.ModelPackManager
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android local Create Studio generator (M4 / E4 / R2).
 *
 * Ready only when `local-sdturbo-v1` graphs are real **and**
 * [Txt2ImgPipeline.SAMPLER_WIRED] is true. Until then [generate] returns
 * [LocalImageResult.Unavailable] with an actionable unlock reason.
 *
 * Pro try-on packs are never used here (different UNet contract).
 */
class AndroidLocalImageGenerator(
    private val packs: ModelPackManager,
    private val packId: String = PACK_ID,
) : LocalImageGenerator {

    /**
     * Product ready = real graphs **and** [Txt2ImgPipeline.SAMPLER_WIRED].
     * Stays false in R2.0 so Create Studio never claims offline falsely.
     */
    override fun isReady(): Boolean {
        if (!Txt2ImgPipeline.SAMPLER_WIRED) return false
        return packGraphsReady()
    }

    override fun generate(prompt: String, seed: Long?): LocalImageResult {
        if (!packs.isReady(packId)) {
            return LocalImageResult.Unavailable(
                "Local image pack not installed — download $packId when published on Model packs.",
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
        return Txt2ImgPipeline(dirPath, config).generate(prompt, seed)
    }

    /** True when pack graphs look real (for Settings / catalog status only). */
    fun packGraphsReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dirPath = packs.installedDir(packId) ?: return false
        val dir = File(dirPath)
        val config = loadConfig(dir) ?: return false
        return missingOrTinyGraphs(dir, config).isEmpty()
    }

    companion object {
        const val PACK_ID = "local-sdturbo-v1"
        /** Below this, treat ONNX as scaffold/placeholder (CI contract only). */
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
