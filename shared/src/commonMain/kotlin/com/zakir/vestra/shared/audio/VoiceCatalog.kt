package com.zakir.vestra.shared.audio

/**
 * Named voice personas for Audio Studio — different persons / varieties.
 * Cloud TTS maps [cloudVoiceId] / [edgeVoiceLabel]; local packs map [localSpeakerId] when wired.
 */
data class VoicePersona(
    val id: String,
    val displayName: String,
    val description: String,
    val variety: VoiceVariety,
    /** Kokoro-style speaker id (e.g. af_heart). */
    val cloudVoiceId: String,
    /** Edge-TTS dropdown label as served by innoai Edge-TTS Space. */
    val edgeVoiceLabel: String,
    val localSpeakerId: String = id,
)

enum class VoiceVariety {
    FEMALE_WARM,
    FEMALE_BRIGHT,
    FEMALE_SOFT,
    MALE_BARITONE,
    MALE_TENOR,
    NEUTRAL,
    STORYTELLER,
    ANNOUNCER,
}

object VoiceCatalog {
    val personas: List<VoicePersona> = listOf(
        VoicePersona(
            id = "amina",
            displayName = "Amina",
            description = "Warm mezzo — modest fashion narration",
            variety = VoiceVariety.FEMALE_WARM,
            cloudVoiceId = "af_heart",
            edgeVoiceLabel = "en-US-AvaNeural - en-US (Female)",
        ),
        VoicePersona(
            id = "noor",
            displayName = "Noor",
            description = "Bright clear voice — product listings",
            variety = VoiceVariety.FEMALE_BRIGHT,
            cloudVoiceId = "af_bella",
            edgeVoiceLabel = "en-US-JennyNeural - en-US (Female)",
        ),
        VoicePersona(
            id = "layla",
            displayName = "Layla",
            description = "Soft alto — calm studio reads",
            variety = VoiceVariety.FEMALE_SOFT,
            cloudVoiceId = "af_sarah",
            edgeVoiceLabel = "en-GB-SoniaNeural - en-GB (Female)",
        ),
        VoicePersona(
            id = "yasir",
            displayName = "Yasir",
            description = "Warm baritone — brand films",
            variety = VoiceVariety.MALE_BARITONE,
            cloudVoiceId = "am_adam",
            edgeVoiceLabel = "en-US-GuyNeural - en-US (Male)",
        ),
        VoicePersona(
            id = "omar",
            displayName = "Omar",
            description = "Light tenor — short social clips",
            variety = VoiceVariety.MALE_TENOR,
            cloudVoiceId = "am_michael",
            edgeVoiceLabel = "en-US-BrianNeural - en-US (Male)",
        ),
        VoicePersona(
            id = "sam",
            displayName = "Sam",
            description = "Neutral mid — captions and demos",
            variety = VoiceVariety.NEUTRAL,
            cloudVoiceId = "af_nicole",
            edgeVoiceLabel = "en-US-AndrewNeural - en-US (Male)",
        ),
        VoicePersona(
            id = "rana",
            displayName = "Rana",
            description = "Storyteller — lookbook scripts",
            variety = VoiceVariety.STORYTELLER,
            cloudVoiceId = "bf_emma",
            edgeVoiceLabel = "en-GB-LibbyNeural - en-GB (Female)",
        ),
        VoicePersona(
            id = "kai",
            displayName = "Kai",
            description = "Announcer — launch and promo",
            variety = VoiceVariety.ANNOUNCER,
            cloudVoiceId = "am_fenrir",
            edgeVoiceLabel = "en-US-ChristopherNeural - en-US (Male)",
        ),
    )

    fun byId(id: String): VoicePersona =
        personas.firstOrNull { it.id == id } ?: personas.first()

    val defaultId: String = "amina"

    /** Section label for [variety] — used to group personas in the picker. */
    fun sectionFor(variety: VoiceVariety): String = when (variety) {
        VoiceVariety.FEMALE_WARM, VoiceVariety.FEMALE_BRIGHT, VoiceVariety.FEMALE_SOFT -> "Female"
        VoiceVariety.MALE_BARITONE, VoiceVariety.MALE_TENOR -> "Male"
        VoiceVariety.NEUTRAL, VoiceVariety.STORYTELLER, VoiceVariety.ANNOUNCER -> "Neutral & character"
    }

    /**
     * Personas grouped into Female / Male / Neutral & character sections, in that display order.
     * Every persona appears in exactly one group; a group with no personas is omitted rather than
     * shown empty.
     */
    fun groupedByVariety(): List<Pair<String, List<VoicePersona>>> {
        val order = listOf("Female", "Male", "Neutral & character")
        val grouped = personas.groupBy { sectionFor(it.variety) }
        return order.mapNotNull { section -> grouped[section]?.let { section to it } }
    }

    /**
     * Typical fundamental frequency (Hz) for [variety] — median speaking-voice pitch ranges from
     * voice-science literature, not a per-persona measurement (personas map to cloud TTS voice
     * ids, not a local sample this catalog has ever analyzed). Used as the "Match voice" target
     * when no second reference clip is available: match the user's recorded clip to a plausible
     * pitch for the selected persona's category, rather than to the exact unmeasurable timbre of
     * a name like "Amina".
     */
    fun typicalHzFor(variety: VoiceVariety): Float = when (variety) {
        VoiceVariety.FEMALE_WARM, VoiceVariety.FEMALE_BRIGHT, VoiceVariety.FEMALE_SOFT -> 220f
        VoiceVariety.MALE_BARITONE, VoiceVariety.MALE_TENOR -> 110f
        VoiceVariety.NEUTRAL, VoiceVariety.STORYTELLER, VoiceVariety.ANNOUNCER -> 165f
    }
}
