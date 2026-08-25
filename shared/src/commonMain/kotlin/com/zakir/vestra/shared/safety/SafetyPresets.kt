package com.zakir.vestra.shared.safety

/**
 * Prompt-level safety presets — exact-match port of lookbookweb's `SAFETY_PRESETS`
 * (`src/lib/safety.ts`), adapted for on-device use. Unlike this app's existing post-process
 * privacy blur (B7, `FaceBlurProcessor`/`RegionBlurOverlay`, which detects and blurs faces
 * *after* generation), these presets inject guard wording *before* generation so the model
 * itself is steered away from unwanted content, matching lookbookweb's real behavior — not a
 * `delay()`-and-random-number kind of feature, an actual string appended to the real prompt that
 * reaches the real generation call.
 */
data class SafetyPreset(
    val id: String,
    val label: String,
    val blurb: String,
    /** Appended to the user's prompt before it reaches generation. Empty means no guard text. */
    val promptGuard: String,
    /** Show a confirmation before running when this preset is active. */
    val confirm: Boolean,
)

object SafetyPresets {
    val OFF = SafetyPreset(
        id = "off",
        label = "Off",
        blurb = "No extra guards applied",
        promptGuard = "",
        confirm = false,
    )
    val STANDARD = SafetyPreset(
        id = "standard",
        label = "Standard",
        blurb = "Family-friendly output only",
        promptGuard = "Keep the output safe for all audiences: no violence, no explicit content, " +
            "no hateful imagery.",
        confirm = false,
    )
    val BLUR_IDENTITIES = SafetyPreset(
        id = "blur-faces",
        label = "Blur identities",
        blurb = "Steers away from depicting identifiable real people",
        promptGuard = "Do not depict identifiable real people; keep faces generic and unrecognisable.",
        confirm = true,
    )
    val REDACT_DETAILS = SafetyPreset(
        id = "redact",
        label = "Redact details",
        blurb = "Steers away from readable text, logos, and personal details",
        promptGuard = "Avoid readable text, logos, licence plates or personal details.",
        confirm = true,
    )

    val ALL = listOf(OFF, STANDARD, BLUR_IDENTITIES, REDACT_DETAILS)

    const val DEFAULT_ID = "standard"

    fun byId(id: String): SafetyPreset = ALL.firstOrNull { it.id == id } ?: STANDARD

    /** Appends the preset's guard clause to [prompt] if it has one. Never throws. */
    fun applyGuard(prompt: String, presetId: String): String {
        val guard = byId(presetId).promptGuard
        if (guard.isBlank()) return prompt
        if (prompt.isBlank()) return guard
        return "$prompt\n\n$guard"
    }
}
