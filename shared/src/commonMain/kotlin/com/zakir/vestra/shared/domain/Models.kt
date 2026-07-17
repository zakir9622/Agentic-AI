package com.zakir.vestra.shared.domain

import kotlinx.serialization.Serializable

/** Which generation engine executes a try-on. Selected in Settings; AUTO routes per device. */
enum class EngineTier {
    AUTO,

    /** On-device segmentation + warp + harmonization pipeline. Works on all supported devices, fully offline. */
    LITE,

    /** On-device quantized try-on diffusion. Flagship NPUs only, fully offline. */
    PRO,

    /** Server-side diffusion via Supabase + Replicate. Requires network and explicit opt-in. */
    CLOUD,
}

/** Where the person in the output image comes from. */
sealed interface PersonSource {
    /** A photo the user picked or captured. Requires the likeness-consent acknowledgement. */
    data class UserPhoto(val uri: String) : PersonSource

    /** A base model from the bundled/downloadable AI-model gallery. */
    data class AiModel(val modelId: String) : PersonSource
}

@Serializable
data class GarmentImage(
    val uri: String,
    /** Best-effort category hint used to pick warp regions; null = auto-detect. */
    val category: GarmentCategory? = null,
)

@Serializable
enum class GarmentCategory {
    UPPER_BODY,
    LOWER_BODY,
    DRESS,

    /** Abaya, jilbab, kaftan, burqa — coverage extends over the arms and to the ankles. */
    FULL_COVERAGE,

    /** Hijab, scarf, dupatta — worn over hair/head instead of the body. */
    HEADSCARF,
}

data class TryOnRequest(
    val garment: GarmentImage,
    val person: PersonSource,
    val tier: EngineTier,
    val backdrop: Backdrop = Backdrop.STUDIO_WHITE,
    val seed: Long? = null,
)

/**
 * The scene behind the model in a shot. [ORIGINAL] keeps the person photo's own
 * background; every other value segments the subject and re-composites it over a
 * generated backdrop — the "studio" part of the photoshoot.
 */
@Serializable
enum class Backdrop(val displayName: String) {
    ORIGINAL("As shot"),
    STUDIO_WHITE("Studio white"),
    EDITORIAL("Editorial"),
    GOLDEN_HOUR("Golden hour"),
    RUNWAY("Runway"),
    MONOCHROME("Monochrome"),
}

/**
 * A photoshoot: the same garment rendered as a set of shots (one per person
 * source — different poses of the AI model, or a single user photo).
 */
data class ShootPlan(
    val garment: GarmentImage,
    val shots: List<PersonSource>,
    val tier: EngineTier,
) {
    init {
        require(shots.isNotEmpty()) { "A shoot needs at least one shot" }
    }
}

/** Progress of a multi-shot photoshoot; [inner] is the active shot's engine state. */
data class ShootState(
    val shotIndex: Int,
    val totalShots: Int,
    val inner: GenerationState,
    val completed: List<TryOnResult>,
) {
    val isFinished: Boolean get() = completed.size == totalShots
}

data class TryOnResult(
    /** Absolute path of the generated image on local storage. */
    val imagePath: String,
    /** Tier that actually ran (relevant when the request used AUTO). */
    val executedTier: EngineTier,
    val durationMillis: Long,
    /** True when the output carries the AI-generated watermark + metadata tag. */
    val watermarked: Boolean,
)

/** Progress states surfaced to the cinematic generation screen. */
sealed interface GenerationState {
    data object Idle : GenerationState
    data class Preparing(val message: String) : GenerationState

    /** [fraction] in 0..1; [stage] is a UI-facing label like "Fitting garment". */
    data class Running(val fraction: Float, val stage: String) : GenerationState
    data class Complete(val result: TryOnResult) : GenerationState
    data class Failed(val error: TryOnError) : GenerationState
}

sealed interface TryOnError {
    data object ModelPackMissing : TryOnError
    data object DeviceNotCapable : TryOnError
    data object NetworkUnavailable : TryOnError
    data class SafetyBlocked(val reason: String) : TryOnError
    data class Internal(val message: String) : TryOnError
}
