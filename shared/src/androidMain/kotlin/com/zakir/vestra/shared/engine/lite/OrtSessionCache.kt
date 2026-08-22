package com.zakir.vestra.shared.engine.lite

import java.util.concurrent.ConcurrentHashMap

/**
 * Caches ONNX sessions per model path to avoid cold-load latency on every shot.
 * Invalidated when pack version changes (path includes version directory).
 */
object OrtSessionCache {
    private val cache = ConcurrentHashMap<String, OrtModel>()

    fun open(modelPath: String): OrtModel =
        cache.getOrPut(modelPath) {
            // Construction may throw IllegalStateException on bad graphs / native link errors.
            OrtModel(modelPath)
        }

    fun invalidateContaining(packRoot: String) {
        cache.keys.filter { it.startsWith(packRoot) }.forEach { key ->
            cache.remove(key)?.close()
        }
    }

    fun clearAll() {
        cache.values.forEach { it.close() }
        cache.clear()
    }
}
