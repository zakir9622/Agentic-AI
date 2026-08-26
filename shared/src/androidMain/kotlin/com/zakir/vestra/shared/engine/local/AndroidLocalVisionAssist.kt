package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngineCache
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

/**
 * Multimodal Gemma 4 vision assist — describe garment / reference photos offline.
 * Uses [LiteRtLmPacks.GEMMA4_CODE] pack with vision backend from config.json.
 */
class AndroidLocalVisionAssist(
    private val context: Context,
    private val packs: ModelPackManager,
    private val useGpu: () -> Boolean = { false },
    private val useNpu: () -> Boolean = { false },
    private val enableSpeculativeDecoding: () -> Boolean = { false },
) : LocalVisionAssist {

    override fun isReady(): Boolean = readinessReason() == null

    override fun readinessReason(): String? {
        val resolved = resolveModel()
            ?: return "Download ${LiteRtLmPacks.GEMMA4_CODE} (~2.6 GB) from Model packs for offline vision assist."
        val spec = LiteRtLmEngineCache.EngineSpec(
            modelPath = resolved.modelPath,
            useGpu = useGpu(),
            visionEnabled = resolved.config.vision,
            audioEnabled = false,
            useNpu = useNpu(),
            enableSpeculativeDecoding = enableSpeculativeDecoding(),
        )
        // A pack whose vision-encoder signature the SDK has already rejected once fails
        // deterministically forever (LiteRtLmEngineCache caches it, durably across restarts) —
        // reflect that up front instead of letting the UI offer a toggle that will just silently
        // "skip" the assist every time.
        return LiteRtLmEngineCache.failureReason(spec)?.let {
            "Offline vision analysis isn't available for the installed pack — it needs a " +
                "corrected model export. Reinstalling the pack won't fix this."
        }
    }

    override fun describeImage(imagePath: String, question: String): LocalAssistResult {
        val resolved = resolveModel()
            ?: return LocalAssistResult.Unavailable(
                "Download ${LiteRtLmPacks.GEMMA4_CODE} (~2.6 GB) from Model packs for offline vision assist.",
            )
        @Suppress("UNCHECKED_CAST")
        return LiteRtLmInference.runVision(
            context = context,
            packs = packs,
            packId = resolved.packId,
            modelPath = resolved.modelPath,
            useGpu = useGpu(),
            visionEnabled = resolved.config.vision,
            imagePath = imagePath,
            question = question,
            mapOk = { LocalAssistResult.Ok(it.text) },
            mapUnavailable = { LocalAssistResult.Unavailable(it) },
            useNpu = useNpu(),
            enableSpeculativeDecoding = enableSpeculativeDecoding(),
        ) as LocalAssistResult
    }

    private fun resolveModel(): LiteRtLmPackResolver.ResolvedPack? {
        val resolved = LiteRtLmPackResolver.resolveWithConfig(
            packs,
            LiteRtLmPacks.GEMMA4_CODE,
            LiteRtLmPacks.GEMMA4_FILE,
        ) ?: return null
        val file = File(resolved.modelPath)
        if (file.length() < LiteRtLmPackLimits.MIN_GEMMA4_BYTES) return null
        if (!resolved.config.vision) return null
        return resolved
    }
}
