package com.zakir.vestra.shared.engine.local

import com.zakir.vestra.shared.audio.VoiceKnobs
import com.zakir.vestra.shared.audio.VoicePersona

/**
 * On-device TTS / voice pack (Audio Studio).
 * Ready only when `local-tts-v1` graphs ship and [RUNNER_WIRED] is flipped.
 */
interface LocalAudioGenerator {
    fun isReady(): Boolean
    fun generate(
        text: String,
        persona: VoicePersona,
        knobs: VoiceKnobs,
        seed: Long? = null,
    ): LocalAudioResult
}

sealed class LocalAudioResult {
    data class Ok(val audioPath: String) : LocalAudioResult()
    data class Unavailable(val reason: String) : LocalAudioResult()
}

object UnimplementedLocalAudioGenerator : LocalAudioGenerator {
    override fun isReady(): Boolean = false
    override fun generate(
        text: String,
        persona: VoicePersona,
        knobs: VoiceKnobs,
        seed: Long?,
    ): LocalAudioResult = LocalAudioResult.Unavailable(
        "On-device TTS pack not wired — use cloud Audio Studio, or install local-tts-v1 when published.",
    )
}

/**
 * Offline voice changer — applies [VoiceKnobs] to an existing clip.
 * Basic DSP can run without a neural pack; neural VC flips [NEURAL_WIRED].
 */
interface LocalVoiceChanger {
    fun isReady(): Boolean
    fun transform(inputPath: String, knobs: VoiceKnobs): LocalAudioResult
}

object UnimplementedLocalVoiceChanger : LocalVoiceChanger {
    override fun isReady(): Boolean = false
    override fun transform(inputPath: String, knobs: VoiceKnobs): LocalAudioResult =
        LocalAudioResult.Unavailable("Local voice changer not available on this platform.")
}

/** Flip when Kokoro / Piper / ONNX TTS sampling is productized. */
object LocalAudioFlags {
    const val TTS_RUNNER_WIRED: Boolean = false
    const val NEURAL_VC_WIRED: Boolean = false
}
