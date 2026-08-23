package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

/**
 * Multimodal Gemma 4 vision assist — describe garment / reference photos offline.
 * Uses [LiteRtLmPacks.GEMMA4_VISION] when installed, else shared [LiteRtLmPacks.GEMMA4_CODE].
 */
class AndroidLocalVisionAssist(
    private val context: Context,
    private val packs: ModelPackManager,
    private val useGpu: () -> Boolean = { false },
) : LocalVisionAssist {

    override fun isReady(): Boolean = resolveModel() != null

    override fun describeImage(imagePath: String, question: String): LocalAssistResult {
        val resolved = resolveModel()
            ?: return LocalAssistResult.Unavailable(
                "Download ${LiteRtLmPacks.GEMMA4_CODE} (~2.6 GB) from Model packs for offline vision assist.",
            )
        val (packId, modelPath) = resolved
        packs.markPackInUse(packId)
        return try {
            LiteRtLmEngine(
                context = context,
                modelPath = modelPath,
                useGpu = useGpu(),
                visionEnabled = true,
                audioEnabled = false,
            ).use { engine ->
                engine.initialize()
                when (val result = engine.describeImage(imagePath, question)) {
                    is com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult.Ok ->
                        LocalAssistResult.Ok(result.text)
                    is com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult.Unavailable ->
                        LocalAssistResult.Unavailable(result.reason)
                }
            }
        } catch (err: Throwable) {
            LocalAssistResult.Unavailable(
                err.message?.take(200) ?: "Vision assist failed.",
            )
        } finally {
            packs.markPackIdle(packId)
        }
    }

    private fun resolveModel(): Pair<String, String>? {
        val path = LiteRtLmPackResolver.modelPath(
            packs,
            LiteRtLmPacks.GEMMA4_VISION,
            LiteRtLmPacks.GEMMA4_FILE,
            LiteRtLmPacks.GEMMA4_CODE,
        ) ?: return null
        val file = File(path.second)
        if (file.length() < LiteRtLmPackLimits.MIN_GEMMA4_BYTES) return null
        return path
    }
}
