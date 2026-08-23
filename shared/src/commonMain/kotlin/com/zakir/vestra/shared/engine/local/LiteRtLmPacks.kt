package com.zakir.vestra.shared.engine.local

/** Known LiteRT-LM pack ids (manifest + catalog aligned). */
object LiteRtLmPacks {
    const val GEMMA4_CODE = "local-gemma-4-e2b-v1"
    const val GEMMA4_VISION = "local-gemma-4-vision-v1"
    const val AUDIO_SCRIBE = "local-audio-scribe-v1"
    const val FUNCTION_GEMMA = "local-functiongemma-v1"
    const val LEGACY_GEMMA3 = "local-gemma-v1"

    const val GEMMA4_FILE = "gemma-4-E2B-it.litertlm"
    const val LEGACY_GEMMA3_FILE = "gemma3-1b-it-int4.task"
    const val AUDIO_SCRIBE_FILE = "whisper-large-v3-turbo.litertlm"
    const val FUNCTION_GEMMA_FILE = "mobile-actions_q8_ekv1024.litertlm"
}
