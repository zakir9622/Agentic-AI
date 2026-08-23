package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.google.ai.edge.litertlm.ToolSet
import com.zakir.vestra.shared.packs.ModelPackManager

/**
 * Gemma 4 E2B Code Studio via LiteRT-LM (Gallery-class `.litertlm` pack).
 */
class AndroidLiteRtLmCodeGenerator(
    private val context: Context,
    private val packs: ModelPackManager,
    private val packId: String = LiteRtLmPacks.GEMMA4_CODE,
    private val useGpu: () -> Boolean = { false },
    private val tools: List<ToolSet> = emptyList(),
) : LocalCodeGenerator {

    override fun providerId(): String = packId

    override fun isReady(): Boolean = LiteRtLmInference.gemma4Ready(packs, packId)

    override fun generate(prompt: String, system: String): LocalCodeResult {
        if (!isReady()) {
            return LocalCodeResult.Unavailable(
                "Download $packId (~2.6 GB) from Model packs for offline Gemma 4 Code Studio.",
            )
        }
        val dir = packs.installedDir(packId)
            ?: return LocalCodeResult.Unavailable("Gemma 4 pack directory missing.")
        val modelPath = LiteRtLmPackConfig.modelPath(java.io.File(dir), LiteRtLmPacks.GEMMA4_FILE)
            ?: return LocalCodeResult.Unavailable("Gemma 4 .litertlm missing — re-download pack.")
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
