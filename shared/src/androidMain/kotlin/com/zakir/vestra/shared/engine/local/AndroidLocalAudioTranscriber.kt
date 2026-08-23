package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

/**
 * Offline speech-to-text via Gemma 4 multimodal audio (Gallery Audio Scribe class).
 * Shares [LiteRtLmPacks.GEMMA4_CODE] weights — no separate download when Code pack is installed.
 */
class AndroidLocalAudioTranscriber(
    private val context: Context,
    private val packs: ModelPackManager,
    private val useGpu: () -> Boolean = { false },
) : LocalAudioTranscriber {

    override fun isReady(): Boolean = resolveModel() != null

    override fun transcribe(audioPath: String, prompt: String): LocalTranscribeResult {
        val resolved = resolveModel()
            ?: return LocalTranscribeResult.Unavailable(
                "Download ${LiteRtLmPacks.GEMMA4_CODE} from Model packs for offline transcription.",
            )
        val (packId, modelPath) = resolved
        packs.markPackInUse(packId)
        return try {
            LiteRtLmEngine(
                context = context,
                modelPath = modelPath,
                useGpu = useGpu(),
                visionEnabled = false,
                audioEnabled = true,
            ).use { engine ->
                engine.initialize()
                when (val result = engine.transcribeAudio(audioPath, prompt)) {
                    is com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult.Ok ->
                        LocalTranscribeResult.Ok(result.text)
                    is com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult.Unavailable ->
                        LocalTranscribeResult.Unavailable(result.reason)
                }
            }
        } catch (err: Throwable) {
            LocalTranscribeResult.Unavailable(
                err.message?.take(200) ?: "Transcription failed.",
            )
        } finally {
            packs.markPackIdle(packId)
        }
    }

    private fun resolveModel(): Pair<String, String>? {
        val path = LiteRtLmPackResolver.modelPath(
            packs,
            LiteRtLmPacks.AUDIO_SCRIBE,
            LiteRtLmPacks.GEMMA4_FILE,
            LiteRtLmPacks.GEMMA4_CODE,
        ) ?: return null
        val file = File(path.second)
        if (file.length() < LiteRtLmPackLimits.MIN_GEMMA4_BYTES) return null
        return path
    }
}
