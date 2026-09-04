package com.zakir.vestra.shared.cloud

/**
 * What shape and finish an image generation should have when the user has not said.
 *
 * The app used to send a hardcoded 1024x1024 with no quality direction at all, so every
 * generation came back square and whatever the base model felt like — a portrait subject
 * cropped into a square frame, and none of the realism vocabulary that measurably improves
 * photographic output.
 *
 * The defaults here are portrait and photoreal. They are **defaults, not overrides**: a prompt
 * that asks for a wide shot gets a wide shot, and a prompt that asks for anime does not get
 * "ultra realistic" bolted onto it. Forcing those unconditionally would mean the app renders
 * something other than what the user typed, which is a worse failure than a square frame.
 *
 * On the word "8k": no free tier renders 8K. FLUX.1-schnell on a free ZeroGPU Space is budgeted
 * in the ~1 megapixel range; 7680x4320 is thirty times that and would exhaust the queue before
 * it produced anything. "8k" here is prompt vocabulary — it steers the model toward fine detail,
 * which is the part that actually reaches the picture — not a resolution request. [PORTRAIT]
 * spends the same pixel budget as the old square did, just shaped correctly.
 */
object ImageOutputStyle {

    /** width x height in pixels. */
    data class Spec(val width: Int, val height: Int, val styleSuffix: String?)

    /**
     * 832x1216 — a standard SDXL/FLUX aspect bucket at 1,011,712 px, within a rounding error of
     * the 1,048,576 px the old square spent. Portrait shape costs nothing here; models are
     * trained on these buckets, so an off-bucket size is likelier to degrade than to help.
     */
    val PORTRAIT = 832 to 1216
    val LANDSCAPE = 1216 to 832
    val SQUARE = 1024 to 1024

    const val PHOTOREAL_SUFFIX =
        "ultra realistic, photographic, high detail, 8k quality, sharp focus, natural lighting"

    /**
     * Words that mean "this should not look like a photograph". When any appears, the photoreal
     * suffix is withheld: appending "ultra realistic, photographic" to "flat vector logo" gives
     * the model two contradictory instructions and it resolves them unpredictably.
     */
    private val NON_PHOTOGRAPHIC = listOf(
        "anime", "manga", "cartoon", "comic", "illustration", "illustrated", "drawing", "sketch",
        "painting", "painted", "watercolor", "watercolour", "oil painting", "vector", "flat art",
        "logo", "icon", "pixel art", "low poly", "3d render", "render", "cgi", "clay",
        "line art", "doodle", "caricature", "abstract", "surreal", "cel shaded", "storyboard",
        "blueprint", "diagram", "chart", "poster art", "graffiti", "stained glass", "origami",
    )

    /** Words that mean the frame should be wider than it is tall. */
    private val LANDSCAPE_WORDS = listOf(
        "landscape", "wide shot", "wide angle", "widescreen", "panorama", "panoramic", "banner",
        "horizontal", "16:9", "cinematic shot", "establishing shot", "vista", "skyline",
        "header image", "cover image", "desktop wallpaper",
    )

    /** Words that mean a square frame is what's wanted. */
    private val SQUARE_WORDS = listOf(
        "square", "1:1", "album cover", "profile picture", "avatar", "thumbnail", "sticker",
        "instagram post",
    )

    /**
     * Choose the frame and finish for [prompt].
     *
     * [preferPhotoreal] is the app-level default; a caller can turn it off wholesale. Even when
     * it is on, an explicitly non-photographic prompt still wins — that is the point.
     */
    fun resolve(prompt: String, preferPhotoreal: Boolean = true): Spec {
        val p = prompt.lowercase()
        val (w, h) = when {
            LANDSCAPE_WORDS.any { p.contains(it) } -> LANDSCAPE
            SQUARE_WORDS.any { p.contains(it) } -> SQUARE
            else -> PORTRAIT
        }
        val suffix = when {
            !preferPhotoreal -> null
            NON_PHOTOGRAPHIC.any { p.contains(it) } -> null
            // Already asked for it themselves — repeating the vocabulary adds nothing and eats
            // prompt budget.
            p.contains("ultra realistic") || p.contains("photorealistic") -> null
            else -> PHOTOREAL_SUFFIX
        }
        return Spec(w, h, suffix)
    }

    /** [prompt] with the resolved finish appended, or unchanged when none applies. */
    fun styledPrompt(prompt: String, preferPhotoreal: Boolean = true): String {
        val suffix = resolve(prompt, preferPhotoreal).styleSuffix ?: return prompt
        return "${prompt.trimEnd().trimEnd('.')}. $suffix"
    }
}
