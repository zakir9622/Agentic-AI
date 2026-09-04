package com.zakir.vestra.shared.audio

/**
 * Turns an edit instruction into knob changes the on-device DSP can actually apply.
 *
 * The honest scope, stated up front: [VoiceKnobs] shifts pitch, rate, formant and spectral
 * balance on an existing waveform. That covers "make it deeper", "speed it up", "warmer",
 * "brighter". It does **not** cover trimming, denoising, normalising or fading, because there is
 * no such processing in the app to route to. Rather than accept those prompts and silently
 * return the clip unchanged — which reads as a broken feature — [unsupported] names them so the
 * caller can say what it cannot do.
 */
object AudioEditRequest {

    data class Resolved(
        val knobs: VoiceKnobs,
        /** Human-readable list of asks in the prompt that nothing here can perform. */
        val unsupported: List<String>,
    ) {
        val changedAnything: Boolean get() = knobs != VoiceKnobs.Default
    }

    /**
     * Edits that need real signal processing the app does not have.
     *
     * Keys are matched against an article-stripped prompt (see [normalize]): "remove the noise"
     * and "remove noise" are the same instruction, and a detector that only caught the second
     * would let the first through to a silent no-op — which is the exact failure this map exists
     * to prevent.
     */
    private val UNSUPPORTED_ASKS = mapOf(
        "denoise" to "noise removal",
        "remove noise" to "noise removal",
        "background noise" to "noise removal",
        "clean up audio" to "noise removal",
        "trim" to "trimming",
        "remove silence" to "silence removal",
        "fade" to "fades",
        "normalize" to "loudness normalisation",
        "normalise" to "loudness normalisation",
        "remaster" to "remastering",
        "echo" to "echo",
        "reverb" to "reverb",
    )

    /**
     * Drop articles and collapse whitespace so phrasing differences do not change the outcome.
     * "remove the background noise" and "remove background noise" must resolve identically.
     */
    private fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("\\b(the|a|an)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Read an instruction into knob deltas.
     *
     * [base] is the current knob state so an edit refines rather than resets: "a bit deeper"
     * after an earlier edit should compound, not start over.
     */
    fun resolve(prompt: String, base: VoiceKnobs = VoiceKnobs.Default): Resolved {
        val p = prompt.lowercase()
        var knobs = base

        val strong = p.contains("much ") || p.contains("a lot") || p.contains("very ")
        val slight = p.contains("slightly") || p.contains("a bit") || p.contains("a little")
        val scale = when {
            strong -> 2.0f
            slight -> 0.5f
            else -> 1.0f
        }

        if (p.contains("deeper") || p.contains("lower pitch") || p.contains("lower voice")) {
            knobs = knobs.copy(pitchSemitones = knobs.pitchSemitones - 3f * scale)
        }
        if (p.contains("higher") || p.contains("higher pitch")) {
            knobs = knobs.copy(pitchSemitones = knobs.pitchSemitones + 3f * scale)
        }
        if (p.contains("speed up") || p.contains("faster")) {
            knobs = knobs.copy(speed = knobs.speed + 0.25f * scale)
        }
        if (p.contains("slow down") || p.contains("slower")) {
            knobs = knobs.copy(speed = knobs.speed - 0.25f * scale)
        }
        if (p.contains("warmer") || p.contains("warmth")) {
            knobs = knobs.copy(warmth = knobs.warmth + 0.2f * scale)
        }
        if (p.contains("brighter") || p.contains("clearer") || p.contains("crisper")) {
            knobs = knobs.copy(clarity = knobs.clarity + 0.2f * scale)
        }
        if (p.contains("breathy") || p.contains("softer")) {
            knobs = knobs.copy(breathiness = knobs.breathiness + 0.3f * scale)
        }
        if (p.contains("raspy") || p.contains("gravelly") || p.contains("rougher")) {
            knobs = knobs.copy(raspyMidGain = knobs.raspyMidGain + 0.3f * scale)
        }

        val normalized = normalize(prompt)
        val unsupported = UNSUPPORTED_ASKS.entries
            .filter { normalized.contains(it.key) }
            .map { it.value }
            .distinct()

        return Resolved(knobs.sanitized(), unsupported)
    }

    /**
     * What to tell the user when part or all of an edit cannot be performed.
     * Null when everything asked for was applied.
     */
    fun shortfallMessage(resolved: Resolved): String? = when {
        resolved.unsupported.isEmpty() -> null
        !resolved.changedAnything ->
            "This build can't do ${resolved.unsupported.joinToString(" or ")} yet — " +
                "it can change pitch, speed, warmth and clarity."
        else ->
            "Applied what it could. Not supported yet: ${resolved.unsupported.joinToString(", ")}."
    }
}
