package com.zakir.vestra.shared.engine.local

/**
 * Offline Create Studio contract (txt2img + optional img2img edit).
 *
 * Pack id: `local-sdturbo-v1`.
 */
interface LocalImageGenerator {
    fun isReady(): Boolean
    /** True when VAE encoder is present — enables offline image edit. */
    fun isEditReady(): Boolean = false
    fun generate(prompt: String, seed: Long? = null, referenceImageUri: String? = null): LocalImageResult
}

sealed class LocalImageResult {
    data class Ok(val imagePath: String) : LocalImageResult()
    data class Unavailable(val reason: String) : LocalImageResult()
}

/** Placeholder until SD-Turbo / LCM pack graphs ship. */
object UnimplementedLocalImageGenerator : LocalImageGenerator {
    override fun isReady(): Boolean = false
    override fun generate(prompt: String, seed: Long?, referenceImageUri: String?): LocalImageResult =
        LocalImageResult.Unavailable(
            "Local image pack not published yet — use cloud Create Studio, " +
                "or wait for local-sdturbo-v1 weights on Model packs.",
        )
}

/**
 * Tracks pack install state for future use. [isReady] stays false until the
 * ONNX runner is implemented — otherwise Create Studio would skip cloud and
 * always fail with “runner not wired.”
 */
class PackAwareLocalImageGenerator(
    private val packReady: () -> Boolean,
    private val runnerImplemented: Boolean = false,
) : LocalImageGenerator {
    override fun isReady(): Boolean = runnerImplemented && packReady()

    override fun generate(prompt: String, seed: Long?, referenceImageUri: String?): LocalImageResult {
        if (!runnerImplemented) {
            return LocalImageResult.Unavailable(
                "Local SD-Turbo runner not wired yet — using cloud Create Studio.",
            )
        }
        if (!packReady()) {
            return LocalImageResult.Unavailable(
                "Local image pack not ready — install local-sdturbo-v1 from Model packs.",
            )
        }
        return LocalImageResult.Unavailable(
            "Local SD-Turbo runner not wired yet — using cloud Create Studio.",
        )
    }
}
