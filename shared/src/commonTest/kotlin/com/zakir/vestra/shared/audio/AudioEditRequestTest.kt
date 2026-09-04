package com.zakir.vestra.shared.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The interesting half is the refusals. Applying a pitch shift is easy; the failure that matters
 * is accepting "remove the background noise", doing nothing, and handing back a clip that sounds
 * identical — which a user reads as the feature being broken rather than absent.
 */
class AudioEditRequestTest {

    @Test
    fun deeperLowersPitch() {
        val r = AudioEditRequest.resolve("make it deeper")
        assertTrue(r.knobs.pitchSemitones < 0f, "expected a downward shift: ${r.knobs}")
        assertTrue(r.changedAnything)
    }

    @Test
    fun higherRaisesPitch() {
        assertTrue(AudioEditRequest.resolve("make it higher").knobs.pitchSemitones > 0f)
    }

    @Test
    fun speedInstructionsMoveInOppositeDirections() {
        assertTrue(AudioEditRequest.resolve("speed up the clip").knobs.speed > 1f)
        assertTrue(AudioEditRequest.resolve("slow down the clip").knobs.speed < 1f)
    }

    @Test
    fun intensityWordsScaleTheChange() {
        val slight = AudioEditRequest.resolve("slightly deeper").knobs.pitchSemitones
        val plain = AudioEditRequest.resolve("deeper").knobs.pitchSemitones
        val strong = AudioEditRequest.resolve("much deeper").knobs.pitchSemitones
        // All negative; "much" should be furthest from zero and "slightly" nearest.
        assertTrue(strong < plain, "expected 'much deeper' ($strong) below 'deeper' ($plain)")
        assertTrue(plain < slight, "expected 'deeper' ($plain) below 'slightly deeper' ($slight)")
    }

    @Test
    fun editsCompoundOnTopOfCurrentKnobsRatherThanResetting() {
        val first = AudioEditRequest.resolve("deeper").knobs
        val second = AudioEditRequest.resolve("deeper", base = first).knobs
        assertTrue(
            second.pitchSemitones < first.pitchSemitones,
            "a second 'deeper' must go further, not start over: $first -> $second",
        )
    }

    @Test
    fun unsupportedEditsAreNamedRatherThanSilentlyIgnored() {
        for ((prompt, expected) in listOf(
            "remove the background noise" to "noise removal",
            "trim the first few seconds" to "trimming",
            "normalize the volume" to "loudness normalisation",
            "add reverb" to "reverb",
        )) {
            val r = AudioEditRequest.resolve(prompt)
            assertTrue(
                r.unsupported.contains(expected),
                "expected '$expected' reported unsupported for '$prompt', got ${r.unsupported}",
            )
        }
    }

    @Test
    fun anEntirelyUnsupportedEditSaysWhatItCanDoInstead() {
        val r = AudioEditRequest.resolve("remove the background noise")
        assertEquals(false, r.changedAnything, "nothing applicable should have been applied")
        val msg = AudioEditRequest.shortfallMessage(r)
        assertNotNull(msg)
        assertTrue(msg.contains("noise removal"), msg)
        assertTrue(msg.contains("pitch"), "the message should name what is possible: $msg")
    }

    @Test
    fun aPartlySupportedEditAppliesWhatItCanAndSaysWhatItCouldNot() {
        val r = AudioEditRequest.resolve("make it deeper and remove the noise")
        assertTrue(r.changedAnything, "the pitch half should still be applied")
        val msg = AudioEditRequest.shortfallMessage(r)
        assertNotNull(msg)
        assertTrue(msg.contains("noise removal"), msg)
    }

    @Test
    fun aFullySupportedEditReportsNoShortfall() {
        val r = AudioEditRequest.resolve("make it deeper and warmer")
        assertTrue(r.changedAnything)
        assertTrue(r.unsupported.isEmpty(), "unexpected unsupported: ${r.unsupported}")
        assertNull(AudioEditRequest.shortfallMessage(r))
    }

    @Test
    fun resolvedKnobsAreAlwaysWithinRange() {
        // Repeated compounding must not drive the DSP outside what it accepts.
        var knobs = VoiceKnobs.Default
        repeat(20) { knobs = AudioEditRequest.resolve("much deeper and much faster", knobs).knobs }
        assertEquals(knobs, knobs.sanitized(), "resolve must return sanitized knobs: $knobs")
    }
}
