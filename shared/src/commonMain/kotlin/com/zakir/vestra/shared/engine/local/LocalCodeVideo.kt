package com.zakir.vestra.shared.engine.local

/**
 * Offline Code Studio contract — Gemma / small coder via MediaPipe when pack installed.
 */
interface LocalCodeGenerator {
    fun isReady(): Boolean
    fun generate(prompt: String, system: String = ""): LocalCodeResult
}

sealed class LocalCodeResult {
    data class Ok(val text: String, val tokensIn: Int = 0, val tokensOut: Int = 0) : LocalCodeResult()
    data class Unavailable(val reason: String) : LocalCodeResult()
}

object UnimplementedLocalCodeGenerator : LocalCodeGenerator {
    override fun isReady(): Boolean = false
    override fun generate(prompt: String, system: String): LocalCodeResult =
        LocalCodeResult.Unavailable(
            "Local code model not installed — download local-gemma-v1 from Model packs, or use cloud Code Studio.",
        )
}

/**
 * Offline Video Studio contract — local still clip from on-device image gen.
 * True diffusion video is not phone-practical; this produces a short MP4 from a local PNG.
 */
interface LocalVideoGenerator {
    fun isReady(): Boolean
    fun generate(prompt: String, seed: Long? = null): LocalVideoResult
}

sealed class LocalVideoResult {
    data class Ok(val videoPath: String) : LocalVideoResult()
    data class Unavailable(val reason: String) : LocalVideoResult()
}

object UnimplementedLocalVideoGenerator : LocalVideoGenerator {
    override fun isReady(): Boolean = false
    override fun generate(prompt: String, seed: Long?): LocalVideoResult =
        LocalVideoResult.Unavailable(
            "Local video needs local-sdturbo-v1 installed — download from Model packs for offline still clips.",
        )
}
