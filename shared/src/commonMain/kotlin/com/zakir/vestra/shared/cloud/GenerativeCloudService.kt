package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.safety.InputSafetyGate
import com.zakir.vestra.shared.safety.SafetyVerdict
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.engine.local.LocalCodeGenerator
import com.zakir.vestra.shared.engine.local.LocalCodeResult
import com.zakir.vestra.shared.engine.local.LocalImageGenerator
import com.zakir.vestra.shared.engine.local.LocalImageResult
import com.zakir.vestra.shared.engine.local.LocalVideoGenerator
import com.zakir.vestra.shared.engine.local.LocalVideoResult
import com.zakir.vestra.shared.engine.local.UnimplementedLocalCodeGenerator
import com.zakir.vestra.shared.engine.local.UnimplementedLocalImageGenerator
import com.zakir.vestra.shared.engine.local.UnimplementedLocalVideoGenerator
import com.zakir.vestra.shared.audio.VoiceCatalog
import com.zakir.vestra.shared.audio.VoiceKnobs
import com.zakir.vestra.shared.audio.VoicePersona
import com.zakir.vestra.shared.engine.local.LocalAudioGenerator
import com.zakir.vestra.shared.engine.local.LocalAudioResult
import com.zakir.vestra.shared.engine.local.LocalVoiceChanger
import com.zakir.vestra.shared.engine.local.UnimplementedLocalAudioGenerator
import com.zakir.vestra.shared.engine.local.UnimplementedLocalVoiceChanger
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.shared.time.EpochClock
import io.ktor.client.HttpClient
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

sealed interface GenerativeState {
    data class Preparing(val message: String) : GenerativeState
    /**
     * @param stage Human-readable activity (no baked-in countdown — UI ticks [deadlineEpochMs]).
     * @param deadlineEpochMs Wall-clock deadline for remaining-seconds display; null = no timer.
     */
    data class Running(
        val fraction: Float,
        val stage: String,
        val deadlineEpochMs: Long? = null,
    ) : GenerativeState
    data class ImageReady(val path: String, val providerId: String) : GenerativeState
    data class VideoReady(val path: String, val providerId: String) : GenerativeState
    data class AudioReady(val path: String, val providerId: String) : GenerativeState
    data class CodeReady(val text: String, val tokensIn: Int, val tokensOut: Int, val providerId: String) : GenerativeState
    data class Failed(val message: String) : GenerativeState
}

/**
 * Free-tier generative service: HF Spaces + HF Inference Providers for image/video,
 * Groq/HF/OpenRouter for code. Optional local generators are tried first when ready
 * (Create / Edit / Audio / Code / Video still-clip).
 */
class GenerativeCloudService(
    private val http: HttpClient,
    private val io: CloudImageIo,
    private val settings: AppSettings,
    private val usage: UsageLedger,
    private val health: ModelHealthTracker = settings.modelHealth,
    private val localImage: LocalImageGenerator = UnimplementedLocalImageGenerator,
    private val localAudio: LocalAudioGenerator = UnimplementedLocalAudioGenerator,
    private val localVoiceChanger: LocalVoiceChanger = UnimplementedLocalVoiceChanger,
    private val localCode: LocalCodeGenerator = UnimplementedLocalCodeGenerator,
    private val localVideo: LocalVideoGenerator = UnimplementedLocalVideoGenerator,
) {
    fun localImageReady(): Boolean = localImage.isReady()

    fun localImageEditReady(): Boolean = localImage.isEditReady()

    fun localAudioReady(): Boolean = localAudio.isReady()

    fun localCodeReady(): Boolean = localCode.isReady()

    fun localVideoReady(): Boolean = localVideo.isReady()

    private val hf = HfGradioClient(http)
    private val hfInference = HfInferenceClient(http)
    private val llm = LlmClient(http)
    private val schema = GradioSchemaClient(http)

    @OptIn(ExperimentalEncodingApi::class)
    fun generateImage(
        prompt: String,
        referenceUri: String?,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val capability = if (referenceUri.isNullOrBlank()) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT
        val provider = settings.selectedProvider(capability)
        var attempted = provider
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            when (val safety = InputSafetyGate.checkPrompt(prompt)) {
                is SafetyVerdict.Blocked -> error(safety.reason)
                is SafetyVerdict.Ok -> Unit
            }
            // Prefer local when offline (or network probe says so). When online, honor the
            // selected cloud model first; local remains a fallback after cloud failures.
            val networkOk = settings.networkLikelyAvailable()
            val localReady = when {
                referenceUri.isNullOrBlank() -> localImage.isReady()
                else -> localImage.isEditReady()
            }
            val tryLocalFirst = localReady && !networkOk
            if (tryLocalFirst) {
                val stage = if (referenceUri.isNullOrBlank()) {
                    "Generating on-device…"
                } else {
                    "Editing on-device…"
                }
                emit(GenerativeState.Running(0.08f, stage))
                when (
                    val local = localImage.generate(
                        prompt.trim(),
                        assists.seed,
                        referenceImageUri = referenceUri,
                    )
                ) {
                    is LocalImageResult.Ok -> {
                        val providerId = if (referenceUri.isNullOrBlank()) {
                            "local-sdturbo-v1"
                        } else {
                            "local-sdturbo-edit"
                        }
                        emit(GenerativeState.ImageReady(local.imagePath, providerId))
                        return@flow
                    }
                    is LocalImageResult.Unavailable -> {
                        emit(
                            GenerativeState.Running(
                                0.1f,
                                "Local pack unavailable — trying cloud…",
                            ),
                        )
                    }
                }
            }
            // Probe can lag behind real 5G/Wi‑Fi — prefer attempting the request.
            if (!settings.networkLikelyAvailable()) {
                emit(
                    GenerativeState.Running(
                        0.05f,
                        "Network probe uncertain — trying cloud anyway…",
                    ),
                )
            }
            val candidates = CloudModelRouting.fallbackChain(provider, capability, settings, health)
            val referenceDataUrl = referenceUri?.takeIf { it.isNotBlank() }?.let {
                val bytes = io.loadImageBytes(it) ?: error("Couldn't read the reference image")
                io.toDataUrl(bytes)
            }
            val variants = visualPromptVariants(prompt, assists)
            var lastFailure: CloudFailure = CloudFailure.Unknown("Image generation failed")
            var skipInference = false
            var skipSpaces = false
            var offline = false
            var deadline = GenerationBudget.forImage().deadlineMs
            var fallbackGraceUsed = false
            emit(
                GenerativeState.Running(
                    0.05f,
                    "Queued · ${provider.displayName}",
                    deadlineEpochMs = deadline,
                ),
            )

            for ((modelIndex, candidate) in candidates.withIndex()) {
                var budget = GenerationBudget(deadline)
                if (budget.expired()) {
                    // Primary ZeroGPU Spaces often burn the whole 120s on one hung poll; grant one
                    // short grace window so InstructPix2Pix (etc.) can still run.
                    val canGrace = !fallbackGraceUsed &&
                        modelIndex > 0 &&
                        lastFailure.allowsImageFallbackGrace()
                    if (!canGrace) break
                    fallbackGraceUsed = true
                    deadline = com.zakir.vestra.shared.time.EpochClock.System.nowMs() +
                        GenerationBudget.IMAGE_FALLBACK_GRACE_MS
                    budget = GenerationBudget(deadline)
                }
                if (offline) break
                attempted = candidate
                if (skipInference && candidate.platform == CloudPlatform.HF_INFERENCE) continue
                if (skipSpaces && candidate.platform == CloudPlatform.HF_SPACE) continue
                if (CloudModelContracts.preflightOrNull(candidate) != null) continue
                if (candidate.requiresApiKey && settings.apiKeyFor(candidate).isNullOrBlank()) continue
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.3f,
                            when {
                                skipSpaces -> "ZeroGPU empty — trying ${candidate.displayName}…"
                                fallbackGraceUsed ->
                                    "${provider.displayName} timed out — trying ${candidate.displayName}…"
                                else -> "${provider.displayName} is busy — trying ${candidate.displayName}…"
                            },
                            deadlineEpochMs = deadline,
                        ),
                    )
                }

                var advanceModel = false
                for ((index, variant) in variants.withIndex()) {
                    if (advanceModel) break
                    budget = GenerationBudget(deadline)
                    if (budget.expired()) break
                    emit(
                        GenerativeState.Running(
                            0.2f + index * 0.15f,
                            if (index == 0) "Submitting to ${candidate.displayName}…"
                            else "Retrying with softer prompt…",
                            deadlineEpochMs = deadline,
                        ),
                    )
                    try {
                        val path = when (candidate.platform) {
                            CloudPlatform.HF_INFERENCE -> {
                                val token = settings.hfToken.value
                                    ?: throw CloudFailureException(CloudFailure.AuthRejected)
                                emit(
                                    GenerativeState.Running(
                                        0.5f,
                                        "HF Inference · ${candidate.displayName}",
                                        deadlineEpochMs = deadline,
                                    ),
                                )
                                val bytes = if (referenceDataUrl != null) {
                                    val refBytes = io.loadImageBytes(referenceUri!!)
                                        ?: error("Couldn't read the reference image")
                                    hfInference.imageToImage(
                                        modelId = candidate.endpoint,
                                        prompt = variant,
                                        imageBytes = refBytes,
                                        hfToken = token,
                                    )
                                } else {
                                    hfInference.textToImage(
                                        modelId = candidate.endpoint,
                                        prompt = variant,
                                        hfToken = token,
                                    )
                                }
                                CloudOutputValidator.validate(bytes)?.let {
                                    throw CloudFailureException(CloudFailure.BadOutput)
                                }
                                io.downloadResult(
                                    "data:image/png;base64,${Base64.encode(bytes)}",
                                )
                            }
                            CloudPlatform.HF_SPACE -> {
                                val data = resolveImageSpacePayload(
                                    candidate,
                                    variant,
                                    referenceDataUrl,
                                )
                                emit(
                                    GenerativeState.Running(
                                        0.35f,
                                        "Space queue · ${candidate.displayName}",
                                        deadlineEpochMs = deadline,
                                    ),
                                )
                                val wakeRetries = if (budget.allowWakeRetry()) 1 else 0
                                val result = hf.predict(
                                    candidate.endpoint,
                                    CloudModelContracts.effectiveApiName(candidate),
                                    data,
                                    settings.hfToken.value,
                                    maxPolls = budget.maxPolls(),
                                    wakeRetries = wakeRetries,
                                    deadlineMs = deadline,
                                    pollRequestTimeoutMs = GenerationBudget.GRADIO_POLL_REQUEST_TIMEOUT_MS,
                                    onPoll = { pollIndex, maxPolls ->
                                        val frac =
                                            0.35f + 0.5f * (pollIndex + 1).toFloat() / maxPolls.coerceAtLeast(1)
                                        emit(
                                            GenerativeState.Running(
                                                frac.coerceIn(0.35f, 0.9f),
                                                "Space poll ${pollIndex + 1}/$maxPolls · ${candidate.displayName}",
                                                deadlineEpochMs = deadline,
                                            ),
                                        )
                                    },
                                )
                                val url = GradioOutput.extractMediaRef(result)
                                emit(
                                    GenerativeState.Running(
                                        0.92f,
                                        "Downloading image…",
                                        deadlineEpochMs = deadline,
                                    ),
                                )
                                io.downloadResult(url, spaceHost = candidate.endpoint)
                            }
                            else -> throw CloudFailureException(
                                CloudFailure.Unknown("Unsupported platform for images: ${candidate.platform}"),
                            )
                        }
                        usage.record(
                            candidate,
                            success = true,
                            note = "Image · ${CloudModelContracts.statusLabel(candidate)} · ${prompt.take(80)}",
                        )
                        health.recordSuccess(candidate.id)
                        emit(GenerativeState.ImageReady(path, candidate.id))
                        return@flow
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val failure = CloudFailureClassifier.from(e)
                        lastFailure = failure
                        val kind = when (failure) {
                            is CloudFailure.QuotaExhausted ->
                                if (failure.scope == CloudFailure.QuotaExhausted.Scope.ACCOUNT) {
                                    ModelHealthTracker.FailureKind.QUOTA_ACCOUNT
                                } else {
                                    ModelHealthTracker.FailureKind.GENERIC
                                }
                            CloudFailure.CreditsExhausted -> ModelHealthTracker.FailureKind.CREDITS
                            CloudFailure.Offline -> ModelHealthTracker.FailureKind.OFFLINE
                            else -> ModelHealthTracker.FailureKind.GENERIC
                        }
                        health.recordFailure(candidate.id, kind)
                        when {
                            failure is CloudFailure.Offline -> {
                                offline = true
                                break
                            }
                            failure is CloudFailure.QuotaExhausted &&
                                failure.scope == CloudFailure.QuotaExhausted.Scope.ACCOUNT -> {
                                skipSpaces = true
                                advanceModel = true
                            }
                            failure is CloudFailure.CreditsExhausted -> {
                                skipInference = true
                                advanceModel = true
                            }
                            failure.advanceModel -> advanceModel = true
                            failure.retryVariants && index < variants.lastIndex -> Unit
                            else -> advanceModel = true
                        }
                    }
                }
            }
            throw CloudFailureException(lastFailure)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = CloudFailureClassifier.from(e)
            val rawForFriendly = when (failure) {
                CloudFailure.SchemaRejected -> "event: error data: null"
                CloudFailure.CreditsExhausted -> "HTTP 402: depleted your monthly Inference Providers credits"
                CloudFailure.Offline -> "No internet connection"
                is CloudFailure.QuotaExhausted -> "ZeroGPU quota exceeded"
                CloudFailure.Timeout -> "Request timed out"
                is CloudFailure.Unknown -> failure.raw
                else -> failure.toUserHint()
            }
            usage.record(
                attempted,
                success = false,
                note = CloudModelContracts.usageFailureNote(attempted, rawForFriendly),
            )
            emit(
                GenerativeState.Failed(
                    CloudModelContracts.friendlyFailure(
                        attempted,
                        rawForFriendly,
                        "Image generation",
                        selectedDisplayName = provider.displayName,
                    ),
                ),
            )
        }
    }

    private suspend fun resolveImageSpacePayload(
        candidate: CloudModelProvider,
        variant: String,
        referenceDataUrl: String?,
    ): List<kotlinx.serialization.json.JsonElement> {
        val handTuned = runCatching {
            if (referenceDataUrl != null) {
                if (!SpacePayloads.hasImageEdit(candidate.id)) null
                else SpacePayloads.forImageEdit(candidate.id, variant, referenceDataUrl)
            } else {
                if (!SpacePayloads.hasImageGen(candidate.id)) null
                else SpacePayloads.forImageGen(candidate.id, variant)
            }
        }.getOrNull()
        if (handTuned != null) return handTuned
        val roles = GradioSchemaClient.promptRoles(
            prompt = variant,
            image = referenceDataUrl?.let { SpacePayloads.fileData(it) },
        )
        val schemaPayload = schema.buildPayload(
            spaceHost = candidate.endpoint,
            apiName = CloudModelContracts.effectiveApiName(candidate),
            roles = roles,
        )
        if (schemaPayload != null) return schemaPayload
        throw CloudFailureException(CloudFailure.SchemaRejected)
    }

    private fun Exception.isMonthlyCreditsExhausted(): Boolean {
        val msg = message.orEmpty()
        return msg.contains("402", ignoreCase = true) ||
            msg.contains("depleted your monthly", ignoreCase = true) ||
            msg.contains("Inference Providers monthly credits", ignoreCase = true) ||
            msg.contains("monthly credits are used up", ignoreCase = true)
    }

    private fun Exception.isAccountQuotaExhausted(): Boolean {
        val msg = message.orEmpty()
        return msg.contains("quota exceeded", ignoreCase = true) ||
            msg.contains("ZeroGPU quota", ignoreCase = true) ||
            msg.contains("exceeded your free ZeroGPU", ignoreCase = true) ||
            msg.contains("0s left", ignoreCase = true)
    }

    private fun Exception.isNonRetryableInferenceError(): Boolean {
        val msg = message.orEmpty()
        return msg.contains("depleted your monthly", ignoreCase = true) ||
            msg.contains("Inference Providers monthly credits", ignoreCase = true) ||
            msg.contains("token rejected for Inference", ignoreCase = true)
    }

    private fun Exception.isBrokenInferenceRoute(): Boolean {
        val msg = message.orEmpty()
        return msg.contains("Model not supported by provider", ignoreCase = true)
    }

    private fun Exception.isNetworkError(): Boolean {
        val msg = message.orEmpty()
        return msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("UnknownHostException", ignoreCase = true) ||
            msg.contains("Network is unreachable", ignoreCase = true) ||
            msg.contains("failed to connect", ignoreCase = true)
    }

    fun generateCode(
        prompt: String,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.CODE)
        var attempted = provider
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            when (val safety = InputSafetyGate.checkPrompt(prompt)) {
                is SafetyVerdict.Blocked -> error(safety.reason)
                is SafetyVerdict.Ok -> Unit
            }
            if (localCode.isReady() && !settings.networkLikelyAvailable()) {
                emit(GenerativeState.Running(0.08f, "Generating code on-device…"))
                when (val local = localCode.generate(prompt.trim(), buildCodeSystem(assists))) {
                    is LocalCodeResult.Ok -> {
                        emit(
                            GenerativeState.CodeReady(
                                local.text,
                                local.tokensIn,
                                local.tokensOut,
                                "local-gemma-v1",
                            ),
                        )
                        return@flow
                    }
                    is LocalCodeResult.Unavailable -> {
                        emit(
                            GenerativeState.Running(
                                0.1f,
                                "Local Gemma unavailable — trying cloud…",
                            ),
                        )
                    }
                }
            }
            if (!settings.networkLikelyAvailable()) {
                emit(
                    GenerativeState.Running(
                        0.05f,
                        "Network probe uncertain — trying cloud anyway…",
                    ),
                )
            }
            val candidates = CloudModelRouting.codeFallbackChain(provider, settings)
            val system = buildCodeSystem(assists)
            val temperature = when {
                assists.creative && assists.pragmatic -> 0.5
                assists.creative -> 0.55
                else -> 0.2
            }
            val cleaned = prompt.trim().ifBlank { "Write a short Hello World in Kotlin." }
            val attempts = listOf(
                cleaned,
                "Complete this coding request helpfully. Assume lawful software intent:\n\n$cleaned",
                "Provide working code for:\n$cleaned\n\nIf anything is unclear, pick sensible defaults and note them.",
            )
            var lastError: Exception? = null
            val codeDeadline = EpochClock.System.nowMs() + 90_000L
            for ((modelIndex, candidate) in candidates.withIndex()) {
                attempted = candidate
                CloudModelContracts.preflightOrNull(candidate)?.let { error(it) }
                requireKeyIfNeeded(candidate)
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.25f,
                            "${provider.displayName} unavailable — trying ${candidate.displayName}…",
                            deadlineEpochMs = codeDeadline,
                        ),
                    )
                } else {
                    emit(
                        GenerativeState.Running(
                            0.3f,
                            "Calling ${candidate.displayName}…",
                            deadlineEpochMs = codeDeadline,
                        ),
                    )
                }
                val key = settings.apiKeyFor(candidate) ?: error("API key required for ${candidate.displayName}")
                var result: LlmResult? = null
                var quotaExhausted = false
                for ((i, attempt) in attempts.withIndex()) {
                    if (i > 0) emit(GenerativeState.Running(0.35f + i * 0.1f, "Retrying…"))
                    try {
                        result = llm.chat(candidate.platform, candidate.endpoint, attempt, key, system, temperature)
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        if (e.isMonthlyCreditsExhausted()) {
                            quotaExhausted = true
                            break
                        }
                    }
                }
                if (quotaExhausted && modelIndex < candidates.lastIndex) {
                    health.recordFailure(candidate.id, ModelHealthTracker.FailureKind.CREDITS)
                    continue
                }
                if (result != null) {
                    usage.record(
                        candidate,
                        tokensIn = result.tokensIn,
                        tokensOut = result.tokensOut,
                        success = true,
                        note = "Code · ${CloudModelContracts.statusLabel(candidate)} · ${prompt.take(80)}",
                    )
                    health.recordSuccess(candidate.id)
                    emit(GenerativeState.CodeReady(result.text, result.tokensIn, result.tokensOut, candidate.id))
                    return@flow
                }
                health.recordFailure(candidate.id)
            }
            throw lastError ?: IllegalStateException("Empty LLM response")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(
                attempted,
                success = false,
                note = CloudModelContracts.usageFailureNote(attempted, e.message.orEmpty()),
            )
            health.recordFailure(attempted.id)
            emit(GenerativeState.Failed(
                CloudModelContracts.friendlyFailure(
                    attempted,
                    e.message.orEmpty(),
                    "Code generation",
                    selectedDisplayName = provider.displayName,
                ),
            ))
        }
    }

    fun generateVideo(
        prompt: String,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.VIDEO)
        var attempted = provider
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            when (val safety = InputSafetyGate.checkPrompt(prompt)) {
                is SafetyVerdict.Blocked -> error(safety.reason)
                is SafetyVerdict.Ok -> Unit
            }
            if (localVideo.isReady() && !settings.networkLikelyAvailable()) {
                emit(GenerativeState.Running(0.08f, "Encoding local still-clip…"))
                when (val local = localVideo.generate(prompt.trim(), assists.seed)) {
                    is LocalVideoResult.Ok -> {
                        emit(GenerativeState.VideoReady(local.videoPath, "local-stillclip-v1"))
                        return@flow
                    }
                    is LocalVideoResult.Unavailable -> {
                        emit(
                            GenerativeState.Running(
                                0.1f,
                                "Local still-clip unavailable — trying cloud…",
                            ),
                        )
                    }
                }
            }
            if (!settings.networkLikelyAvailable()) {
                emit(
                    GenerativeState.Running(
                        0.05f,
                        "Network probe uncertain — trying cloud anyway…",
                    ),
                )
            }
            val candidates = CloudModelRouting.fallbackChain(provider, AiCapability.VIDEO, settings, health)
            val variants = visualPromptVariants(prompt, assists)
            val budget = GenerationBudget.forVideo()
            val deadline = budget.deadlineMs
            var lastError: Exception? = null
            emit(
                GenerativeState.Running(
                    0.05f,
                    "Queued · ${provider.displayName}",
                    deadlineEpochMs = deadline,
                ),
            )
            for ((modelIndex, candidate) in candidates.withIndex()) {
                budget.throwIfExpired()
                attempted = candidate
                CloudModelContracts.preflightOrNull(candidate)?.let { error(it) }
                requireKeyIfNeeded(candidate)
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.2f,
                            "${provider.displayName} is busy — trying ${candidate.displayName}…",
                            deadlineEpochMs = deadline,
                        ),
                    )
                }
                for ((index, variant) in variants.withIndex()) {
                    budget.throwIfExpired()
                    emit(
                        GenerativeState.Running(
                            0.15f + index * 0.15f,
                            if (index == 0) {
                                "Submitting video job · ${candidate.displayName}"
                            } else {
                                "Retrying with softer prompt…"
                            },
                            deadlineEpochMs = deadline,
                        ),
                    )
                    try {
                        val maxPolls = when (candidate.id) {
                            // Wan2 queues hard — fail fast so LTX can run within the budget.
                            "wan2-video-hf" -> budget.maxPolls(pollDelayMs = 2_500, floor = 3, ceiling = 24)
                            else -> budget.maxPolls(pollDelayMs = 3_000, floor = 5, ceiling = 90)
                        }
                        val pollDelay = if (candidate.id == "wan2-video-hf") 2_500L else 3_000L
                        val result = hf.predict(
                            spaceHost = candidate.endpoint,
                            apiName = CloudModelContracts.effectiveApiName(candidate),
                            data = SpacePayloads.forVideo(candidate.id, variant),
                            hfToken = settings.hfToken.value,
                            maxPolls = maxPolls,
                            pollDelayMs = pollDelay,
                            onPoll = { pollIndex, polls ->
                                val frac =
                                    0.2f + 0.65f * (pollIndex + 1).toFloat() / polls.coerceAtLeast(1)
                                emit(
                                    GenerativeState.Running(
                                        frac.coerceIn(0.2f, 0.9f),
                                        "Video poll ${pollIndex + 1}/$polls · ${candidate.displayName}",
                                        deadlineEpochMs = deadline,
                                    ),
                                )
                            },
                        )
                        val url = extractRef(result)
                        emit(
                            GenerativeState.Running(
                                0.92f,
                                "Downloading video…",
                                deadlineEpochMs = deadline,
                            ),
                        )
                        val path = io.downloadResult(url, spaceHost = candidate.endpoint)
                        usage.record(
                            candidate,
                            success = true,
                            note = "Video · ${CloudModelContracts.statusLabel(candidate)} · ${prompt.take(80)}",
                        )
                        health.recordSuccess(candidate.id)
                        emit(GenerativeState.VideoReady(path, candidate.id))
                        return@flow
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val failure = CloudFailureClassifier.from(e)
                        lastError = e
                        val kind = when {
                            e.isAccountQuotaExhausted() -> ModelHealthTracker.FailureKind.QUOTA_ACCOUNT
                            failure is CloudFailure.Offline -> ModelHealthTracker.FailureKind.OFFLINE
                            e.message.orEmpty().contains("429") ||
                                e.message.orEmpty().contains("rate limit", ignoreCase = true) ||
                                e.message.orEmpty().contains("Queue is full", ignoreCase = true) ->
                                ModelHealthTracker.FailureKind.RATE_LIMIT
                            else -> ModelHealthTracker.FailureKind.GENERIC
                        }
                        health.recordFailure(candidate.id, kind)
                        if (e.isAccountQuotaExhausted()) throw e
                        if (failure is CloudFailure.Offline) throw e
                        // Rate-limited / full queue: skip remaining prompt variants for this host.
                        if (kind == ModelHealthTracker.FailureKind.RATE_LIMIT) break
                    }
                }
            }
            throw lastError ?: IllegalStateException("Video generation failed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(
                attempted,
                success = false,
                note = CloudModelContracts.usageFailureNote(attempted, e.message.orEmpty()),
            )
            health.recordFailure(
                attempted.id,
                if (e.isAccountQuotaExhausted()) {
                    ModelHealthTracker.FailureKind.QUOTA_ACCOUNT
                } else {
                    ModelHealthTracker.FailureKind.GENERIC
                },
            )
            emit(GenerativeState.Failed(
                CloudModelContracts.friendlyFailure(
                    attempted,
                    e.message.orEmpty(),
                    "Video generation",
                    selectedDisplayName = provider.displayName,
                ),
            ))
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun generateAudio(
        prompt: String,
        persona: VoicePersona = VoiceCatalog.byId(VoiceCatalog.defaultId),
        knobs: VoiceKnobs = VoiceKnobs.Default,
        referenceAudioUri: String? = null,
    ): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.AUDIO)
        var attempted = provider
        val safeKnobs = knobs.sanitized()
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            when (val safety = InputSafetyGate.checkPrompt(prompt)) {
                is SafetyVerdict.Blocked -> error(safety.reason)
                is SafetyVerdict.Ok -> Unit
            }
            // Voice-changer-only path: transform an existing clip offline.
            if (!referenceAudioUri.isNullOrBlank() && prompt.trim().equals("voice-change", ignoreCase = true)) {
                emit(GenerativeState.Running(0.2f, "Applying local voice knobs…"))
                when (val changed = localVoiceChanger.transform(referenceAudioUri, safeKnobs)) {
                    is LocalAudioResult.Ok -> {
                        emit(GenerativeState.AudioReady(changed.audioPath, "local-voice-changer"))
                        return@flow
                    }
                    is LocalAudioResult.Unavailable -> error(changed.reason)
                }
            }
            if (localAudio.isReady()) {
                emit(GenerativeState.Running(0.08f, "Generating speech on-device…"))
                when (val local = localAudio.generate(prompt.trim(), persona, safeKnobs)) {
                    is LocalAudioResult.Ok -> {
                        emit(GenerativeState.AudioReady(local.audioPath, "local-tts-system"))
                        return@flow
                    }
                    is LocalAudioResult.Unavailable -> {
                        emit(GenerativeState.Running(0.1f, "Local TTS unavailable — using cloud…"))
                    }
                }
            }
            if (!settings.networkLikelyAvailable()) {
                emit(
                    GenerativeState.Running(
                        0.05f,
                        "Network probe uncertain — trying cloud TTS anyway…",
                    ),
                )
            }
            val candidates = CloudModelRouting.fallbackChain(provider, AiCapability.AUDIO, settings, health)
            val budget = GenerationBudget.forAudio()
            val deadline = budget.deadlineMs
            var lastError: Exception? = null
            emit(
                GenerativeState.Running(
                    0.05f,
                    "Queued · ${provider.displayName} · ${persona.displayName}",
                    deadlineEpochMs = deadline,
                ),
            )
            for ((modelIndex, candidate) in candidates.withIndex()) {
                budget.throwIfExpired()
                attempted = candidate
                CloudModelContracts.preflightOrNull(candidate)?.let { error(it) }
                requireKeyIfNeeded(candidate)
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.2f,
                            "${provider.displayName} busy — trying ${candidate.displayName}…",
                            deadlineEpochMs = deadline,
                        ),
                    )
                }
                try {
                    val path = when (candidate.platform) {
                        CloudPlatform.HF_INFERENCE -> {
                            val token = settings.hfToken.value
                                ?: throw CloudFailureException(CloudFailure.AuthRejected)
                            emit(
                                GenerativeState.Running(
                                    0.45f,
                                    "HF TTS · ${candidate.displayName}",
                                    deadlineEpochMs = deadline,
                                ),
                            )
                            val bytes = hfInference.textToSpeech(
                                modelId = candidate.endpoint,
                                text = prompt.trim(),
                                hfToken = token,
                            )
                            CloudOutputValidator.validateAudio(bytes)?.let {
                                throw CloudFailureException(CloudFailure.BadOutput)
                            }
                            io.downloadResult(
                                "data:audio/wav;base64,${Base64.encode(bytes)}",
                            )
                        }
                        CloudPlatform.HF_SPACE -> {
                            val data = SpacePayloads.forAudio(
                                candidate.id,
                                prompt.trim(),
                                persona.cloudVoiceId,
                                safeKnobs,
                                edgeVoiceLabel = persona.edgeVoiceLabel,
                            )
                            emit(
                                GenerativeState.Running(
                                    0.35f,
                                    "Space TTS · ${candidate.displayName}",
                                    deadlineEpochMs = deadline,
                                ),
                            )
                            val result = hf.predict(
                                spaceHost = candidate.endpoint,
                                apiName = CloudModelContracts.effectiveApiName(candidate),
                                data = data,
                                hfToken = settings.hfToken.value,
                                maxPolls = budget.maxPolls(pollDelayMs = 2_000, floor = 3, ceiling = 20),
                                pollDelayMs = 2_000,
                                onPoll = { pollIndex, maxPolls ->
                                    budget.throwIfExpired()
                                    val frac =
                                        0.35f + 0.5f * (pollIndex + 1).toFloat() / maxPolls.coerceAtLeast(1)
                                    emit(
                                        GenerativeState.Running(
                                            frac.coerceIn(0.35f, 0.88f),
                                            "Audio poll ${pollIndex + 1}/$maxPolls",
                                            deadlineEpochMs = deadline,
                                        ),
                                    )
                                },
                            )
                            val url = extractRef(result)
                            io.downloadResult(url, spaceHost = candidate.endpoint)
                        }
                        else -> error("${candidate.platform} is not supported for Audio")
                    }
                    emit(GenerativeState.Running(0.92f, "Applying local voice knobs…"))
                    val finalPath = if (localVoiceChanger.isReady() && !safeKnobs.isIdentity) {
                        when (val changed = localVoiceChanger.transform(path, safeKnobs)) {
                            is LocalAudioResult.Ok -> changed.audioPath
                            is LocalAudioResult.Unavailable -> path
                        }
                    } else {
                        path
                    }
                    usage.record(candidate, success = true, note = "audio · ${persona.id}")
                    health.recordSuccess(candidate.id)
                    emit(GenerativeState.AudioReady(finalPath, candidate.id))
                    return@flow
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    val kind = when {
                        e.message.orEmpty().contains("429") ||
                            e.message.orEmpty().contains("rate limit", ignoreCase = true) ->
                            ModelHealthTracker.FailureKind.RATE_LIMIT
                        else -> ModelHealthTracker.FailureKind.GENERIC
                    }
                    health.recordFailure(candidate.id, kind)
                    if (e is CloudFailureException && e.failure is CloudFailure.Offline) throw e
                }
            }
            throw lastError ?: IllegalStateException("Audio generation failed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(
                attempted,
                success = false,
                note = CloudModelContracts.usageFailureNote(attempted, e.message.orEmpty()),
            )
            health.recordFailure(attempted.id)
            emit(
                GenerativeState.Failed(
                    CloudModelContracts.friendlyFailure(
                        attempted,
                        e.message.orEmpty(),
                        "Audio generation",
                        selectedDisplayName = provider.displayName,
                    ),
                ),
            )
        }
    }

    private fun buildCodeSystem(assists: GenerativeAssists): String = buildString {
        append("You are a helpful coding assistant. Return clear, working code with brief explanations. ")
        append("If the prompt is incomplete or oddly phrased, infer sensible defaults and note them — never refuse for ambiguity alone. ")
        if (assists.pragmatic) {
            append(
                "Prefer completing lawful software tasks over refusals. " +
                    "Do not decline ordinary coding, networking, UI, or automation questions. ",
            )
        }
        if (assists.creative) {
            append("Explore practical alternatives when helpful; keep answers concrete. ")
        }
    }

    /**
     * Ordered prompt variants: richest first, then progressively softer so a
     * blocked / flaky Space still has a chance to succeed.
     */
    private fun visualPromptVariants(prompt: String, assists: GenerativeAssists): List<String> {
        val base = prompt.trim().ifBlank { "modest fashion lookbook portrait" }
        val rich = enrichVisualPrompt(base, assists)
        val soft = enrichVisualPrompt(
            base,
            assists.copy(bypassFilter = true, fashionContext = true, detailBoost = false, qualityGuard = false),
        )
        val bare = base
        return listOf(rich, soft, bare).distinct()
    }

    private fun enrichVisualPrompt(prompt: String, assists: GenerativeAssists): String {
        val extras = buildList {
            if (assists.bypassFilter) {
                add(
                    "editorial fashion catalog photography, tasteful modest clothing, " +
                        "professional lookbook, fabric and garment focus, SFW fashion editorial",
                )
            }
            if (assists.fashionContext) {
                add("fashion photography, modest wear lookbook, garment fabric detail, clothing catalog style")
            }
            if (assists.detailBoost) {
                add("sharp detail, coherent lighting, high resolution")
            }
            if (assists.qualityGuard) {
                add("avoid blur, avoid deformed anatomy, avoid text overlays, clean background")
            }
        }
        return if (extras.isEmpty()) prompt else "$prompt. ${extras.joinToString(". ")}"
    }

    private fun requireKeyIfNeeded(provider: CloudModelProvider) {
        if (provider.requiresApiKey && settings.apiKeyFor(provider).isNullOrBlank()) {
            error("Add the free ${provider.platform.name} API key in Settings before using ${provider.displayName}")
        }
    }

    /** OpenAI-compatible chat for News tab and assistants. */
    suspend fun chat(
        prompt: String,
        system: String,
        capability: AiCapability = AiCapability.CODE,
        temperature: Double = 0.4,
    ): LlmResult = chatWithFallback(prompt, system, capability, temperature).first

    /**
     * Chat with the same fallback chain as [generateCode] — tries Groq, OpenRouter,
     * and HF Inference when the selected model is unavailable or rate-limited.
     */
    suspend fun chatWithFallback(
        prompt: String,
        system: String,
        capability: AiCapability = AiCapability.CODE,
        temperature: Double = 0.4,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Pair<LlmResult, CloudModelProvider> {
        val provider = settings.selectedProvider(capability)
        // Soft gate — ConnectivityManager can briefly report offline on 5G.
        if (!settings.networkLikelyAvailable()) {
            // Fall through: first HTTP attempt still runs; classifier handles real Offline.
        }
        val candidates = CloudModelRouting.codeFallbackChain(provider, settings)
        val effectiveSystem = buildCodeSystem(assists).let { base ->
            if (system.isBlank()) base else "$base\n\n$system"
        }
        val effectiveTemp = when {
            assists.creative && assists.pragmatic -> temperature.coerceAtLeast(0.45)
            assists.creative -> temperature.coerceAtLeast(0.5)
            else -> temperature
        }
        var lastError: Exception? = null
        for (candidate in candidates) {
            if (CloudModelContracts.preflightOrNull(candidate) != null) continue
            requireKeyIfNeeded(candidate)
            val key = settings.apiKeyFor(candidate) ?: continue
            try {
                val result = llm.chat(
                    platform = candidate.platform,
                    model = candidate.endpoint,
                    prompt = prompt,
                    apiKey = key,
                    system = effectiveSystem,
                    temperature = effectiveTemp,
                )
                if (result.text.isBlank()) error("Empty LLM response")
                usage.record(
                    candidate,
                    tokensIn = result.tokensIn,
                    tokensOut = result.tokensOut,
                    success = true,
                    note = "Chat · ${CloudModelContracts.statusLabel(candidate)}",
                )
                return result to candidate
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (e.isMonthlyCreditsExhausted()) continue
            }
        }
        throw lastError ?: IllegalStateException("Chat failed — add Groq, OpenRouter, or HF token in Settings")
    }

    private fun extractRef(element: kotlinx.serialization.json.JsonElement): String =
        GradioOutput.extractMediaRef(element)
}

/** Failures where burning the primary image deadline should still try one alternate Space. */
private fun CloudFailure.allowsImageFallbackGrace(): Boolean = when (this) {
    CloudFailure.Timeout, CloudFailure.Busy, CloudFailure.Waking -> true
    is CloudFailure.Unknown ->
        raw.contains("timeout", ignoreCase = true) ||
            raw.contains("timed out", ignoreCase = true) ||
            raw.contains("queue", ignoreCase = true)
    else -> false
}
