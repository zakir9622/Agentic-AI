package com.zakir.vestra.shared.cloud

import kotlinx.serialization.Serializable

/**
 * Free-tier cloud platforms only. Paid hosts (Replicate, fal.ai) are intentionally excluded.
 */
enum class CloudPlatform {
    /** Hugging Face Gradio Space — free ZeroGPU quota, optional HF token. */
    HF_SPACE,
    /** Hugging Face Inference / Router API — free tier with token. */
    HF_INFERENCE,
    /** Groq — free-tier LLM inference (TPM limits). */
    GROQ,
    /** OpenRouter free models only (`:free` suffix). */
    OPENROUTER,
}

enum class AiCapability {
    TRY_ON,
    IMAGE_GEN,
    IMAGE_EDIT,
    CODE,
    VIDEO,
}

/**
 * Curated **free** open-source cloud models. Every entry must be usable without payment.
 * Cost estimates are always 0; token estimates help Usage tracking for LLMs.
 */
@Serializable
data class CloudModelProvider(
    val id: String,
    val displayName: String,
    val description: String,
    val platform: CloudPlatform,
    val capability: AiCapability,
    val endpoint: String,
    val apiName: String = "predict",
    val license: String,
    val requiresApiKey: Boolean,
    val freeTier: Boolean = true,
    val qualityScore: Int,
    val speedScore: Int,
    val estTokensPerRequest: Int = -1,
    val estCostUsd: Double = 0.0,
    val usageNote: String = "",
)

object CloudModelCatalog {
    val providers: List<CloudModelProvider> = listOf(
        // ── Virtual try-on (HF Spaces, free) ────────────────────────────
        CloudModelProvider(
            id = "idm-vton-hf",
            displayName = "IDM-VTON",
            description = "State-of-the-art diffusion try-on. Best garment fidelity. Free HF Space.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.TRY_ON,
            endpoint = "yisol-idm-vton.hf.space",
            apiName = "tryon",
            license = "CC BY-NC-SA 4.0",
            requiresApiKey = false,
            qualityScore = 95,
            speedScore = 60,
            usageNote = "Ready · tryon ImageEditor+garment+auto-mask. Free ZeroGPU.",
        ),
        CloudModelProvider(
            id = "leffa-hf",
            displayName = "Leffa",
            description = "MIT-licensed controllable try-on with fine detail preservation.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.TRY_ON,
            endpoint = "nymbo-leffa.hf.space",
            apiName = "predict",
            license = "MIT",
            requiresApiKey = false,
            qualityScore = 92,
            speedScore = 65,
            usageNote = "Degraded · host often 503. Prefer IDM-VTON.",
        ),
        CloudModelProvider(
            id = "ootd-hf",
            displayName = "OOTDiffusion",
            description = "Open outfit diffusion — strong on full-body garments like abayas.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.TRY_ON,
            endpoint = "levihsu-ootdiffusion.hf.space",
            apiName = "process_hd",
            license = "CC BY-NC-SA 4.0",
            requiresApiKey = false,
            qualityScore = 88,
            speedScore = 50,
            usageNote = "Ready · process_hd model+garment+steps/guidance/seed. Queues at peak.",
        ),
        CloudModelProvider(
            id = "fitdit-hf",
            displayName = "FitDiT",
            description = "Diffusion Transformer try-on — requires mask + pose (not supported in-app yet).",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.TRY_ON,
            endpoint = "boyuanjiang-fitdit.hf.space",
            apiName = "process",
            license = "Apache 2.0",
            requiresApiKey = false,
            qualityScore = 94,
            speedScore = 55,
            usageNote = "Unsupported in-app · needs mask editor + pose. Use IDM-VTON.",
        ),
        CloudModelProvider(
            id = "catvton-hf",
            displayName = "CatVTON",
            description = "Lightweight concatenation-based try-on. Fast free demos.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.TRY_ON,
            endpoint = "zhengchong-catvton.hf.space",
            apiName = "submit_function",
            license = "CC BY-NC 4.0",
            requiresApiKey = false,
            qualityScore = 86,
            speedScore = 80,
            usageNote = "Ready · ImageEditor person + cloth type. Prefer IDM if busy.",
        ),
        CloudModelProvider(
            id = "catvton-flux-hf",
            displayName = "CatVTON-FLUX",
            description = "CatVTON + FLUX fill inpainting — stronger garment transfer on free ZeroGPU.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.TRY_ON,
            endpoint = "xiaozaa-catvton-flux-try-on.hf.space",
            apiName = "submit_function_flux",
            license = "Non-commercial / Space terms",
            requiresApiKey = false,
            qualityScore = 91,
            speedScore = 55,
            usageNote = "Degraded · often 503/queued. Prefer IDM-VTON or CatVTON.",
        ),
        // Kolors Space no longer exposes tryon API — removed.

        // ── Image generation / recreate (free HF) ───────────────────────
        CloudModelProvider(
            id = "flux-schnell-hf",
            displayName = "FLUX.1 Schnell",
            description = "Fast open image model. Text-to-image from prompts. Free HF Space.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.IMAGE_GEN,
            endpoint = "black-forest-labs-flux-1-schnell.hf.space",
            apiName = "infer",
            license = "Apache 2.0",
            requiresApiKey = false,
            qualityScore = 93,
            speedScore = 85,
            usageNote = "Ready · infer prompt+seed+size+4 steps.",
        ),
        CloudModelProvider(
            id = "sdxl-lightning-hf",
            displayName = "SDXL Lightning",
            description = "ByteDance SDXL Lightning — quick prompt images on free HF Space.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.IMAGE_GEN,
            endpoint = "ByteDance-SDXL-Lightning.hf.space",
            apiName = "generate_image",
            license = "OpenRAIL++",
            requiresApiKey = false,
            qualityScore = 88,
            speedScore = 92,
            usageNote = "Ready · generate_image prompt + 4-Step dropdown.",
        ),
        CloudModelProvider(
            id = "qwen-image-edit-hf",
            displayName = "Qwen Image Edit",
            description = "Recreate / edit an input image from a text prompt.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.IMAGE_EDIT,
            endpoint = "qwen-qwen-image-edit.hf.space",
            apiName = "infer",
            license = "Apache 2.0",
            requiresApiKey = false,
            qualityScore = 90,
            speedScore = 70,
            usageNote = "Ready · infer reference image + prompt + guidance/steps.",
        ),
        CloudModelProvider(
            id = "instruct-pix2pix-hf",
            displayName = "InstructPix2Pix",
            description = "Edit any image with natural-language instructions.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.IMAGE_EDIT,
            endpoint = "timbrooks-instruct-pix2pix.hf.space",
            apiName = "generate",
            license = "MIT",
            requiresApiKey = false,
            qualityScore = 82,
            speedScore = 75,
            usageNote = "Ready · generate image + edit instruction + CFG/seed.",
        ),

        // ── Coding LLMs (free tiers) ────────────────────────────────────
        CloudModelProvider(
            id = "qwen25-coder-hf",
            displayName = "Qwen2.5-Coder 32B",
            description = "Strong open coding model via Hugging Face Inference (free tier).",
            platform = CloudPlatform.HF_INFERENCE,
            capability = AiCapability.CODE,
            endpoint = "Qwen/Qwen2.5-Coder-32B-Instruct",
            license = "Apache 2.0",
            requiresApiKey = true,
            qualityScore = 92,
            speedScore = 70,
            estTokensPerRequest = 2000,
            usageNote = "Ready · needs HF token with Inference Providers. Tokens tracked in Usage.",
        ),
        CloudModelProvider(
            id = "llama33-70b-groq",
            displayName = "Llama 3.3 70B (Groq)",
            description = "Fast coding/chat on Groq free tier. Great for code assist.",
            platform = CloudPlatform.GROQ,
            capability = AiCapability.CODE,
            endpoint = "llama-3.3-70b-versatile",
            license = "Llama 3.3",
            requiresApiKey = true,
            qualityScore = 91,
            speedScore = 98,
            estTokensPerRequest = 2000,
            usageNote = "Ready · Groq TPM limits. Tokens tracked in Usage. Recommended for Code.",
        ),
        CloudModelProvider(
            id = "openrouter-free",
            displayName = "OpenRouter Free Router",
            description = "OpenRouter free model router — no payment required (`openrouter/free`).",
            platform = CloudPlatform.OPENROUTER,
            capability = AiCapability.CODE,
            endpoint = "openrouter/free",
            license = "Upstream model licenses",
            requiresApiKey = true,
            qualityScore = 88,
            speedScore = 80,
            estTokensPerRequest = 2000,
            usageNote = "Ready · openrouter/free. Requires free OpenRouter API key.",
        ),
        CloudModelProvider(
            id = "deepseek-r1-free-or",
            displayName = "OpenRouter Free (legacy id)",
            description = "Legacy catalog id — routes to openrouter/free (DeepSeek :free retired).",
            platform = CloudPlatform.OPENROUTER,
            capability = AiCapability.CODE,
            endpoint = "openrouter/free",
            license = "Upstream model licenses",
            requiresApiKey = true,
            qualityScore = 88,
            speedScore = 80,
            estTokensPerRequest = 2000,
            usageNote = "Ready · legacy id → openrouter/free (DeepSeek :free retired).",
        ),

        // ── Video (free HF Spaces) ──────────────────────────────────────
        CloudModelProvider(
            id = "wan2-video-hf",
            displayName = "Wan2 Video",
            description = "Open text-to-video on free HF Space (Wan2). Queues at peak.",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.VIDEO,
            endpoint = "OpenKing-wan2-video-generation.hf.space",
            apiName = "generate_video",
            license = "Apache 2.0 / Space terms",
            requiresApiKey = false,
            qualityScore = 86,
            speedScore = 55,
            usageNote = "Degraded · generate_video 8-arg schema; queues fill often.",
        ),
        CloudModelProvider(
            id = "ltx-zerogpu-hf",
            displayName = "LTX-Video ZeroGPU",
            description = "LTX-Video community ZeroGPU Space (replacement for retired official Space).",
            platform = CloudPlatform.HF_SPACE,
            capability = AiCapability.VIDEO,
            endpoint = "DeepRat-LTX-Video-ZeroGPU-Optimized.hf.space",
            apiName = "text_to_video",
            license = "Apache 2.0",
            requiresApiKey = false,
            qualityScore = 84,
            speedScore = 70,
            usageNote = "Ready · text_to_video (14 args). Prefer when Wan2 is queued.",
        ),
    )

    init {
        require(providers.all { it.freeTier && it.estCostUsd <= 0.0 }) {
            "CloudModelCatalog must contain only free providers"
        }
        require(providers.none { it.platform.name in setOf("REPLICATE", "FAL") }) {
            "Paid platforms are not allowed"
        }
    }

    fun byId(id: String): CloudModelProvider? = providers.firstOrNull { it.id == id }

    fun forCapability(capability: AiCapability): List<CloudModelProvider> =
        providers.filter { it.capability == capability }

    fun defaultFor(capability: AiCapability): CloudModelProvider {
        val preferredId = when (capability) {
            AiCapability.TRY_ON -> defaultTryOnId
            AiCapability.IMAGE_GEN -> defaultImageGenId
            AiCapability.IMAGE_EDIT -> defaultImageEditId
            AiCapability.CODE -> defaultCodeId
            AiCapability.VIDEO -> defaultVideoId
        }
        return byId(preferredId) ?: forCapability(capability).first()
    }

    val defaultTryOnId: String = "idm-vton-hf"
    val defaultImageGenId: String = "flux-schnell-hf"
    val defaultImageEditId: String = "qwen-image-edit-hf"
    val defaultCodeId: String = "llama33-70b-groq"
    val defaultVideoId: String = "ltx-zerogpu-hf"
    val defaultId: String = defaultTryOnId
}
