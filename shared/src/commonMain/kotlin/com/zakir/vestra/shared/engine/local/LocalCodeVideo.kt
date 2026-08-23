package com.zakir.vestra.shared.engine.local

/**
 * Offline Code Studio contract — Gemma via LiteRT-LM (Gemma 4) or legacy MediaPipe (Gemma 3).
 */
interface LocalCodeGenerator {
    /** Catalog / pack id emitted as providerId in GenerativeState.CodeReady. */
    fun providerId(): String
    fun isReady(): Boolean
    fun generate(prompt: String, system: String = ""): LocalCodeResult

    /**
     * Loads the model so the first real generation doesn't pay the cold start.
     *
     * A multi-GB LiteRT-LM pack takes seconds to a minute to initialize; doing that lazily
     * inside the first generate() is what made selecting a model feel like nothing happened
     * and then made the first prompt appear to hang. Called when the user picks the model, on
     * a background dispatcher. Returns the failure reason, or null on success.
     */
    fun warmUp(): String? = if (isReady()) null else "Pack not installed"
}

sealed class LocalCodeResult {
    data class Ok(val text: String, val tokensIn: Int = 0, val tokensOut: Int = 0) : LocalCodeResult()
    data class Unavailable(val reason: String) : LocalCodeResult()
}

object UnimplementedLocalCodeGenerator : LocalCodeGenerator {
    override fun providerId(): String = LiteRtLmPacks.GEMMA4_CODE
    override fun isReady(): Boolean = false
    override fun generate(prompt: String, system: String): LocalCodeResult =
        LocalCodeResult.Unavailable(
            "Local code model not installed — download local-gemma-4-e2b-v1 from Model packs, or use cloud Code Studio.",
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
