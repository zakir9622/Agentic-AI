package com.zakir.vestra.shared.engine.local

import com.zakir.vestra.shared.packs.ModelPackManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android local Create Studio generator (M4 / E4).
 *
 * Ready only when `local-sdturbo-v1` is installed **and** every declared ONNX graph
 * is present with a real weight size (scaffold placeholders are rejected).
 * Full SD-Turbo sampling reuses Pro OrtGraph once weights ship — until then
 * [generate] returns [LocalImageResult.Unavailable] with an actionable reason.
 */
class AndroidLocalImageGenerator(
    private val packs: ModelPackManager,
    private val packId: String = PACK_ID,
) : LocalImageGenerator {

    override fun isReady(): Boolean = packGraphsReady()

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
        // Graphs present — sampling pipeline not productized yet (reuse Pro UnetRunner).
        return LocalImageResult.Unavailable(
            "Local SD-Turbo graphs found but sampling runner not wired yet — using cloud Create Studio.",
        )
    }

    private fun packGraphsReady(): Boolean {
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

@Serializable
data class LocalImagePackConfig(
    val version: Int = 1,
    val graphs: LocalImageGraphs? = null,
    val scheduler: LocalImageScheduler? = null,
    val resolution: Int = 512,
    val lcmDistilled: Boolean = true,
)

@Serializable
data class LocalImageGraphs(
    val text_encoder: String? = null,
    val unet: String? = null,
    val vae_decoder: String? = null,
) {
    val textEncoder: String? get() = text_encoder
    val vaeDecoder: String? get() = vae_decoder
}

@Serializable
data class LocalImageScheduler(
    val type: String = "lcm",
    val steps: Int = 4,
    val guidance: Float = 1.0f,
)
