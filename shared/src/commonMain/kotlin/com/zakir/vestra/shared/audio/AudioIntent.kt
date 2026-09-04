package com.zakir.vestra.shared.audio

/**
 * What the user is actually asking the Audio studio to do.
 *
 * The studio had exactly one behaviour: read the prompt aloud. Ask it for "a lo-fi beat" and it
 * spoke the words "a lo-fi beat"; ask it to "change this to a deeper voice" and it read that
 * sentence out too. There was no path to anything else, because [AiCapability.AUDIO] mapped
 * one-to-one onto text-to-speech.
 *
 * Rather than split the capability enum — which would fragment one studio into five in the UI,
 * against the single-composer design the rest of the app follows — the studio stays one surface
 * and the *prompt* selects the task, the way it does for a chat assistant. This resolver is that
 * selection, kept as pure logic so it can be tested without a network or a device.
 *
 * The deliberate bias is toward [SPEAK]. It is the one task that always works, entirely offline,
 * so an ambiguous prompt should land there rather than on a cloud model that may queue for a
 * minute and fail. Misrouting "read this out" to a music model is a much worse outcome than
 * misrouting "something jazzy" to speech.
 */
enum class AudioIntent {
    /** Text to speech — the default, and the only task with a guaranteed offline path. */
    SPEAK,

    /** Prompt to music or sound effect. Needs a generative audio model. */
    MUSIC,

    /** Rewrite or generate song lyrics. Text in, text out — a language task, not an audio one. */
    LYRICS,

    /** Convert an attached recording to a different voice. Requires a reference clip. */
    VOICE_CONVERT,

    /** Alter an attached recording (trim, clean, effects) without changing who is speaking. */
    EDIT_AUDIO,
    ;

    /** True when this task cannot start without an attached audio clip. */
    val requiresReferenceAudio: Boolean get() = this == VOICE_CONVERT || this == EDIT_AUDIO

    companion object {

        private val MUSIC_WORDS = listOf(
            "song", "music", "musical", "beat", "melody", "instrumental", "track", "tune",
            "jingle", "soundtrack", "score", "riff", "chord", "bassline", "drum", "percussion",
            "lo-fi", "lofi", "ambient music", "sound effect", "sfx", "compose",
        )

        private val LYRICS_WORDS = listOf(
            "lyric", "lyrics", "verse", "chorus", "rewrite the words", "write the words",
            "songwriting", "rhyme scheme",
        )

        private val VOICE_CONVERT_WORDS = listOf(
            "change voice", "change the voice", "voice change", "voice changer", "sound like",
            "make me sound", "different voice", "convert voice", "voice conversion",
            "clone voice", "voice clone", "in the voice of", "as a woman", "as a man",
            "deeper voice", "higher voice",
        )

        private val EDIT_AUDIO_WORDS = listOf(
            "remove noise", "denoise", "clean up", "trim", "cut the", "fade", "normalize",
            "normalise", "louder", "quieter", "speed up", "slow down", "reverb", "echo",
            "remaster", "enhance the audio", "remove silence",
        )

        /** The literal prompt the composer sends for the dedicated voice-changer control. */
        const val VOICE_CHANGE_SENTINEL = "voice-change"

        /**
         * Resolve what to do from the prompt and whether a clip is attached.
         *
         * [hasReferenceAudio] is not a hint, it is a gate: a request to change a voice with
         * nothing to change is not a voice conversion, and routing it as one would produce an
         * error where speaking the text would have produced something. Tasks that need a clip
         * fall back to [SPEAK] when there isn't one.
         */
        fun resolve(prompt: String, hasReferenceAudio: Boolean): AudioIntent {
            val p = prompt.trim().lowercase()
            if (p == VOICE_CHANGE_SENTINEL) return VOICE_CONVERT
            if (p.isEmpty()) return SPEAK

            // Lyrics before music: "write lyrics for a rock song" contains "song" but the ask is
            // words, not audio. The narrower reading wins when both match.
            if (LYRICS_WORDS.any { p.contains(it) }) return LYRICS

            if (hasReferenceAudio) {
                if (VOICE_CONVERT_WORDS.any { p.contains(it) }) return VOICE_CONVERT
                if (EDIT_AUDIO_WORDS.any { p.contains(it) }) return EDIT_AUDIO
            }

            if (MUSIC_WORDS.any { p.contains(it) }) return MUSIC

            return SPEAK
        }

        /**
         * What the user should be told when a task needs a clip and none is attached.
         * Null when [intent] can proceed.
         */
        fun missingReferenceMessage(intent: AudioIntent, hasReferenceAudio: Boolean): String? =
            when {
                !intent.requiresReferenceAudio || hasReferenceAudio -> null
                intent == VOICE_CONVERT ->
                    "Attach a recording first — voice conversion needs a clip to convert."
                else -> "Attach a recording first — there's nothing to edit yet."
            }
    }
}
