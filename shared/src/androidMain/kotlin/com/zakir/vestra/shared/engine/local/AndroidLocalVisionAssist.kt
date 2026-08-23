package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

/**
 * Multimodal Gemma 4 vision assist — describe garment / reference photos offline.
 */
class AndroidLocalVisionAssist(
    private val context: Context,
    private val packs: ModelPackManager,
    private val packId: String = LiteRtLmPacks.GEMMA4_VISION,
    private val useGpu: () -> Boolean = { false },
) : LocalVisionAssist {

    override fun isReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dir = packs.installedDir(packId) ?: return false
        val path = LiteRtLmPackConfig.modelPath(File(dir), LiteRtLmPacks.GEMMA4_FILE) ?: return false
        return File(path).length() >= LiteRtLmPackLimits.MIN_GEMMA4_BYTES
    }

    override fun describeImage(imagePath: String, question: String): LocalAssistResult {
        if (!isReady()) {
            return LocalAssistResult.Unavailable(
                "Download $packId from Model packs for offline vision assist.",
            )
        }
        val dir = packs.installedDir(packId)
            ?: return LocalAssistResult.Unavailable("Vision pack directory missing.")
        val modelPath = LiteRtLmPackConfig.modelPath(File(dir), LiteRtLmPacks.GEMMA4_FILE)
            ?: return LocalAssistResult.Unavailable("Vision .litertlm missing — re-download pack.")
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
}
