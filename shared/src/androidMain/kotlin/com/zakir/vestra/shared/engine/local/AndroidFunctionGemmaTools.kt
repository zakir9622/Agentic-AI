package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.packs.ModelPackManager

/**
 * FunctionGemma 270M tool-calling pack (Gallery Mobile Actions class).
 * Debug / experimental — requires [LiteRtLmPacks.FUNCTION_GEMMA] installed.
 */
class AndroidFunctionGemmaTools(
    private val context: Context,
    private val packs: ModelPackManager,
    private val useGpu: () -> Boolean = { false },
    private val toolSet: LookbookStudioToolSet = LookbookStudioToolSet(),
) : LocalCodeGenerator {

    override fun providerId(): String = LiteRtLmPacks.FUNCTION_GEMMA

    override fun isReady(): Boolean {
        val resolved = LiteRtLmPackResolver.modelPath(
            packs,
            LiteRtLmPacks.FUNCTION_GEMMA,
            LiteRtLmPacks.FUNCTION_GEMMA_FILE,
        ) ?: return false
        return java.io.File(resolved.second).length() >= LiteRtLmPackLimits.MIN_FUNCTION_BYTES
    }

    override fun generate(prompt: String, system: String): LocalCodeResult {
        val resolved = LiteRtLmPackResolver.modelPath(
            packs,
            LiteRtLmPacks.FUNCTION_GEMMA,
            LiteRtLmPacks.FUNCTION_GEMMA_FILE,
        ) ?: return LocalCodeResult.Unavailable(
            "Download ${LiteRtLmPacks.FUNCTION_GEMMA} from Model packs for local tools.",
        )
        val (packId, modelPath) = resolved
        packs.markPackInUse(packId)
        return try {
            LiteRtLmEngine(
                context = context,
                modelPath = modelPath,
                useGpu = useGpu(),
                visionEnabled = false,
                audioEnabled = false,
                tools = listOf(toolSet),
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
            LocalCodeResult.Unavailable(err.message?.take(200) ?: "FunctionGemma tools failed.")
        } finally {
            packs.markPackIdle(packId)
        }
    }
}
