package com.zakir.vestra.shared.engine.local

/**
 * Offline Create Studio contract (generation-stability M4 / follow-up E4).
 *
 * Pack id: `local-sdturbo-v1`. Until ONNX weights are published, [isReady] is false
 * and [generate] returns a typed failure — cloud image gen remains the default path.
 */
interface LocalImageGenerator {
    fun isReady(): Boolean
    fun generate(prompt: String, seed: Long? = null): LocalImageResult
}

sealed class LocalImageResult {
    data class Ok(val imagePath: String) : LocalImageResult()
    data class Unavailable(val reason: String) : LocalImageResult()
}

/** Placeholder until SD-Turbo / LCM pack graphs ship. */
object UnimplementedLocalImageGenerator : LocalImageGenerator {
    override fun isReady(): Boolean = false
    override fun generate(prompt: String, seed: Long?): LocalImageResult =
        LocalImageResult.Unavailable(
            "Local image pack not installed — download local-sdturbo-v1 when published, " +
                "or use cloud Create Studio.",
        )
}
