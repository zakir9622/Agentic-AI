package com.zakir.vestra.shared.engine.litert

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Guards LiteRT-LM engine lifecycle — mirrors [com.zakir.vestra.shared.engine.lite.OrtSessionCache].
 * Never evict or close engines while inference is active (rc16 trim-safe pattern).
 */
object LiteRtLmEngineCache {
    private val inferenceDepth = AtomicInteger(0)
    private val pendingClose = ConcurrentHashMap.newKeySet<String>()

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
}
