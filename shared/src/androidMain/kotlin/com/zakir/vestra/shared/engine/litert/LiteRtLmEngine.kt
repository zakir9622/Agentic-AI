package com.zakir.vestra.shared.engine.litert

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.zakir.vestra.shared.diagnostics.EngineLogHook
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import com.google.ai.edge.litertlm.ToolSet
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

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
    private val managedByCache: Boolean = false,
    // Tried before GPU (falls back to GPU, then CPU, on failure) — only meaningful when
    // [useGpu] is also true, since NPU is a narrower preference nested under "prefer
    // accelerated". Default false: unverified on real hardware in this codebase.
    private val useNpu: Boolean = false,
    // Sets the SDK's `ExperimentalFlags.enableSpeculativeDecoding` before a GPU/NPU init —
    // never for CPU. Default false: an `@RequiresOptIn`-marked, explicitly "experimental and
    // temporary" flag with no behavior verified on real hardware here.
    private val enableSpeculativeDecoding: Boolean = false,
) : AutoCloseable {

    private var engine: Engine? = null
    private var loadMs: Long = 0L
    private var usedBackend: LiteRtLmBackendTier = LiteRtLmBackendTier.CPU

    // Dual-write: Logcat for local dev, EngineLogHook so it also lands in the exported
    // diagnostics bundle (app_trace.log) — this was previously Logcat-only, so GPU-fallback
    // reasons and cold-load timings never made it into a shared troubleshooting bundle.
    private fun logW(msg: String) {
        Log.w(TAG, msg)
        EngineLogHook.w(TAG, msg)
    }

    private fun logI(msg: String) {
        Log.i(TAG, msg)
        EngineLogHook.i(TAG, msg)
    }

    fun isInitialized(): Boolean = engine != null

    /** True if the actually-loaded backend is GPU or NPU (may differ from [useGpu] after a fallback). */
    fun usedGpuBackend(): Boolean = usedBackend != LiteRtLmBackendTier.CPU

    /** True only if NPU specifically loaded (as opposed to falling back to GPU or CPU). */
    fun usedNpuBackend(): Boolean = usedBackend == LiteRtLmBackendTier.NPU

    fun initialize() {
        val started = System.currentTimeMillis()
        if (useGpu && useNpu) {
            runCatching { buildAndInitEngine(LiteRtLmBackendTier.NPU) }
                .onSuccess { eng ->
                    engine = eng
                    usedBackend = LiteRtLmBackendTier.NPU
                }
                .onFailure { npuError ->
                    // NPU delegate availability is chipset/driver-specific and unverified on any
                    // device in this codebase's dev/test loop — treat a failure exactly like the
                    // established GPU failure below: fall back, don't wedge the studio.
                    logW("NPU engine init failed, falling back to GPU: ${npuError.message}")
                    initializeGpuThenCpu()
                }
        } else if (useGpu) {
            initializeGpuThenCpu()
        } else {
            engine = buildAndInitEngine(LiteRtLmBackendTier.CPU)
            usedBackend = LiteRtLmBackendTier.CPU
        }
        loadMs = System.currentTimeMillis() - started
        logI("Cold load ${loadMs}ms · ${File(modelPath).name} · ${backendLabel(usedBackend)}")
    }

    private fun initializeGpuThenCpu() {
        runCatching { buildAndInitEngine(LiteRtLmBackendTier.GPU) }
            .onSuccess { eng ->
                engine = eng
                usedBackend = LiteRtLmBackendTier.GPU
            }
            .onFailure { gpuError ->
                // GPU delegate compilation failing for a quantized model is a real, known
                // class of Android-device issue (driver/op-support gaps), not necessarily
                // transient — confirmed live via a user's device log:
                // "Failed to create engine: INTERNAL ... litert_compiled_model_executor.cc".
                // Without this fallback, "Retry load" just repeats the identical failing GPU
                // path forever; CPU is slower but should load the same weights correctly.
                logW("GPU engine init failed, falling back to CPU: ${gpuError.message}")
                val eng = buildAndInitEngine(LiteRtLmBackendTier.CPU)
                engine = eng
                usedBackend = LiteRtLmBackendTier.CPU
            }
    }

    private fun buildAndInitEngine(tier: LiteRtLmBackendTier): Engine {
        val backend = when (tier) {
            LiteRtLmBackendTier.NPU -> Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
            LiteRtLmBackendTier.GPU -> Backend.GPU()
            LiteRtLmBackendTier.CPU -> Backend.CPU()
        }
        val visionBackend = if (visionEnabled) backend else null
        val audioBackend = if (audioEnabled) Backend.CPU() else null
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = context.cacheDir.absolutePath,
            visionBackend = visionBackend,
            audioBackend = audioBackend,
        )
        // ExperimentalFlags.enableSpeculativeDecoding is a global static on the SDK, not part of
        // EngineConfig — set right before Engine(config).initialize() reads it. LiteRtLmEngineCache
        // deliberately lets cold-loads for DIFFERENT EngineSpecs run concurrently on different
        // threads (see its class doc), so without this lock two simultaneous cold-loads (e.g. one
        // studio falling back to CPU while another initializes on GPU) could interleave: one
        // thread's flag write lands between another thread's write and its own initialize() call,
        // silently building an engine with the wrong flag value. Scoped to just this SDK-mandated
        // set-then-initialize window (not the whole class's cache/warm-up locking), so it only
        // costs anything on the rare case of two truly-simultaneous cold loads.
        return synchronized(engineInitLock) {
            setSpeculativeDecoding(enableSpeculativeDecoding && tier != LiteRtLmBackendTier.CPU)
            val eng = Engine(config)
            eng.initialize()
            eng
        }
    }

    @OptIn(ExperimentalApi::class)
    private fun setSpeculativeDecoding(enabled: Boolean) {
        ExperimentalFlags.enableSpeculativeDecoding = enabled
    }

    fun coldLoadMs(): Long = loadMs

    fun generateText(
        prompt: String,
        system: String,
        temperature: Float = 0.2f,
        maxTokens: Int = 1024,
    ): LiteRtLmGenerateResult {
        val eng = engine ?: return LiteRtLmGenerateResult.Unavailable("LiteRT-LM engine not initialized.")
        return runWithTimeout("Code generation") {
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
        }
    }

    /**
     * Streaming variant of [generateText]: emits each incremental chunk as it's generated,
     * instead of blocking until the whole response is done.
     *
     * `Conversation.sendMessageAsync(text, extraContext): Flow<Message>` is the litertlm-android
     * API for this — verified against Google's own reference usage in the AI Edge Gallery app
     * (`LlmChatModelHelper.kt`'s `MessageCallback.onMessage`), which appends every callback's
     * `message.toString()` onto an accumulator (`"$response$partialResult"`). That confirms each
     * emission is a **delta chunk**, not the cumulative text so far — this method accumulates
     * those deltas itself so callers always receive the full text-so-far.
     */
    fun generateTextStream(
        prompt: String,
        system: String,
        temperature: Float = 0.2f,
    ): Flow<LiteRtLmStreamEvent> = flow {
        val eng = engine
        if (eng == null) {
            emit(LiteRtLmStreamEvent.Unavailable("LiteRT-LM engine not initialized."))
            return@flow
        }
        val convConfig = ConversationConfig(
            systemInstruction = if (system.isBlank()) null else Contents.of(system.trim()),
            samplerConfig = SamplerConfig(temperature = temperature.toDouble(), topK = 40, topP = 0.95),
            tools = tools.map { tool(it) },
        )
        try {
            withTimeout(INFERENCE_TIMEOUT_SEC * 1000L) {
                eng.createConversation(convConfig).use { conversation ->
                    val builder = StringBuilder()
                    conversation.sendMessageAsync(prompt.trim(), emptyMap()).collect { message ->
                        val delta = message.toString()
                        if (delta.isNotEmpty()) {
                            builder.append(delta)
                            emit(LiteRtLmStreamEvent.Partial(builder.toString()))
                        }
                    }
                    if (builder.isBlank()) {
                        emit(LiteRtLmStreamEvent.Unavailable("Model returned empty text."))
                    } else {
                        val text = builder.toString()
                        emit(LiteRtLmStreamEvent.Done(text, prompt.length / 4, text.length / 4))
                    }
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            emit(
                LiteRtLmStreamEvent.Unavailable(
                    "Code generation timed out after ${INFERENCE_TIMEOUT_SEC}s — try a shorter prompt or cloud.",
                ),
            )
        } catch (err: Throwable) {
            emit(LiteRtLmStreamEvent.Unavailable(err.message?.take(200) ?: "LiteRT-LM failed."))
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
        return runWithTimeout("Vision assist") {
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
        return runWithTimeout("Transcription") {
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
        }
    }

    private fun runWithTimeout(label: String, block: () -> LiteRtLmGenerateResult): LiteRtLmGenerateResult {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            val future = executor.submit(Callable { block() })
            future.get(INFERENCE_TIMEOUT_SEC, TimeUnit.SECONDS)
        } catch (timeout: TimeoutException) {
            LiteRtLmGenerateResult.Unavailable(
                "$label timed out after ${INFERENCE_TIMEOUT_SEC}s — try a shorter prompt or cloud.",
            )
        } catch (err: Throwable) {
            LiteRtLmGenerateResult.Unavailable(
                err.message?.take(200) ?: "$label failed.",
            )
        } finally {
            executor.shutdownNow()
        }
    }

    /** Immediate close — used by [LiteRtLmEngineCache] eviction only. */
    fun closeNow() {
        runCatching { engine?.close() }
        engine = null
    }

    override fun close() {
        if (managedByCache) {
            LiteRtLmEngineCache.requestClose(modelPath)
            if (LiteRtLmEngineCache.hasActiveInference()) return
            LiteRtLmEngineCache.evictModelPath(modelPath)
            return
        }
        closeNow()
    }

    companion object {
        private const val TAG = "LookbookLiteRtLm"
        const val INFERENCE_TIMEOUT_SEC = 90L

        // Shared across every LiteRtLmEngine instance — the race it guards (ExperimentalFlags is
        // a global SDK static) is cross-instance, so a per-instance lock wouldn't help.
        private val engineInitLock = Any()

        fun backendLabel(tier: LiteRtLmBackendTier): String = when (tier) {
            LiteRtLmBackendTier.NPU -> "NPU"
            LiteRtLmBackendTier.GPU -> "GPU"
            LiteRtLmBackendTier.CPU -> "CPU"
        }

        fun toolsKey(tools: List<ToolSet>): String =
            tools.joinToString(",") { it.javaClass.name }
    }
}

/** Which physical backend an [LiteRtLmEngine] actually loaded on, after any fallback. */
enum class LiteRtLmBackendTier { NPU, GPU, CPU }

sealed class LiteRtLmGenerateResult {
    data class Ok(val text: String, val tokensIn: Int, val tokensOut: Int) : LiteRtLmGenerateResult()
    data class Unavailable(val reason: String) : LiteRtLmGenerateResult()
}

/** Emissions from [LiteRtLmEngine.generateTextStream]. [Partial.textSoFar] is cumulative. */
sealed class LiteRtLmStreamEvent {
    data class Partial(val textSoFar: String) : LiteRtLmStreamEvent()
    data class Done(val text: String, val tokensIn: Int, val tokensOut: Int) : LiteRtLmStreamEvent()
    data class Unavailable(val reason: String) : LiteRtLmStreamEvent()
}
