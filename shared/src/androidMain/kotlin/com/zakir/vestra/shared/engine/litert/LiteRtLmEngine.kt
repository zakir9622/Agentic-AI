package com.zakir.vestra.shared.engine.litert

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import com.google.ai.edge.litertlm.ToolSet
import java.io.File

/**
 * Thin wrapper around LiteRT-LM [Engine] for Code / vision / audio on Android.
 * Always construct and [initialize] on a background thread ([Dispatchers.Default]).
 */
class LiteRtLmEngine(
    private val context: Context,
    private val modelPath: String,
    private val useGpu: Boolean,
    private val visionEnabled: Boolean = false,
    private val audioEnabled: Boolean = false,
    private val tools: List<ToolSet> = emptyList(),
) : AutoCloseable {

    private var engine: Engine? = null
    private var loadMs: Long = 0L

    fun initialize() {
        val started = System.currentTimeMillis()
        val backend = if (useGpu) Backend.GPU() else Backend.CPU()
        val visionBackend = if (visionEnabled) backend else null
        val audioBackend = if (audioEnabled) Backend.CPU() else null
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = context.cacheDir.absolutePath,
            visionBackend = visionBackend,
            audioBackend = audioBackend,
        )
        val eng = Engine(config)
        eng.initialize()
        engine = eng
        loadMs = System.currentTimeMillis() - started
        Log.i(TAG, "Cold load ${loadMs}ms · ${File(modelPath).name} · ${backendLabel(useGpu)}")
    }

    fun coldLoadMs(): Long = loadMs

    fun generateText(
        prompt: String,
        system: String,
        temperature: Float = 0.2f,
        maxTokens: Int = 1024,
    ): LiteRtLmGenerateResult {
        val eng = engine ?: return LiteRtLmGenerateResult.Unavailable("LiteRT-LM engine not initialized.")
        LiteRtLmEngineCache.enterInference()
        return try {
            val convConfig = ConversationConfig(
                systemInstruction = if (system.isBlank()) null else Contents.of(system.trim()),
                samplerConfig = SamplerConfig(
                    temperature = temperature.toDouble(),
                    topK = 40,
                    topP = 0.95,
                ),
                tools = tools.map { tool(it) },
            )
            eng.createConversation(convConfig).use { conversation ->
                val response = conversation.sendMessage(prompt.trim())
                val text = response.toString().trim()
                if (text.isBlank()) {
                    LiteRtLmGenerateResult.Unavailable("Model returned empty text.")
                } else {
                    LiteRtLmGenerateResult.Ok(
                        text = text,
                        tokensIn = prompt.length / 4,
                        tokensOut = text.length / 4,
                    )
                }
            }
        } catch (err: Throwable) {
            LiteRtLmGenerateResult.Unavailable(
                err.message?.take(200) ?: "LiteRT-LM generation failed.",
            )
        } finally {
            LiteRtLmEngineCache.leaveInference()
            LiteRtLmEngineCache.drainPendingClose { /* no-op — engines are per-job */ }
        }
    }

    fun describeImage(imagePath: String, question: String): LiteRtLmGenerateResult {
        val eng = engine ?: return LiteRtLmGenerateResult.Unavailable("LiteRT-LM engine not initialized.")
        if (!visionEnabled) {
            return LiteRtLmGenerateResult.Unavailable("Vision backend not enabled for this model.")
        }
        val image = File(imagePath)
        if (!image.isFile) {
            return LiteRtLmGenerateResult.Unavailable("Image file missing.")
        }
        LiteRtLmEngineCache.enterInference()
        return try {
            eng.createConversation().use { conversation ->
                val contents = Contents.of(
                    Content.ImageFile(image.absolutePath),
                    Content.Text(question.trim()),
                )
                val response = conversation.sendMessage(contents)
                val text = response.toString().trim()
                if (text.isBlank()) {
                    LiteRtLmGenerateResult.Unavailable("Vision model returned empty text.")
                } else {
                    LiteRtLmGenerateResult.Ok(text = text, tokensIn = 0, tokensOut = text.length / 4)
                }
            }
        } catch (err: Throwable) {
            LiteRtLmGenerateResult.Unavailable(
                err.message?.take(200) ?: "Vision assist failed.",
            )
        } finally {
            LiteRtLmEngineCache.leaveInference()
        }
    }

    fun transcribeAudio(audioPath: String, prompt: String = "Transcribe this audio accurately."): LiteRtLmGenerateResult {
        val eng = engine ?: return LiteRtLmGenerateResult.Unavailable("LiteRT-LM engine not initialized.")
        if (!audioEnabled) {
            return LiteRtLmGenerateResult.Unavailable("Audio backend not enabled for this model.")
        }
        val audio = File(audioPath)
        if (!audio.isFile) {
            return LiteRtLmGenerateResult.Unavailable("Audio file missing.")
        }
        LiteRtLmEngineCache.enterInference()
        return try {
            eng.createConversation().use { conversation ->
                val contents = Contents.of(
                    Content.AudioFile(audio.absolutePath),
                    Content.Text(prompt),
                )
                val response = conversation.sendMessage(contents)
                val text = response.toString().trim()
                if (text.isBlank()) {
                    LiteRtLmGenerateResult.Unavailable("Transcription returned empty text.")
                } else {
                    LiteRtLmGenerateResult.Ok(text = text, tokensIn = 0, tokensOut = text.length / 4)
                }
            }
        } catch (err: Throwable) {
            LiteRtLmGenerateResult.Unavailable(
                err.message?.take(200) ?: "Audio transcription failed.",
            )
        } finally {
            LiteRtLmEngineCache.leaveInference()
        }
    }

    override fun close() {
        LiteRtLmEngineCache.requestClose(modelPath)
        if (LiteRtLmEngineCache.hasActiveInference()) return
        runCatching { engine?.close() }
        engine = null
    }

    companion object {
        private const val TAG = "LookbookLiteRtLm"

        fun backendLabel(useGpu: Boolean): String = if (useGpu) "GPU" else "CPU"
    }
}

sealed class LiteRtLmGenerateResult {
    data class Ok(val text: String, val tokensIn: Int, val tokensOut: Int) : LiteRtLmGenerateResult()
    data class Unavailable(val reason: String) : LiteRtLmGenerateResult()
}
