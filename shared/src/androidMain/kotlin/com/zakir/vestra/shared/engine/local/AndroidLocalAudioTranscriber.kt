package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

/**
 * Offline speech-to-text via LiteRT-LM audio model (Gallery Audio Scribe class).
 */
class AndroidLocalAudioTranscriber(
    private val context: Context,
    private val packs: ModelPackManager,
    private val packId: String = LiteRtLmPacks.AUDIO_SCRIBE,
    private val useGpu: () -> Boolean = { false },
) : LocalAudioTranscriber {

    override fun isReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dir = packs.installedDir(packId) ?: return false
        val dirFile = File(dir)
        val cfg = LiteRtLmPackConfig.read(dirFile, LiteRtLmPacks.AUDIO_SCRIBE_FILE)
        val path = LiteRtLmPackConfig.modelPath(dirFile, cfg.primaryFile) ?: return false
        return File(path).length() >= LiteRtLmPackLimits.MIN_AUDIO_BYTES
    }

    override fun transcribe(audioPath: String, prompt: String): LocalTranscribeResult {
        if (!isReady()) {
            return LocalTranscribeResult.Unavailable(
                "Download $packId from Model packs for offline transcription.",
            )
        }
        val dir = packs.installedDir(packId)
            ?: return LocalTranscribeResult.Unavailable("Audio scribe pack directory missing.")
        val dirFile = File(dir)
        val cfg = LiteRtLmPackConfig.read(dirFile, LiteRtLmPacks.AUDIO_SCRIBE_FILE)
        val modelPath = LiteRtLmPackConfig.modelPath(dirFile, cfg.primaryFile)
            ?: return LocalTranscribeResult.Unavailable("Audio .litertlm missing — re-download pack.")
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
}
