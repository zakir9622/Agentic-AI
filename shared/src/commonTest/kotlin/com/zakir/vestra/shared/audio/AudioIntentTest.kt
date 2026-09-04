package com.zakir.vestra.shared.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The routing decision is the whole feature, so the cases that matter are the ambiguous ones and
 * the ones where routing "correctly" would still produce a worse result than falling back.
 */
class AudioIntentTest {

    @Test
    fun plainTextIsSpoken() {
        assertEquals(AudioIntent.SPEAK, AudioIntent.resolve("read this out for me", hasReferenceAudio = false))
        assertEquals(AudioIntent.SPEAK, AudioIntent.resolve("hello there", hasReferenceAudio = false))
    }

    @Test
    fun anEmptyPromptIsSpokenRatherThanGuessed() {
        assertEquals(AudioIntent.SPEAK, AudioIntent.resolve("   ", hasReferenceAudio = false))
    }

    @Test
    fun musicRequestsDoNotGetReadAloud() {
        // The reported behaviour: asking for a song produced speech saying the words of the ask.
        for (prompt in listOf(
            "a lo-fi beat with rain",
            "compose an instrumental for a title screen",
            "make me a jingle for a coffee advert",
            "something jazzy with a walking bassline",
        )) {
            assertEquals(AudioIntent.MUSIC, AudioIntent.resolve(prompt, hasReferenceAudio = false), prompt)
        }
    }

    @Test
    fun lyricsBeatMusicWhenBothWordsAppear() {
        // "write lyrics for a rock song" contains "song", but the deliverable is words. The
        // narrower reading has to win or every lyric request becomes an instrumental.
        assertEquals(
            AudioIntent.LYRICS,
            AudioIntent.resolve("write lyrics for a rock song", hasReferenceAudio = false),
        )
        assertEquals(
            AudioIntent.LYRICS,
            AudioIntent.resolve("rewrite the chorus of this track", hasReferenceAudio = false),
        )
    }

    @Test
    fun voiceConversionNeedsAClipAndFallsBackWhenThereIsNone() {
        val prompt = "change the voice to something deeper"
        assertEquals(AudioIntent.VOICE_CONVERT, AudioIntent.resolve(prompt, hasReferenceAudio = true))
        // With nothing attached this is not a conversion. Routing it as one produces an error
        // where speaking the sentence would at least produce audio.
        assertEquals(AudioIntent.SPEAK, AudioIntent.resolve(prompt, hasReferenceAudio = false))
    }

    @Test
    fun audioEditingNeedsAClipToo() {
        val prompt = "remove noise and normalize it"
        assertEquals(AudioIntent.EDIT_AUDIO, AudioIntent.resolve(prompt, hasReferenceAudio = true))
        assertEquals(AudioIntent.SPEAK, AudioIntent.resolve(prompt, hasReferenceAudio = false))
    }

    @Test
    fun theComposersVoiceChangeSentinelAlwaysRoutesToConversion() {
        // The dedicated voice-changer control sends this exact string, not a sentence.
        assertEquals(
            AudioIntent.VOICE_CONVERT,
            AudioIntent.resolve(AudioIntent.VOICE_CHANGE_SENTINEL, hasReferenceAudio = true),
        )
    }

    @Test
    fun conversionIsPreferredOverEditingWhenBothCouldMatch() {
        // "make me sound deeper and remove noise": who is speaking is the bigger change, and an
        // edit pass can still follow it.
        assertEquals(
            AudioIntent.VOICE_CONVERT,
            AudioIntent.resolve("make me sound deeper and remove noise", hasReferenceAudio = true),
        )
    }

    @Test
    fun tasksThatNeedAClipSayWhichOnesTheyAre() {
        assertEquals(true, AudioIntent.VOICE_CONVERT.requiresReferenceAudio)
        assertEquals(true, AudioIntent.EDIT_AUDIO.requiresReferenceAudio)
        assertEquals(false, AudioIntent.SPEAK.requiresReferenceAudio)
        assertEquals(false, AudioIntent.MUSIC.requiresReferenceAudio)
        assertEquals(false, AudioIntent.LYRICS.requiresReferenceAudio)
    }

    @Test
    fun theMissingClipMessageNamesWhatIsMissing() {
        assertNotNull(AudioIntent.missingReferenceMessage(AudioIntent.VOICE_CONVERT, hasReferenceAudio = false))
        assertNotNull(AudioIntent.missingReferenceMessage(AudioIntent.EDIT_AUDIO, hasReferenceAudio = false))
        assertNull(AudioIntent.missingReferenceMessage(AudioIntent.VOICE_CONVERT, hasReferenceAudio = true))
        assertNull(AudioIntent.missingReferenceMessage(AudioIntent.SPEAK, hasReferenceAudio = false))
    }

    @Test
    fun anAttachedClipDoesNotTurnEveryPromptIntoAnEdit() {
        // Having a recording attached must not hijack an unrelated ask.
        assertEquals(AudioIntent.SPEAK, AudioIntent.resolve("read this out", hasReferenceAudio = true))
        assertEquals(AudioIntent.MUSIC, AudioIntent.resolve("a lo-fi beat", hasReferenceAudio = true))
    }
}
