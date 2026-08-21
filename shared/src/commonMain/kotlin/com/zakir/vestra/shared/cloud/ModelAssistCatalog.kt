package com.zakir.vestra.shared.cloud

/**
 * Per-capability assist toggles that free models can honor via prompt / system
 * rewriting (HF Spaces rarely expose raw sampler knobs on Gradio predict).
 */
data class GenerativeAssists(
    /** Code: complete lawful tasks instead of soft-refusing. */
    val pragmatic: Boolean = true,
    /** Code: slightly higher temperature / exploratory answers. */
    val creative: Boolean = false,
    /** Image/video: lookbook + modest-fashion framing. */
    val fashionContext: Boolean = true,
    /** Image/video: sharpness / lighting clauses. */
    val detailBoost: Boolean = true,
    /**
     * Image/video: soften safety false-positives on clothing prompts by
     * reframing as fashion catalog / editorial photography (not NSFW).
     */
    val bypassFilter: Boolean = true,
    /** Image/video: append common quality negatives (blur, artifacts). */
    val qualityGuard: Boolean = true,
)

object ModelAssistCatalog {
    fun forCapability(capability: AiCapability): List<AssistToggle> = when (capability) {
        AiCapability.CODE -> listOf(
            AssistToggle("pragmatic", "Pragmatic (fewer refusals)", "Ask the model to complete lawful coding tasks instead of declining."),
            AssistToggle("creative", "Creative", "Higher temperature for more exploratory code answers."),
        )
        AiCapability.IMAGE_GEN, AiCapability.IMAGE_EDIT, AiCapability.VIDEO -> listOf(
            AssistToggle("bypass", "Bypass filter assist", "Reframe as fashion/editorial so clothing prompts trip fewer false blocks."),
            AssistToggle("fashion", "Fashion context", "Modest wear lookbook framing."),
            AssistToggle("detail", "Detail boost", "Sharpness and lighting clauses."),
            AssistToggle("quality", "Quality guard", "Avoid blur / artifacts language."),
        )
        AiCapability.TRY_ON -> emptyList() // garment+person inputs, not free-text assist
    }
}

data class AssistToggle(
    val id: String,
    val label: String,
    val description: String,
)
