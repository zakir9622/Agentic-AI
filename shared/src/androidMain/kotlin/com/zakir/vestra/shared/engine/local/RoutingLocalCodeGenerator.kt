package com.zakir.vestra.shared.engine.local

import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.settings.AppSettings

/**
 * Routes Code Studio to the user's selected on-device generator:
 * Gemma 4 (LiteRT-LM) or legacy Gemma 3 (MediaPipe).
 */
class RoutingLocalCodeGenerator(
    private val settings: AppSettings,
    private val gemma4: LocalCodeGenerator,
    private val legacyGemma3: LocalCodeGenerator,
    private val functionGemma: LocalCodeGenerator? = null,
) : LocalCodeGenerator {

    private fun delegate(): LocalCodeGenerator = when (settings.selectionId(AiCapability.CODE)) {
        LiteRtLmPacks.LEGACY_GEMMA3 -> legacyGemma3
        LiteRtLmPacks.GEMMA4_CODE -> gemma4
        LiteRtLmPacks.FUNCTION_GEMMA -> functionGemma ?: gemma4
        else -> when {
            gemma4.isReady() -> gemma4
            legacyGemma3.isReady() -> legacyGemma3
            functionGemma?.isReady() == true -> functionGemma
            else -> gemma4
        }
    }

    override fun providerId(): String = delegate().providerId()

    override fun isReady(): Boolean = delegate().isReady()

    override fun generate(prompt: String, system: String): LocalCodeResult =
        delegate().generate(prompt, system)
}
