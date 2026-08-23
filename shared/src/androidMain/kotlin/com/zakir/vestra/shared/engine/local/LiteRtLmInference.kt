package com.zakir.vestra.shared.engine.local

import android.content.Context
import com.google.ai.edge.litertlm.ToolSet
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngineCache
import com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

/** Shared warm-engine inference for all LiteRT-LM generators. */
internal object LiteRtLmInference {
    fun runText(
        context: Context,
        packs: ModelPackManager,
        packId: String,
        modelPath: String,
        useGpu: Boolean,
        visionEnabled: Boolean = false,
        audioEnabled: Boolean = false,
        tools: List<ToolSet> = emptyList(),
        prompt: String,
        system: String,
        mapOk: (LiteRtLmGenerateResult.Ok) -> Any,
        mapUnavailable: (String) -> Any,
    ): Any {
        packs.markPackInUse(packId)
        return try {
            val spec = LiteRtLmEngineCache.EngineSpec(
                modelPath = modelPath,
                useGpu = useGpu,
                visionEnabled = visionEnabled,
                audioEnabled = audioEnabled,
                toolsKey = LiteRtLmEngine.toolsKey(tools),
            )
            LiteRtLmEngineCache.withEngine(context, spec, tools) { engine ->
                when (val result = engine.generateText(prompt, system)) {
                    is LiteRtLmGenerateResult.Ok -> mapOk(result)
                    is LiteRtLmGenerateResult.Unavailable -> mapUnavailable(result.reason)
                }
            }
        } catch (err: Throwable) {
            mapUnavailable(err.message?.take(200) ?: "LiteRT-LM failed.")
        } finally {
            packs.markPackIdle(packId)
        }
    }

    fun runVision(
        context: Context,
        packs: ModelPackManager,
        packId: String,
        modelPath: String,
        useGpu: Boolean,
        visionEnabled: Boolean,
        imagePath: String,
        question: String,
        mapOk: (LiteRtLmGenerateResult.Ok) -> Any,
        mapUnavailable: (String) -> Any,
    ): Any {
        packs.markPackInUse(packId)
        return try {
            val spec = LiteRtLmEngineCache.EngineSpec(
                modelPath = modelPath,
                useGpu = useGpu,
                visionEnabled = visionEnabled,
                audioEnabled = false,
            )
            LiteRtLmEngineCache.withEngine(context, spec) { engine ->
                when (val result = engine.describeImage(imagePath, question)) {
                    is LiteRtLmGenerateResult.Ok -> mapOk(result)
                    is LiteRtLmGenerateResult.Unavailable -> mapUnavailable(result.reason)
                }
            }
        } catch (err: Throwable) {
            mapUnavailable(err.message?.take(200) ?: "Vision assist failed.")
        } finally {
            packs.markPackIdle(packId)
        }
    }

    fun runTranscribe(
        context: Context,
        packs: ModelPackManager,
        packId: String,
        modelPath: String,
        useGpu: Boolean,
        audioEnabled: Boolean,
        audioPath: String,
        prompt: String,
        mapOk: (LiteRtLmGenerateResult.Ok) -> Any,
        mapUnavailable: (String) -> Any,
    ): Any {
        packs.markPackInUse(packId)
        return try {
            val spec = LiteRtLmEngineCache.EngineSpec(
                modelPath = modelPath,
                useGpu = useGpu,
                visionEnabled = false,
                audioEnabled = audioEnabled,
            )
            LiteRtLmEngineCache.withEngine(context, spec) { engine ->
                when (val result = engine.transcribeAudio(audioPath, prompt)) {
                    is LiteRtLmGenerateResult.Ok -> mapOk(result)
                    is LiteRtLmGenerateResult.Unavailable -> mapUnavailable(result.reason)
                }
            }
        } catch (err: Throwable) {
            mapUnavailable(err.message?.take(200) ?: "Transcription failed.")
        } finally {
            packs.markPackIdle(packId)
        }
    }

    fun gemma4Ready(packs: ModelPackManager, packId: String): Boolean {
        if (!packs.isReady(packId)) return false
        val dir = packs.installedDir(packId) ?: return false
        val path = LiteRtLmPackConfig.modelPath(File(dir), LiteRtLmPacks.GEMMA4_FILE) ?: return false
        return File(path).length() >= LiteRtLmPackLimits.MIN_GEMMA4_BYTES
    }
}
