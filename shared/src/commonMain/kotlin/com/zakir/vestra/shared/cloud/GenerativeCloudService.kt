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
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for images"
            }
            val variants = visualPromptVariants(prompt, assists)
            var lastError: Exception? = null
            for ((index, variant) in variants.withIndex()) {
                emit(
                    GenerativeState.Running(
                        0.2f + index * 0.15f,
                        if (index == 0) "Generating image…" else "Retrying with softer prompt…",
                    ),
                )
                try {
                    val data = buildList {
                        if (!referenceUri.isNullOrBlank()) {
                            val bytes = io.loadImageBytes(referenceUri)
                                ?: error("Couldn't read the reference image")
                            add(io.toDataUrl(bytes))
                        }
                        add(variant)
                    }
                    val result = hf.predict(provider.endpoint, provider.apiName, data, settings.hfToken.value)
                    val url = extractRef(result)
                    emit(GenerativeState.Running(0.85f, "Downloading…"))
                    val path = io.downloadResult(url, spaceHost = provider.endpoint)
                    usage.record(provider, success = true, note = "Image · ${prompt.take(80)}")
                    emit(GenerativeState.ImageReady(path, provider.id))
                    return@flow
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("Image generation failed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(provider, success = false, note = e.message.orEmpty())
            emit(GenerativeState.Failed(friendlyError(e, "Image generation")))
        }
    }

    fun generateCode(
        prompt: String,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.CODE)
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            emit(GenerativeState.Running(0.3f, "Thinking…"))
            val key = settings.apiKeyFor(provider) ?: error("API key required for ${provider.displayName}")
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
            var result: LlmResult? = null
            for ((i, attempt) in attempts.withIndex()) {
                if (i > 0) emit(GenerativeState.Running(0.35f + i * 0.1f, "Retrying…"))
                try {
                    result = llm.chat(provider.platform, provider.endpoint, attempt, key, system, temperature)
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                }
            }
            val done = result ?: throw (lastError ?: IllegalStateException("Empty LLM response"))
            usage.record(
                provider,
                tokensIn = done.tokensIn,
                tokensOut = done.tokensOut,
                success = true,
                note = "Code · ${prompt.take(80)}",
            )
            emit(GenerativeState.CodeReady(done.text, done.tokensIn, done.tokensOut, provider.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(provider, success = false, note = e.message.orEmpty())
            emit(GenerativeState.Failed(friendlyError(e, "Code generation")))
        }
    }

    fun generateVideo(
        prompt: String,
        assists: GenerativeAssists = GenerativeAssists(),
    ): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.VIDEO)
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for video"
            }
            val variants = visualPromptVariants(prompt, assists)
            var lastError: Exception? = null
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
                        spaceHost = provider.endpoint,
                        apiName = provider.apiName,
                        data = listOf(variant),
                        hfToken = settings.hfToken.value,
                        maxPolls = 180,
                        pollDelayMs = 3_000,
                    )
                    val url = extractRef(result)
                    emit(GenerativeState.Running(0.9f, "Downloading video…"))
                    val path = io.downloadResult(url, spaceHost = provider.endpoint)
                    usage.record(provider, success = true, note = "Video · ${prompt.take(80)}")
                    emit(GenerativeState.VideoReady(path, provider.id))
                    return@flow
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("Video generation failed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(provider, success = false, note = e.message.orEmpty())
            emit(GenerativeState.Failed(friendlyError(e, "Video generation")))
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

    private fun friendlyError(e: Exception, label: String): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("LinkedHashMap", ignoreCase = true) ||
                raw.contains("Kotlin reflection", ignoreCase = true) ->
                "$label failed to encode the request. Update the app and retry."
            raw.contains("timeout", ignoreCase = true) || raw.contains("timed out", ignoreCase = true) ->
                "$label timed out on the free tier. Tap Retry off-peak, or Force stop and try a faster model."
            raw.contains("401") || raw.contains("Unauthorized", ignoreCase = true) ->
                "API key rejected. Re-save your free token in Settings."
            raw.contains("429") || raw.contains("rate", ignoreCase = true) ->
                "Free-tier rate limit hit. Wait a minute or switch model in Settings."
            raw.contains("NSFW", ignoreCase = true) ||
                raw.contains("safety", ignoreCase = true) ||
                raw.contains("content policy", ignoreCase = true) ||
                raw.contains("blocked", ignoreCase = true) ->
                "$label was blocked by the free model filter. Enable Bypass filter assist, rephrase as fashion/editorial, or switch model in Settings."
            raw.contains("No internet", ignoreCase = true) ->
                "No internet connection. Reconnect and retry."
            raw.isBlank() -> "$label failed. Tap Retry — assists will soften the next attempt."
            else -> raw.take(280)
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
