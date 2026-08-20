package com.zakir.vestra.shared.cloud

import kotlinx.serialization.Serializable

enum class CloudPlatform {
    /** Hugging Face Gradio Space — free ZeroGPU quota, optional HF token for priority. */
    HF_SPACE,
    /** Replicate.com — pay-per-run, needs API token. */
    REPLICATE,
    /** fal.ai — fast GPU inference, needs API key. */
    FAL,
}

/**
 * Curated open-source try-on models available via free/paid cloud APIs.
 * HF Spaces are free (ZeroGPU daily quota); Replicate/FAL need user API keys.
 */
@Serializable
data class CloudModelProvider(
    val id: String,
    val displayName: String,
    val description: String,
    val platform: CloudPlatform,
    /** HF: space host (e.g. yisol-idm-vton.hf.space). Replicate: owner/model. FAL: endpoint id. */
    val endpoint: String,
    /** Gradio API name (HF only). */
    val apiName: String = "predict",
    val license: String,
    val requiresApiKey: Boolean,
    val freeTier: Boolean,
    val qualityScore: Int,
    val speedScore: Int,
    /** Replicate model version hash when platform is REPLICATE. */
    val replicateVersion: String? = null,
)

object CloudModelCatalog {
    val providers: List<CloudModelProvider> = listOf(
        CloudModelProvider(
            id = "idm-vton-hf",
            displayName = "IDM-VTON",
            description = "State-of-the-art diffusion try-on. Best garment fidelity. Free HF Space (ZeroGPU).",
            platform = CloudPlatform.HF_SPACE,
            endpoint = "yisol-idm-vton.hf.space",
            apiName = "tryon",
            license = "CC BY-NC-SA 4.0",
            requiresApiKey = false,
            freeTier = true,
            qualityScore = 95,
            speedScore = 60,
        ),
        CloudModelProvider(
            id = "leffa-hf",
            displayName = "Leffa",
            description = "MIT-licensed controllable try-on with better fine detail preservation.",
            platform = CloudPlatform.HF_SPACE,
            endpoint = "nymbo-leffa.hf.space",
            apiName = "predict",
            license = "MIT",
            requiresApiKey = false,
            freeTier = true,
            qualityScore = 92,
            speedScore = 65,
        ),
        CloudModelProvider(
            id = "ootd-hf",
            displayName = "OOTDiffusion",
            description = "Open outfit diffusion — strong on full-body garments like abayas.",
            platform = CloudPlatform.HF_SPACE,
            endpoint = "levihsu-ootdiffusion.hf.space",
            apiName = "process_hd",
            license = "CC BY-NC-SA 4.0",
            requiresApiKey = false,
            freeTier = true,
            qualityScore = 88,
            speedScore = 50,
        ),
        CloudModelProvider(
            id = "fitdit-hf",
            displayName = "FitDiT",
            description = "Diffusion Transformer try-on with exceptional garment detail. Free HF Space.",
            platform = CloudPlatform.HF_SPACE,
            endpoint = "boyuanjiang-fitdit.hf.space",
            apiName = "predict",
            license = "Apache 2.0",
            requiresApiKey = false,
            freeTier = true,
            qualityScore = 94,
            speedScore = 55,
        ),
        CloudModelProvider(
            id = "catvton-hf",
            displayName = "CatVTON",
            description = "Lightweight concatenation-based try-on. Fast free HF Space demos.",
            platform = CloudPlatform.HF_SPACE,
            endpoint = "zhengchong-catvton.hf.space",
            apiName = "predict",
            license = "CC BY-NC 4.0",
            requiresApiKey = false,
            freeTier = true,
            qualityScore = 86,
            speedScore = 80,
        ),
        CloudModelProvider(
            id = "idm-vton-replicate",
            displayName = "IDM-VTON (Replicate)",
            description = "Same model on Replicate — reliable when HF Space is queued. ~\$0.02/image.",
            platform = CloudPlatform.REPLICATE,
            endpoint = "cuuupid/idm-vton",
            license = "CC BY-NC-SA 4.0",
            requiresApiKey = true,
            freeTier = false,
            qualityScore = 95,
            speedScore = 75,
            replicateVersion = "0513734a452173b8173e907e3a59d19a36266e55b8a3c9d4883a6059c0aab5c",
        ),
        CloudModelProvider(
            id = "ootd-replicate",
            displayName = "OOTDiffusion (Replicate)",
            description = "Full-body outfit diffusion on Replicate. ~\$0.03/image.",
            platform = CloudPlatform.REPLICATE,
            endpoint = "qiweiii/oot_diffusion_dc",
            license = "CC BY-NC-SA 4.0",
            requiresApiKey = true,
            freeTier = false,
            qualityScore = 88,
            speedScore = 70,
            replicateVersion = "430dcd2e0539e9531e0d556aaeca33c8bc4160fbf0ca7f5adedb4e3c1803036e",
        ),
        CloudModelProvider(
            id = "catvton-fal",
            displayName = "CatVTON (FAL)",
            description = "Fast lightweight try-on on fal.ai. Good for quick previews.",
            platform = CloudPlatform.FAL,
            endpoint = "fal-ai/cat-vton",
            license = "CC BY-NC 4.0",
            requiresApiKey = true,
            freeTier = false,
            qualityScore = 85,
            speedScore = 90,
        ),
    )

    fun byId(id: String): CloudModelProvider? = providers.firstOrNull { it.id == id }

    val defaultId: String = "idm-vton-hf"
}
