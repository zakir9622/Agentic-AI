package com.zakir.vestra.shared.local

import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.domain.EngineTier

/**
 * How a local entry appears in Create / Audio Studio model pickers.
 * Quality packs run as post-steps, not as txt2img / TTS generators.
 */
enum class LocalModelPickerRole {
    /** Offered in the studio ON-DEVICE list for this capability. */
    STUDIO_GENERATOR,
    /** Settings / packs only — never a Create Studio generator. */
    QUALITY_POST,
    /** Gallery / casting assets — try-on adjacent, not generation. */
    ASSET,
}

/**
 * Catalog of open-source models that run on-device (your phone as a local AI device).
 * Packs are downloaded once from Hugging Face and then work fully offline.
 *
 * This is separate from [com.zakir.vestra.shared.cloud.CloudModelCatalog] — those need network.
 *
 * Honesty: full offline Image / Video / TTS / Code generation needs published weights.
 * Until then, entries stay `runnable = false` with clear scaffold / coming-soon labels.
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
    val pickerRole: LocalModelPickerRole = LocalModelPickerRole.STUDIO_GENERATOR,
)

object LocalModelCatalog {
    val entries: List<LocalModelEntry> = listOf(
        LocalModelEntry(
            id = "local-lite-tryon",
            displayName = "Fast try-on (ONNX)",
            description = "Open garment segmentation + human parsing compositor. Works on Android 15+ (app minSdk).",
            capability = AiCapability.TRY_ON,
            packId = "lite-v1",
            engineTier = EngineTier.LITE,
            license = "Apache-2.0 / open ONNX graphs",
            approxSizeLabel = "~15–40 MB",
            runnable = true,
            testingNote = "Download lite-v1 from Settings → Model packs (~68 MB). Required for Lite and Pro try-on.",
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
            testingNote = "Preferred Pro pack on HF manifest. Download lite-v1 + pro-v1 for full Pro try-on.",
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
            runnable = false,
            testingNote = "Export ready; HF manifest upload pending — not selectable for download until hosted. Prefer pro-v1.",
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
            pickerRole = LocalModelPickerRole.ASSET,
        ),
        LocalModelEntry(
            id = "local-sdturbo-v1",
            displayName = "Local image gen (SD-Turbo)",
            description = "On-device SD-Turbo / LCM via ORT — sampler wired; needs published ONNX pack.",
            capability = AiCapability.IMAGE_GEN,
            packId = "local-sdturbo-v1",
            license = "OpenRAIL-M / Apache-2.0 (weights TBD)",
            approxSizeLabel = "~1–1.5 GB",
            runnable = false,
            testingNote = "Engine ready · download local-sdturbo-v1 when on Model packs (ml/export_image_gen_pack.py).",
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
            id = "local-gemma-planned",
            displayName = "Gemma 3 1B on-device (planned)",
            description = "Google Gemma 3 1B via LiteRT-LM / MediaPipe LLM Inference — offline chat and code assist.",
            capability = AiCapability.CODE,
            packId = null,
            license = "Gemma Terms of Use",
            approxSizeLabel = "~1–2 GB (.litertlm INT4)",
            runnable = false,
            testingNote = "Feasible on Pixel 8+ / 8 GB RAM phones via LiteRT-LM. Not wired in this build — catalog placeholder only.",
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
            testingNote = "No local video weights — cloud LTX / Wan2 only.",
        ),
        LocalModelEntry(
            id = "local-tts-system",
            displayName = "Device TTS (system)",
            description = "Offline speak via Android Text-to-speech (Google / OEM voices) + optional DSP knobs.",
            capability = AiCapability.AUDIO,
            packId = null,
            license = "Device TTS engine",
            approxSizeLabel = "0 (built-in)",
            runnable = true,
            testingNote = "Ready offline when a TTS language pack is installed on the phone.",
        ),
        LocalModelEntry(
            id = "local-tts-v1",
            displayName = "Local TTS neural (Kokoro / Piper)",
            description = "On-device neural TTS pack — ONNX / ExecuTorch (optional upgrade over system TTS).",
            capability = AiCapability.AUDIO,
            packId = "local-tts-v1",
            license = "Apache-2.0 (planned)",
            approxSizeLabel = "~80–300 MB",
            runnable = false,
            testingNote = "Scaffold — system TTS works today; neural pack when published.",
        ),
        LocalModelEntry(
            id = "local-voice-changer",
            displayName = "Local voice changer (DSP)",
            description = "Offline pitch / speed / formant / warmth / clarity knobs — no neural pack required.",
            capability = AiCapability.AUDIO,
            packId = null,
            license = "App DSP",
            approxSizeLabel = "0 (built-in)",
            runnable = true,
            testingNote = "Record with the mic or use device/cloud TTS, then apply knobs on-device.",
        ),
        LocalModelEntry(
            id = "local-quality-birefnet",
            displayName = "BiRefNet matting",
            description = "Open bilateral reference net for cleaner garment / person mattes before try-on.",
            capability = AiCapability.TRY_ON,
            packId = "birefnet-v1",
            license = "MIT",
            approxSizeLabel = "~224 MB",
            runnable = true,
            testingNote = "Optional · download birefnet-v1 from Model packs — post-step activates when installed.",
            pickerRole = LocalModelPickerRole.QUALITY_POST,
        ),
        LocalModelEntry(
            id = "local-quality-realesrgan",
            displayName = "Real-ESRGAN upscale",
            description = "Open 2×/4× upscaler for listing-ready stills after try-on or Create.",
            capability = AiCapability.TRY_ON,
            packId = "realesrgan-v1",
            license = "BSD-3-Clause",
            approxSizeLabel = "~5 MB",
            runnable = true,
            testingNote = "Optional quality pack — not an Image Create generator. Auto-upscale when installed.",
            pickerRole = LocalModelPickerRole.QUALITY_POST,
        ),
        LocalModelEntry(
            id = "local-quality-gfpgan",
            displayName = "GFPGAN face restore (planned)",
            description = "Open face restoration for shopper selfies and creator casting stills.",
            capability = AiCapability.TRY_ON,
            packId = null,
            license = "Apache-2.0 (planned)",
            approxSizeLabel = "~100–350 MB",
            runnable = false,
            testingNote = "Quality pack reserved: gfpgan-v1. Optional post-step after diffusion — not Image Edit.",
            pickerRole = LocalModelPickerRole.QUALITY_POST,
        ),
    )

    fun runnable(): List<LocalModelEntry> = entries.filter { it.runnable }

    /** All catalog rows tagged with [capability] (Settings / Usage). */
    fun forCapability(capability: AiCapability): List<LocalModelEntry> =
        entries.filter { it.capability == capability }

    /**
     * Create / Audio Studio ON-DEVICE list — generators and scaffolds only.
     * Excludes quality upscalers / matting packs that must not appear as Image models.
     */
    fun forStudioPicker(capability: AiCapability): List<LocalModelEntry> =
        forCapability(capability).filter { it.pickerRole == LocalModelPickerRole.STUDIO_GENERATOR }

    /** Honest short status for picker rows. */
    fun studioStatusLabel(entry: LocalModelEntry, packReady: Boolean): String = when {
        entry.id == "local-sdturbo-v1" && packReady -> "Ready offline"
        entry.id == "local-sdturbo-v1" && !packReady ->
            "Engine ready · pack weights not on device"
        entry.runnable && (entry.packId == null || packReady) -> "Ready offline"
        !entry.runnable && entry.packId != null -> "Scaffold · weights not published"
        !entry.runnable -> "Coming soon · no on-device weights yet"
        else -> "Download in Settings"
    }

    /** Green-dot readiness for studio ON-DEVICE rows (may differ from catalog [LocalModelEntry.runnable]). */
    fun studioEntryReady(entry: LocalModelEntry, packReady: Boolean): Boolean = when {
        entry.id == "local-sdturbo-v1" -> packReady
        entry.runnable && (entry.packId == null || packReady) -> true
        else -> false
    }
}
