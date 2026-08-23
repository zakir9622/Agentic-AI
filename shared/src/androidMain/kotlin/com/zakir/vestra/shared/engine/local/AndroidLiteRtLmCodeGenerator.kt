package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.google.ai.edge.litertlm.ToolSet
import com.zakir.vestra.shared.packs.ModelPackManager

/**
 * Code Studio / chat over a LiteRT-LM `.litertlm` pack (Gallery-class). Defaults to the
 * Gemma 4 E2B pack; [packId] / [primaryFile] / [minBytes] point it at any other LiteRT-LM
 * pack — e.g. the much smaller Qwen3 0.6B INT4 route.
 */
class AndroidLiteRtLmCodeGenerator(
    private val context: Context,
    private val packs: ModelPackManager,
    private val packId: String = LiteRtLmPacks.GEMMA4_CODE,
    private val useGpu: () -> Boolean = { false },
    private val tools: List<ToolSet> = emptyList(),
    private val primaryFile: String = LiteRtLmPacks.GEMMA4_FILE,
    private val minBytes: Long = LiteRtLmPackLimits.MIN_GEMMA4_BYTES,
    private val downloadHint: String = "~2.6 GB",
) : LocalCodeGenerator {

    override fun providerId(): String = packId

    override fun isReady(): Boolean =
        LiteRtLmInference.litertLmReady(packs, packId, primaryFile, minBytes)

    override fun generate(prompt: String, system: String): LocalCodeResult {
        if (!isReady()) {
            return LocalCodeResult.Unavailable(
                "Download $packId ($downloadHint) from Model packs for offline on-device generation.",
            )
        }
        val dir = packs.installedDir(packId)
            ?: return LocalCodeResult.Unavailable("$packId pack directory missing.")
        val modelPath = LiteRtLmPackConfig.modelPath(java.io.File(dir), primaryFile)
            ?: return LocalCodeResult.Unavailable("$primaryFile missing — re-download $packId.")
        @Suppress("UNCHECKED_CAST")
        return LiteRtLmInference.runText(
            context = context,
            packs = packs,
            packId = packId,
            modelPath = modelPath,
            useGpu = useGpu(),
            tools = tools,
            prompt = prompt,
            system = system,
            mapOk = { LocalCodeResult.Ok(it.text, it.tokensIn, it.tokensOut) },
            mapUnavailable = { LocalCodeResult.Unavailable(it) },
        ) as LocalCodeResult
    }
}
