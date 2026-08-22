package com.zakir.vestra.shared.local

import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.domain.EngineTier

/**
 * Catalog of open-source models that run on-device (your phone as a local AI device).
 * Packs are downloaded once from Hugging Face and then work fully offline.
 *
 * This is separate from [com.zakir.vestra.shared.cloud.CloudModelCatalog] — those need network.
 */
data class LocalModelEntry(
    val id: String,
    val displayName: String,
    val description: String,
    val capability: AiCapability,
    /** Matching [com.zakir.vestra.shared.domain.ModelPack.id] when downloadable. */
    val packId: String?,
    val engineTier: EngineTier? = null,
    val license: String,
    val approxSizeLabel: String,
    val openSource: Boolean = true,
    val offlineAfterInstall: Boolean = true,
    /** Ready to run in this app build once the pack is installed. */
    val runnable: Boolean,
    val testingNote: String,
)

object LocalModelCatalog {
    val entries: List<LocalModelEntry> = listOf(
        LocalModelEntry(
            id = "local-lite-tryon",
            displayName = "Lite try-on (ONNX)",
            description = "Open garment segmentation + human parsing compositor. Works on every Android 8+ phone.",
            capability = AiCapability.TRY_ON,
            packId = "lite-v1",
            engineTier = EngineTier.LITE,
            license = "Apache-2.0 / open ONNX graphs",
            approxSizeLabel = "~15–40 MB",
            runnable = true,
            testingNote = "Best for fast local testing. Set Try-on engine → Lite. Debug builds bundle lite-v1.",
        ),
        LocalModelEntry(
            id = "local-pro-int8",
            displayName = "Pro try-on INT8 (SD1.5)",
            description = "Quantized Stable Diffusion 1.5 + ControlNet depth — full diffusion try-on on device. Pixel 9 optimized.",
            capability = AiCapability.TRY_ON,
            packId = "pro-v2-int8",
            engineTier = EngineTier.PRO,
            license = "CreativeML OpenRAIL-M (SD1.5)",
            approxSizeLabel = "~2 GB",
            runnable = true,
            testingNote = "Requires lite-v1 installed first (human parsing). Flagship phones 8 GB+ RAM.",
        ),
        LocalModelEntry(
            id = "local-pro-fp16",
            displayName = "Pro try-on FP16 (SD1.5)",
            description = "Higher-fidelity FP16 diffusion pack for devices with more RAM/storage.",
            capability = AiCapability.TRY_ON,
            packId = "pro-v1",
            engineTier = EngineTier.PRO,
            license = "CreativeML OpenRAIL-M (SD1.5)",
            approxSizeLabel = "~4.3 GB",
            runnable = true,
            testingNote = "Use when INT8 quality is not enough. Same Pro engine.",
        ),
        LocalModelEntry(
            id = "local-studio-models",
            displayName = "Studio model gallery",
            description = "Open pose / ethnicity-tagged base model photos for casting shoots (no generation weights).",
            capability = AiCapability.TRY_ON,
            packId = "studio-models-v1",
            license = "App-bundled / pack license",
            approxSizeLabel = "~50–200 MB",
            runnable = true,
            testingNote = "Optional. Improves casting variety for local shoots.",
        ),
        LocalModelEntry(
            id = "local-image-gen-planned",
            displayName = "Local image gen (planned)",
            description = "On-device FLUX/SD Turbo-class pack for Create Studio without cloud.",
            capability = AiCapability.IMAGE_GEN,
            packId = null,
            license = "TBD (Apache / OpenRAIL)",
            approxSizeLabel = "~1–3 GB",
            runnable = false,
            testingNote = "Not in this build — use Cloud image models in Settings for now. Pack ID reserved: local-flux-schnell.",
        ),
        LocalModelEntry(
            id = "local-coder-planned",
            displayName = "Local coding LLM (planned)",
            description = "Small open coder (e.g. Qwen2.5-Coder 1.5B / Gemma 2B) via ExecuTorch / MediaPipe for offline Code Studio.",
            capability = AiCapability.CODE,
            packId = null,
            license = "Apache-2.0 (planned)",
            approxSizeLabel = "~1–2 GB",
            runnable = false,
            testingNote = "Not in this build — use Groq/HF free coding models. Pack ID reserved: local-coder-v1.",
        ),
        LocalModelEntry(
            id = "local-video-planned",
            displayName = "Local video (research)",
            description = "On-device short video is not practical on phones yet; use free LTX-Video HF Space.",
            capability = AiCapability.VIDEO,
            packId = null,
            license = "N/A",
            approxSizeLabel = "N/A",
            runnable = false,
            offlineAfterInstall = false,
            testingNote = "Cloud video only. Local packs not offered.",
        ),
        LocalModelEntry(
            id = "local-quality-birefnet",
            displayName = "BiRefNet matting",
            description = "Open bilateral reference net for cleaner garment / person mattes before try-on.",
            capability = AiCapability.TRY_ON,
            packId = "birefnet-v1",
            license = "MIT",
            approxSizeLabel = "~50–200 MB",
            runnable = false,
            testingNote = "Optional · ONNX export pending on HF — post-step wired but inactive until pack ships.",
        ),
        LocalModelEntry(
            id = "local-quality-realesrgan",
            displayName = "Real-ESRGAN upscale",
            description = "Open 2×/4× upscaler for listing-ready stills after try-on or Create.",
            capability = AiCapability.IMAGE_GEN,
            packId = "realesrgan-v1",
            license = "BSD-3-Clause",
            approxSizeLabel = "~20–70 MB",
            runnable = false,
            testingNote = "Optional · ONNX export pending on HF — auto-upscale when pack is installed and valid.",
        ),
        LocalModelEntry(
            id = "local-quality-gfpgan",
            displayName = "GFPGAN face restore (planned)",
            description = "Open face restoration for shopper selfies and creator casting stills.",
            capability = AiCapability.IMAGE_EDIT,
            packId = null,
            license = "Apache-2.0 (planned)",
            approxSizeLabel = "~100–350 MB",
            runnable = false,
            testingNote = "Quality pack reserved: gfpgan-v1. Optional post-step after diffusion.",
        ),
    )

    fun runnable(): List<LocalModelEntry> = entries.filter { it.runnable }

    fun forCapability(capability: AiCapability): List<LocalModelEntry> =
        entries.filter { it.capability == capability }
}
