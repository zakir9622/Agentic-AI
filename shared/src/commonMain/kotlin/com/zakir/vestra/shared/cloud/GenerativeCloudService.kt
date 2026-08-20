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

    fun generateImage(prompt: String, referenceUri: String?): Flow<GenerativeState> = flow {
        val capability = if (referenceUri.isNullOrBlank()) AiCapability.IMAGE_GEN else AiCapability.IMAGE_EDIT
        val provider = settings.selectedProvider(capability)
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            emit(GenerativeState.Running(0.25f, "Generating image…"))
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for images"
            }
            val data = buildList {
                if (!referenceUri.isNullOrBlank()) {
                    val bytes = io.loadImageBytes(referenceUri)
                        ?: error("Couldn't read the reference image")
                    add(io.toDataUrl(bytes))
                }
                add(prompt.trim())
            }
            val result = hf.predict(provider.endpoint, provider.apiName, data, settings.hfToken.value)
            val url = extractRef(result)
            emit(GenerativeState.Running(0.85f, "Downloading…"))
            val path = io.downloadResult(url, spaceHost = provider.endpoint)
            usage.record(provider, success = true, note = "Image · ${prompt.take(80)}")
            emit(GenerativeState.ImageReady(path, provider.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(provider, success = false, note = e.message.orEmpty())
            emit(GenerativeState.Failed(e.message ?: "Image generation failed"))
        }
    }

    fun generateCode(prompt: String): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.CODE)
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            emit(GenerativeState.Running(0.3f, "Thinking…"))
            val key = settings.apiKeyFor(provider) ?: error("API key required for ${provider.displayName}")
            val result = llm.chat(provider.platform, provider.endpoint, prompt.trim(), key)
            usage.record(
                provider,
                tokensIn = result.tokensIn,
                tokensOut = result.tokensOut,
                success = true,
                note = "Code · ${prompt.take(80)}",
            )
            emit(GenerativeState.CodeReady(result.text, result.tokensIn, result.tokensOut, provider.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(provider, success = false, note = e.message.orEmpty())
            emit(GenerativeState.Failed(e.message ?: "Code generation failed"))
        }
    }

    fun generateVideo(prompt: String): Flow<GenerativeState> = flow {
        val provider = settings.selectedProvider(AiCapability.VIDEO)
        emit(GenerativeState.Preparing("Connecting to ${provider.displayName}"))
        try {
            requireKeyIfNeeded(provider)
            require(settings.networkLikelyAvailable()) { "No internet connection" }
            emit(GenerativeState.Running(0.2f, "Rendering video (this can take a minute)…"))
            require(provider.platform == CloudPlatform.HF_SPACE) {
                "Only free Hugging Face Spaces are supported for video"
            }
            val result = hf.predict(
                provider.endpoint,
                provider.apiName,
                listOf(prompt.trim()),
                settings.hfToken.value,
            )
            val url = extractRef(result)
            emit(GenerativeState.Running(0.9f, "Downloading video…"))
            val path = io.downloadResult(url, spaceHost = provider.endpoint)
            usage.record(provider, success = true, note = "Video · ${prompt.take(80)}")
            emit(GenerativeState.VideoReady(path, provider.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            usage.record(provider, success = false, note = e.message.orEmpty())
            emit(GenerativeState.Failed(e.message ?: "Video generation failed"))
        }
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
