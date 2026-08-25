package com.zakir.vestra.shared.audio

sealed class PitchMatchResult {
    data class Matched(val sourceHz: Float, val targetHz: Float, val semitones: Int) : PitchMatchResult()
    data class Unavailable(val reason: String) : PitchMatchResult()
}

/**
 * "Match voice" flow: detect the fundamental frequency of a recorded sample and a target
 * reference clip, then compute the semitone shift that would move the recording's pitch onto
 * the target's — ready to hand straight to [VoiceKnobs.pitchSemitones].
 */
object PitchMatcher {
    fun match(sourceSamples: ShortArray, targetSamples: ShortArray, sampleRate: Int): PitchMatchResult {
        val sourceHz = PitchDetector.detectPitchHz(sourceSamples, sampleRate)
            ?: return PitchMatchResult.Unavailable("Couldn't detect a clear pitch in the recorded clip.")
        val targetHz = PitchDetector.detectPitchHz(targetSamples, sampleRate)
            ?: return PitchMatchResult.Unavailable("Couldn't detect a clear pitch in the target clip.")
        val semitones = PitchDetector.semitoneDifferenceRounded(sourceHz, targetHz)
            .coerceIn(-12, 12)
        return PitchMatchResult.Matched(sourceHz, targetHz, semitones)
    }
}
