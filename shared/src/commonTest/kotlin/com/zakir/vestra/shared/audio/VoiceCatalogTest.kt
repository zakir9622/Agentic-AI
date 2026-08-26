package com.zakir.vestra.shared.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoiceCatalogTest {
    @Test
    fun personas_cover_varieties() {
        assertTrue(VoiceCatalog.personas.size >= 6)
        assertEquals("amina", VoiceCatalog.defaultId)
        assertEquals("Amina", VoiceCatalog.byId("amina").displayName)
        assertEquals(VoiceCatalog.personas.first(), VoiceCatalog.byId("missing"))
    }
}

class VoiceKnobsTest {
    @Test
    fun sanitized_clamps_metrics() {
        val knobs = VoiceKnobs(
            pitchSemitones = 40f,
            speed = 0.1f,
            formant = 3f,
            warmth = 2f,
            clarity = -1f,
        ).sanitized()
        assertEquals(12f, knobs.pitchSemitones)
        assertEquals(0.5f, knobs.speed)
        assertEquals(1.5f, knobs.formant)
        assertEquals(1f, knobs.warmth)
        assertEquals(0f, knobs.clarity)
    }

    @Test
    fun default_is_near_identity() {
        assertTrue(VoiceKnobs.Default.isIdentity)
        assertFalse(VoiceKnobs(pitchSemitones = 3f).isIdentity)
    }

    // Float.coerceIn alone does not filter NaN/Infinite (both x<min and x>max are false for
    // NaN, so it returns unchanged) — regression coverage for the fix that makes sanitized()
    // actually safe against a value that would otherwise crash a Compose Slider/Animatable
    // downstream ("current must not be NaN", see KnobSlider in AudioStudioPane.kt).
    @Test
    fun sanitized_never_returns_nan_for_any_field() {
        assertAllFinite(VoiceKnobs(pitchSemitones = Float.NaN).sanitized())
        assertAllFinite(VoiceKnobs(speed = Float.NaN).sanitized())
        assertAllFinite(VoiceKnobs(formant = Float.NaN).sanitized())
        assertAllFinite(VoiceKnobs(warmth = Float.NaN).sanitized())
        assertAllFinite(VoiceKnobs(clarity = Float.NaN).sanitized())
    }

    @Test
    fun sanitized_never_returns_infinite_for_any_field() {
        assertAllFinite(VoiceKnobs(pitchSemitones = Float.POSITIVE_INFINITY).sanitized())
        assertAllFinite(VoiceKnobs(speed = Float.NEGATIVE_INFINITY).sanitized())
        assertAllFinite(VoiceKnobs(formant = Float.POSITIVE_INFINITY).sanitized())
        assertAllFinite(VoiceKnobs(warmth = Float.NEGATIVE_INFINITY).sanitized())
        assertAllFinite(VoiceKnobs(clarity = Float.POSITIVE_INFINITY).sanitized())
    }

    private fun assertAllFinite(knobs: VoiceKnobs) {
        assertFalse(knobs.pitchSemitones.isNaN() || knobs.pitchSemitones.isInfinite(), "pitchSemitones: $knobs")
        assertFalse(knobs.speed.isNaN() || knobs.speed.isInfinite(), "speed: $knobs")
        assertFalse(knobs.formant.isNaN() || knobs.formant.isInfinite(), "formant: $knobs")
        assertFalse(knobs.warmth.isNaN() || knobs.warmth.isInfinite(), "warmth: $knobs")
        assertFalse(knobs.clarity.isNaN() || knobs.clarity.isInfinite(), "clarity: $knobs")
    }
}
