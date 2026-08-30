package com.zakir.vestra.shared.engine.litert

import android.content.Context
import com.google.ai.edge.litertlm.ToolSet
import com.zakir.vestra.shared.diagnostics.EngineLogHook
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reuses LiteRT-LM [Engine] instances per model spec — avoids cold-loading ~2.6 GB on every shot.
 * Single-flight init per key; never evict while inference is active (rc16 trim-safe pattern).
 */
object LiteRtLmEngineCache {
    private val inferenceDepth = AtomicInteger(0)
    private val pendingClose = ConcurrentHashMap.newKeySet<String>()
    private val engines = ConcurrentHashMap<EngineSpec, LiteRtLmEngine>()
    private val initLocks = ConcurrentHashMap<EngineSpec, Any>()

    // A pack/backend mismatch (e.g. a model file whose vision encoder the SDK rejects) fails
    // identically on every attempt — caching the reason turns a repeated multi-second native
    // init-and-crash into an immediate, honest failure instead of silently retrying it on every
    // single generation. Cleared alongside the engine on evictModelPath/clearAll (pack reinstall),
    // never on a bare "Retry load" — retrying a deterministic mismatch can't succeed differently.
    private val failed = ConcurrentHashMap<EngineSpec, String>()

    // The vendored SDK synchronizes native lifecycle calls per-Engine-instance only — it has no
    // documented contract for concurrent native calls across two DIFFERENT engine instances (e.g.
    // Code's Gemma-4-E2B and Chat's Qwen3, both intentionally left resident by the StudioBag
    // design). Two such calls racing into liblitertlm_jni.so is exactly what produced a native
    // SIGSEGV on a background inference thread. This mutex serializes only the actual native
    // call moment (see withEngine/withEngineFlow below) — never engine construction/warm-up,
    // which must stay free to run in parallel, or a second studio's first cold-load would wait
    // behind the first studio's multi-second model load.
    private val nativeCallMutex = Mutex()

    // Durable counterpart to [failed] — set once from VestraApp, survives cold start. Null in
    // unit tests and before app init; failureReason() degrades gracefully to the in-memory-only
    // map when unset.
    var failureStore: LiteRtLmFailureStore? = null

    /** In-memory first, then the durable store (which also warms the in-memory map on a hit). */
    fun failureReason(spec: EngineSpec): String? {
        failed[spec]?.let { return it }
        val persisted = failureStore?.isKnownFailed(spec) ?: return null
        failed[spec] = persisted
        return persisted
    }

    data class EngineSpec(
        val modelPath: String,
        val useGpu: Boolean,
        val visionEnabled: Boolean,
        val audioEnabled: Boolean,
        val toolsKey: String = "",
        val useNpu: Boolean = false,
        val enableSpeculativeDecoding: Boolean = false,
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

    /** Looks up (or cold-loads) the warm engine for [spec]. Does not touch inference depth. */
    private fun warmEngine(context: Context, spec: EngineSpec, tools: List<ToolSet>): LiteRtLmEngine {
        failureReason(spec)?.let { reason -> throw IllegalStateException(reason) }
        val lock = initLocks.getOrPut(spec) { Any() }
        val engine = engines.getOrPut(spec) {
            LiteRtLmEngine(
                context = context,
                modelPath = spec.modelPath,
                useGpu = spec.useGpu,
                useNpu = spec.useNpu,
                visionEnabled = spec.visionEnabled,
                audioEnabled = spec.audioEnabled,
                enableSpeculativeDecoding = spec.enableSpeculativeDecoding,
                tools = tools,
                managedByCache = true,
            )
        }
        synchronized(lock) {
            if (!engine.isInitialized()) {
                try {
                    engine.initialize()
                } catch (err: Throwable) {
                    engines.remove(spec)
                    initLocks.remove(spec)
                    // OutOfMemoryError is exactly the transient case the class comment above
                    // rules out caching for — another studio's model still resident, a large app
                    // heap at that moment — not a deterministic pack/backend mismatch. Leaving it
                    // uncached lets the next attempt retry for real once memory is freed, instead
                    // of permanently wedging this studio until a pack reinstall.
                    if (err !is OutOfMemoryError) {
                        val reason = err.message ?: "LiteRT-LM engine failed to initialize."
                        failed[spec] = reason
                        failureStore?.recordFailure(spec, reason)
                    }
                    // This catch block previously had no logging at all — a deterministic
                    // native init failure (e.g. the vision-encoder signature mismatch) left zero
                    // durable trace beyond the truncated message string reaching the UI.
                    // EngineLogHook.nonFatal() already logs (CrashReporter.recordNonFatal writes
                    // its own "W" trace line) in addition to persisting the full exception to
                    // crash_log.txt — a separate .e() call here would just duplicate that line.
                    EngineLogHook.nonFatal(
                        "LiteRtLmEngineCache.warmEngine",
                        err,
                        "model=${File(spec.modelPath).name} vision=${spec.visionEnabled} " +
                            "audio=${spec.audioEnabled} gpu=${spec.useGpu} " +
                            "cachedPermanently=${err !is OutOfMemoryError}",
                    )
                    throw err
                }
            }
        }
        return engine
    }

    /**
     * Borrow a warm engine; [block] runs while inference depth is elevated. Callers already run
     * off the UI thread (they're invoked from `Dispatchers.Default`-flowed generation flows), so
     * blocking here on [runBlocking] to acquire [nativeCallMutex] is safe. Bounded by the same
     * [LiteRtLmEngine.INFERENCE_TIMEOUT_SEC] the native call itself would time out at, so a
     * second studio's request fails with a clear "busy" message instead of hanging indefinitely
     * behind a stuck first call.
     */
    fun <T> withEngine(
        context: Context,
        spec: EngineSpec,
        tools: List<ToolSet> = emptyList(),
        block: (LiteRtLmEngine) -> T,
    ): T {
        val engine = warmEngine(context, spec, tools)
        enterInference()
        return try {
            runBlocking { withNativeCallLock { block(engine) } }
        } finally {
            leaveInference()
            drainPendingClose { path -> evictModelPath(path) }
        }
    }

    /**
     * Streaming counterpart of [withEngine]. A [Flow] builder runs its body lazily, only once
     * collected, so `enterInference()` must happen inside that body — calling it before
     * returning the flow (as a plain `withEngine { engine -> engine.someStreamingCall() }`
     * would) marks inference "done" before a single chunk has actually streamed, letting
     * [evictModelPath] close the engine mid-generation.
     */
    fun <T> withEngineFlow(
        context: Context,
        spec: EngineSpec,
        tools: List<ToolSet> = emptyList(),
        block: (LiteRtLmEngine) -> Flow<T>,
    ): Flow<T> = flow {
        val engine = warmEngine(context, spec, tools)
        enterInference()
        try {
            withNativeCallLock { emitAll(block(engine)) }
        } finally {
            leaveInference()
            drainPendingClose { path -> evictModelPath(path) }
        }
    }

    /**
     * Waits up to [LiteRtLmEngine.INFERENCE_TIMEOUT_SEC] to acquire [nativeCallMutex], then runs
     * [block] holding it — serializing the actual native call moment across every EngineSpec, not
     * just within one. Throws (rather than hanging) if the lock can't be acquired in time, so a
     * stuck first call can never wedge a second studio forever.
     *
     * `internal` (not `private`) so [LiteRtLmEngineCacheTest] can exercise the exact mutex +
     * timeout logic directly — `withEngine`/`withEngineFlow` themselves require a real, natively-
     * initialized [LiteRtLmEngine] that isn't constructible in a JVM unit test.
     */
    internal suspend fun <T> withNativeCallLock(block: suspend () -> T): T {
        val acquired = withTimeoutOrNull(LiteRtLmEngine.INFERENCE_TIMEOUT_SEC * 1000L) {
            nativeCallMutex.lock()
        }
        if (acquired == null) {
            throw IllegalStateException("Another on-device model is busy — try again in a moment.")
        }
        return try {
            block()
        } finally {
            nativeCallMutex.unlock()
        }
    }

    /**
     * True if the plain (non-vision, non-audio, no-NPU, no-speculative-decoding) engine for
     * [modelPath] is already warm — the only spec shape [Gemma4PrewarmWorker] ever warms. Matching
     * on [modelPath] alone would also count a vision- or audio-enabled engine resident for the same
     * file as "already warm" and skip prewarming the plain spec that Code generation actually needs.
     */
    fun isModelLoaded(modelPath: String): Boolean =
        engines.entries.any { (spec, engine) ->
            spec.modelPath == modelPath &&
                !spec.visionEnabled &&
                !spec.audioEnabled &&
                !spec.useNpu &&
                !spec.enableSpeculativeDecoding &&
                engine.isInitialized()
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
        failed.keys.removeIf { it.modelPath == modelPath }
    }

    fun clearAll() {
        if (hasActiveInference()) {
            android.util.Log.w("LookbookLiteRtLm", "Skipping clearAll — LiteRT inference active")
            return
        }
        engines.values.forEach { it.closeNow() }
        engines.clear()
        initLocks.clear()
        failed.clear()
    }
}
