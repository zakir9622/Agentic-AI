package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

sealed interface GenerativeState {
    data class Preparing(val message: String) : GenerativeState
    data class Running(val fraction: Float, val stage: String) : GenerativeState
    data class ImageReady(val path: String, val providerId: String) : GenerativeState
    data class VideoReady(val path: String, val providerId: String) : GenerativeState
    data class CodeReady(val text: String, val tokensIn: Int, val tokensOut: Int, val providerId: String) : GenerativeState
    data class Failed(val message: String) : GenerativeState
}

/**
 * Free-tier generative service: HF Spaces for image/video, Groq/HF/OpenRouter-free for code.
 */
class GenerativeCloudService(
    private val http: HttpClient,
    private val io: CloudImageIo,
    private val settings: AppSettings,
    private val usage: UsageLedger,
) {
    private val hf = HfGradioClient(http)
    private val llm = LlmClient(http)

    fun generateImage(
        prompt: String,
        referenceUri: String?,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val capability = if (referenceUri.isNullOrBlank()) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT
        val provider = settings.selectedProvider(capability)
        // Errors are reported against the model that actually ran, which is not the selected
        // one once the fallback chain kicks in.
        var attempted = provider
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            CloudModelContracts.preflightOrNull(provider)?.let { error(it) }
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for images"
            }
            // Free ZeroGPU Spaces run out of quota without warning, so a healthy sibling Space
            // finishes the job instead of handing the user a dead end.
            val candidates = CloudModelRouting.fallbackChain(provider, capability)
            val referenceDataUrl = referenceUri?.takeIf { it.isNotBlank() }?.let {
                val bytes = io.loadImageBytes(it) ?: error("Couldn't read the reference image")
                io.toDataUrl(bytes)
            }
            val variants = visualPromptVariants(prompt, assists)
            var lastError: Exception? = null
            for ((modelIndex, candidate) in candidates.withIndex()) {
                attempted = candidate
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.3f,
                            "${provider.displayName} is busy — trying ${candidate.displayName}…",
                        ),
                    )
                }
                for ((index, variant) in variants.withIndex()) {
                    emit(
                        GenerativeState.Running(
                            0.2f + index * 0.15f,
                            if (index == 0) "Generating image…" else "Retrying with softer prompt…",
                        ),
                    )
                    try {
                        val data = if (referenceDataUrl != null) {
                            SpacePayloads.forImageEdit(candidate.id, variant, referenceDataUrl)
                        } else {
                            SpacePayloads.forImageGen(candidate.id, variant)
                        }
                        val result = hf.predict(
                            candidate.endpoint,
                            CloudModelContracts.effectiveApiName(candidate),
                            data,
                            settings.hfToken.value,
                        )
                        val url = extractRef(result)
                        emit(GenerativeState.Running(0.85f, "Downloading…"))
                        val path = io.downloadResult(url, spaceHost = candidate.endpoint)
                        usage.record(
                            candidate,
                            success = true,
                            note = "Image · ${CloudModelContracts.statusLabel(candidate)} · ${prompt.take(80)}",
                        )
                        emit(GenerativeState.ImageReady(path, candidate.id))
                        return@flow
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        // The GPU allowance is account-wide, so no other Space can serve this.
                        if (e.isAccountQuotaExhausted()) throw e
                    }
                }
            }
            throw lastError ?: IllegalStateException("Image generation failed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(
                attempted,
                success = false,
                note = CloudModelContracts.usageFailureNote(attempted, e.message.orEmpty()),
            )
            emit(
                GenerativeState.Failed(
                    CloudModelContracts.friendlyFailure(attempted, e.message.orEmpty(), "Image generation"),
                ),
            )
        }
    }

    private fun Exception.isAccountQuotaExhausted(): Boolean {
        val msg = message.orEmpty()
        return msg.contains("quota exceeded", ignoreCase = true) ||
            msg.contains("ZeroGPU quota", ignoreCase = true) ||
            msg.contains("exceeded your free ZeroGPU", ignoreCase = true)
    }

    fun generateCode(
        prompt: String,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.CODE)
        var attempted = provider
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            require(settings.networkLikelyAvailable()) { "No internet connection" }
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
            for ((modelIndex, candidate) in candidates.withIndex()) {
                attempted = candidate
                CloudModelContracts.preflightOrNull(candidate)?.let { error(it) }
                requireKeyIfNeeded(candidate)
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.25f,
                            "${provider.displayName} unavailable — trying ${candidate.displayName}…",
                        ),
                    )
                } else {
                    emit(GenerativeState.Running(0.3f, "Thinking…"))
                }
                val key = settings.apiKeyFor(candidate) ?: error("API key required for ${candidate.displayName}")
                var result: LlmResult? = null
                for ((i, attempt) in attempts.withIndex()) {
                    if (i > 0) emit(GenerativeState.Running(0.35f + i * 0.1f, "Retrying…"))
                    try {
                        result = llm.chat(candidate.platform, candidate.endpoint, attempt, key, system, temperature)
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                    }
                }
                if (result != null) {
                    usage.record(
                        candidate,
                        tokensIn = result.tokensIn,
                        tokensOut = result.tokensOut,
                        success = true,
                        note = "Code · ${CloudModelContracts.statusLabel(candidate)} · ${prompt.take(80)}",
                    )
                    emit(GenerativeState.CodeReady(result.text, result.tokensIn, result.tokensOut, candidate.id))
                    return@flow
                }
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
            emit(GenerativeState.Failed(CloudModelContracts.friendlyFailure(attempted, e.message.orEmpty(), "Code generation")))
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
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for video"
            }
            val candidates = CloudModelRouting.fallbackChain(provider, AiCapability.VIDEO)
            val variants = visualPromptVariants(prompt, assists)
            var lastError: Exception? = null
            for ((modelIndex, candidate) in candidates.withIndex()) {
                attempted = candidate
                CloudModelContracts.preflightOrNull(candidate)?.let { error(it) }
                requireKeyIfNeeded(candidate)
                if (modelIndex > 0) {
                    emit(
                        GenerativeState.Running(
                            0.2f,
                            "${provider.displayName} is busy — trying ${candidate.displayName}…",
                        ),
                    )
                }
                for ((index, variant) in variants.withIndex()) {
                    emit(
                        GenerativeState.Running(
                            0.15f + index * 0.15f,
                            if (index == 0) {
                                "Rendering video (this can take a minute)…"
                            } else {
                                "Retrying with softer prompt…"
                            },
                        ),
                    )
                    try {
                        val result = hf.predict(
                            spaceHost = candidate.endpoint,
                            apiName = CloudModelContracts.effectiveApiName(candidate),
                            data = SpacePayloads.forVideo(candidate.id, variant),
                            hfToken = settings.hfToken.value,
                            maxPolls = 180,
                            pollDelayMs = 3_000,
                        )
                        val url = extractRef(result)
                        emit(GenerativeState.Running(0.9f, "Downloading video…"))
                        val path = io.downloadResult(url, spaceHost = candidate.endpoint)
                        usage.record(
                            candidate,
                            success = true,
                            note = "Video · ${CloudModelContracts.statusLabel(candidate)} · ${prompt.take(80)}",
                        )
                        emit(GenerativeState.VideoReady(path, candidate.id))
                        return@flow
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        if (e.isAccountQuotaExhausted()) throw e
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
            emit(GenerativeState.Failed(CloudModelContracts.friendlyFailure(attempted, e.message.orEmpty(), "Video generation")))
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

    private fun extractRef(element: kotlinx.serialization.json.JsonElement): String = when (element) {
        is JsonPrimitive -> element.content
        is kotlinx.serialization.json.JsonObject ->
            element["url"]?.jsonPrimitive?.content
                ?: element["path"]?.jsonPrimitive?.content
                ?: element["video"]?.jsonPrimitive?.content
                ?: element["image"]?.jsonPrimitive?.content
                ?: error("Unrecognized generative output")
        else -> error("Unrecognized generative output")
    }
}
