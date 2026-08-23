package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.google.ai.edge.litertlm.ToolSet
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

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

    override fun isReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dir = packs.installedDir(packId) ?: return false
        val path = LiteRtLmPackConfig.modelPath(File(dir), LiteRtLmPacks.GEMMA4_FILE) ?: return false
        return File(path).length() >= LiteRtLmPackLimits.MIN_GEMMA4_BYTES
    }

    override fun generate(prompt: String, system: String): LocalCodeResult {
        if (!isReady()) {
            return LocalCodeResult.Unavailable(
                "Download $packId (~2.6 GB) from Model packs for offline Gemma 4 Code Studio.",
            )
        }
        val dir = packs.installedDir(packId)
            ?: return LocalCodeResult.Unavailable("Gemma 4 pack directory missing.")
        val modelPath = LiteRtLmPackConfig.modelPath(File(dir), LiteRtLmPacks.GEMMA4_FILE)
            ?: return LocalCodeResult.Unavailable("Gemma 4 .litertlm missing — re-download pack.")
        packs.markPackInUse(packId)
        return try {
            LiteRtLmEngine(
                context = context,
                modelPath = modelPath,
                useGpu = useGpu(),
                visionEnabled = false,
                audioEnabled = false,
                tools = tools,
            ).use { engine ->
                engine.initialize()
                when (val result = engine.generateText(prompt, system)) {
                    is com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult.Ok ->
                        LocalCodeResult.Ok(result.text, result.tokensIn, result.tokensOut)
                    is com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult.Unavailable ->
                        LocalCodeResult.Unavailable(result.reason)
                }
            }
        } catch (err: Throwable) {
            LocalCodeResult.Unavailable(
                err.message?.take(200) ?: "On-device Gemma 4 failed — try cloud Code Studio.",
            )
        } finally {
            packs.markPackIdle(packId)
        }
    }
}
