package com.zakir.vestra.shared.engine.litert

import android.content.Context
import com.google.ai.edge.litertlm.ToolSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reuses LiteRT-LM [Engine] instances per model spec — avoids cold-loading ~2.6 GB on every shot.
 * Single-flight init per key; never evict while inference is active (rc16 trim-safe pattern).
 */
object LiteRtLmEngineCache {
    private val inferenceDepth = AtomicInteger(0)
    private val pendingClose = ConcurrentHashMap.newKeySet<String>()
    private val engines = ConcurrentHashMap<EngineSpec, LiteRtLmEngine>()
    private val initLocks = ConcurrentHashMap<EngineSpec, Any>()

    data class EngineSpec(
        val modelPath: String,
        val useGpu: Boolean,
        val visionEnabled: Boolean,
        val audioEnabled: Boolean,
        val toolsKey: String = "",
    )

    fun enterInference() {
        inferenceDepth.incrementAndGet()
    }

    fun leaveInference() {
        inferenceDepth.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }

    fun hasActiveInference(): Boolean = inferenceDepth.get() > 0

    fun requestClose(modelPath: String) {
        if (hasActiveInference()) {
            pendingClose.add(modelPath)
            android.util.Log.w("LookbookLiteRtLm", "Deferring engine close — inference active")
        }
    }

    fun drainPendingClose(onClose: (String) -> Unit) {
        if (hasActiveInference()) return
        val paths = pendingClose.toList()
        pendingClose.clear()
        paths.forEach(onClose)
    }

    /** Borrow a warm engine; [block] runs while inference depth is elevated. */
    fun <T> withEngine(
        context: Context,
        spec: EngineSpec,
        tools: List<ToolSet> = emptyList(),
        block: (LiteRtLmEngine) -> T,
    ): T {
        val lock = initLocks.getOrPut(spec) { Any() }
        val engine = engines.getOrPut(spec) {
            LiteRtLmEngine(
                context = context,
                modelPath = spec.modelPath,
                useGpu = spec.useGpu,
                visionEnabled = spec.visionEnabled,
                audioEnabled = spec.audioEnabled,
                tools = tools,
                managedByCache = true,
            )
        }
        synchronized(lock) {
            if (!engine.isInitialized()) {
                engine.initialize()
            }
        }
        enterInference()
        return try {
            block(engine)
        } finally {
            leaveInference()
            drainPendingClose { path -> evictModelPath(path) }
        }
    }

    fun evictModelPath(modelPath: String) {
        if (hasActiveInference()) return
        engines.entries.removeIf { (spec, eng) ->
            if (spec.modelPath == modelPath) {
                eng.closeNow()
                initLocks.remove(spec)
                true
            } else {
                false
            }
        }
    }

    fun clearAll() {
        if (hasActiveInference()) {
            android.util.Log.w("LookbookLiteRtLm", "Skipping clearAll — LiteRT inference active")
            return
        }
        engines.values.forEach { it.closeNow() }
        engines.clear()
        initLocks.clear()
    }
}
