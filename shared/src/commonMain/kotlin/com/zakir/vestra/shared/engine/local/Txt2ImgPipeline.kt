package com.zakir.vestra.shared.engine.local

/**
 * On-device SD-Turbo / LCM txt2img pipeline (Create Studio).
 *
 * Separate from Pro try-on UNet (9-channel inpaint). Reuses the same ORT /
 * LatentCodec / scheduler *ideas* once real graphs ship — never the Pro pack
 * files themselves.
 *
 * Unlock checklist (R2.2):
 * 1. Publish `local-sdturbo-v1` with text_encoder / unet / vae_decoder ≥ 1 MB each
 * 2. Flip [SAMPLER_WIRED] after wiring UnetRunner-style denoising
 * 3. Point [AndroidLocalImageGenerator.isReady] at [isRunnable]
 */
class Txt2ImgPipeline(
    private val packDir: String,
    private val config: LocalImagePackConfig,
) {
    fun missingRequirements(): List<String> {
        val missing = mutableListOf<String>()
        if (!SAMPLER_WIRED) {
            missing += "sampler (Txt2ImgPipeline.SAMPLER_WIRED=false)"
        }
        val graphs = listOfNotNull(
            config.graphs?.textEncoder,
            config.graphs?.unet,
            config.graphs?.vaeDecoder,
        )
        if (graphs.isEmpty()) {
            missing += "graphs in config.json"
        }
        // File presence is checked by AndroidLocalImageGenerator (needs java.io.File).
        return missing
    }

    fun isRunnable(): Boolean = SAMPLER_WIRED && config.graphs != null

    /**
     * Product sampling entry — not implemented until [SAMPLER_WIRED].
     * Callers must treat [LocalImageResult.Unavailable] as the success path today.
     */
    fun generate(prompt: String, seed: Long?): LocalImageResult {
        val missing = missingRequirements()
        if (missing.isNotEmpty()) {
            return LocalImageResult.Unavailable(
                "On-device Create Studio locked ($packDir): ${missing.joinToString()}. " +
                    "Export weights via ml/export_image_gen_pack.py, then flip SAMPLER_WIRED.",
            )
        }
        return LocalImageResult.Unavailable(
            "Txt2ImgPipeline runnable but generate() not productized — unexpected state.",
        )
    }

    companion object {
        /**
         * Flip to true only when denoising loop is wired against real ONNX graphs
         * and airplane-mode Create Studio is proven on a flagship device.
         */
        const val SAMPLER_WIRED: Boolean = false
    }
}
