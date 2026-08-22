package com.zakir.vestra.shared.engine.local

/**
 * Offline Create Studio contract (generation-stability M4 / follow-up E4).
 *
 * Pack id: `local-sdturbo-v1`. Until ONNX weights **and** a wired runner exist,
 * [isReady] is false and [generate] returns a typed failure — cloud image gen
 * remains the default path.
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

    override fun generate(prompt: String, seed: Long?): LocalImageResult {
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
