package com.zakir.vestra.shared.cloud

import com.zakir.vestra.shared.time.EpochClock
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One model as a provider's own `/models` endpoint describes it — not as this app's curated
 * [CloudModelCatalog] does.
 *
 * The distinction matters: a Groq key unlocks ~20 chat models, of which the catalog has a
 * payload contract for one. Listing only the contracted model would hide what the key actually
 * buys; listing everything as selectable would defer the failure to generation time. So every
 * fetched model is surfaced, and [runnable] decides whether it can be picked.
 */
@Serializable
data class DirectoryModel(
    /** Provider-native model id, e.g. "llama-3.3-70b-versatile" or "models/gemini-2.5-flash". */
    val id: String,
    val displayName: String,
    val description: String = "",
    /** Who publishes it, when the provider says. */
    val owner: String = "",
    /** Total context window in tokens; null when the provider does not report one. */
    val contextTokens: Int? = null,
    /** Max tokens the provider will generate in one response, when reported. */
    val maxOutputTokens: Int? = null,
    /** What it accepts and returns, e.g. ["text", "image"]. Empty when unreported. */
    val inputModalities: List<String> = emptyList(),
    val outputModalities: List<String> = emptyList(),
    /**
     * Human-readable price, already formatted — "Free" for `:free` OpenRouter models and for
     * every provider whose free tier this app restricts itself to.
     */
    val pricingLabel: String = "Free",
    /**
     * True when this app has a working route for the model — a curated [CloudModelProvider]
     * whose endpoint matches. False rows still render, greyed, with [unsupportedReason].
     */
    val runnable: Boolean = false,
    /** The curated provider id to select when [runnable]; null otherwise. */
    val catalogProviderId: String? = null,
    /** Why the row cannot be selected. Empty when [runnable]. */
    val unsupportedReason: String = "",
) {
    /** Compact "128K ctx · text→text" style line for a list row. Empty when nothing is known. */
    val specLine: String
        get() = buildList {
            contextTokens?.let { add("${formatTokens(it)} ctx") }
            if (inputModalities.isNotEmpty() || outputModalities.isNotEmpty()) {
                val inputs = inputModalities.ifEmpty { listOf("text") }.joinToString("/")
                val outputs = outputModalities.ifEmpty { listOf("text") }.joinToString("/")
                add("$inputs→$outputs")
            }
            if (owner.isNotBlank()) add(owner)
            add(pricingLabel)
        }.joinToString(" · ")
}

private fun formatTokens(tokens: Int): String = when {
    tokens >= 1_000_000 -> "${tokens / 1_000_000}M"
    tokens >= 1_000 -> "${tokens / 1_000}K"
    else -> tokens.toString()
}

/** Outcome of a directory refresh. Distinguishes "no key" and "rejected" from "empty list". */
sealed interface DirectoryResult {
    data class Loaded(val models: List<DirectoryModel>, val fetchedAtMs: Long) : DirectoryResult
    data object NoKey : DirectoryResult
    data class Unauthorized(val detail: String) : DirectoryResult
    data class Failed(val detail: String) : DirectoryResult
}

/**
 * Live per-provider model listing.
 *
 * Two of these endpoints were already being called before this class existed —
 * [ProviderConnectivityChecker] GETs Groq's `/v1/models` and Gemini's `/v1beta/models` purely to
 * check a key and throws the body away. This reads the same responses.
 *
 * Conventions follow [FreeCloudDiscovery]: constructor-injected [HttpClient], a per-class lenient
 * [Json], `bodyAsText()` plus an explicit parse rather than `body<T>()`, and every parse wrapped
 * so a schema drift downgrades to "couldn't list" instead of taking a screen down.
 */
class ProviderModelDirectory(
    private val http: HttpClient,
    private val clock: EpochClock = EpochClock.System,
    private val ttlMs: Long = DEFAULT_TTL_MS,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = mutableMapOf<CloudPlatform, DirectoryResult.Loaded>()

    /** Last successful listing for [platform], if one is still inside the TTL. */
    fun cached(platform: CloudPlatform): DirectoryResult.Loaded? =
        cache[platform]?.takeIf { clock.nowMs() - it.fetchedAtMs < ttlMs }

    /**
     * Fetch [platform]'s model list. Returns the cached listing when one is fresh and [force] is
     * false, so opening a provider page repeatedly does not re-hit the network.
     */
    suspend fun refresh(
        platform: CloudPlatform,
        apiKey: String?,
        force: Boolean = false,
    ): DirectoryResult {
        if (!force) cached(platform)?.let { return it }
        if (apiKey.isNullOrBlank()) return DirectoryResult.NoKey

        val result = runCatching {
            when (platform) {
                CloudPlatform.GROQ -> fetchGroq(apiKey)
                CloudPlatform.OPENROUTER -> fetchOpenRouter(apiKey)
                CloudPlatform.GEMINI -> fetchGemini(apiKey)
                CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> fetchHuggingFace(apiKey)
            }
        }.getOrElse { err -> DirectoryResult.Failed(err.message?.take(160) ?: "Network error") }

        if (result is DirectoryResult.Loaded) cache[platform] = result
        return result
    }

    /** Drop every cached listing — used when a key is replaced or cleared. */
    fun invalidate(platform: CloudPlatform? = null) {
        if (platform == null) cache.clear() else cache.remove(platform)
    }

    // ── Per-provider fetches ────────────────────────────────────────────────────────────

    /**
     * Groq's OpenAI-compatible list: `data[]` of `{id, owned_by, context_window,
     * max_completion_tokens, active}`. No pricing — Groq's free tier is rate-limited, not billed.
     */
    private suspend fun fetchGroq(apiKey: String): DirectoryResult {
        val response = http.get("https://api.groq.com/openai/v1/models") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }
        failureOrNull(response.status)?.let { return it }
        val models = dataArray(response.bodyAsText()).mapNotNull { obj ->
            val id = obj.str("id") ?: return@mapNotNull null
            // `active: false` models are listed but rejected at inference time.
            if (obj["active"]?.jsonPrimitive?.content == "false") return@mapNotNull null
            decorate(
                DirectoryModel(
                    id = id,
                    displayName = id.substringAfterLast('/'),
                    owner = obj.str("owned_by").orEmpty(),
                    contextTokens = obj.int("context_window"),
                    maxOutputTokens = obj.int("max_completion_tokens"),
                    inputModalities = listOf("text"),
                    outputModalities = listOf("text"),
                    pricingLabel = "Free tier",
                ),
                CloudPlatform.GROQ,
            )
        }
        return DirectoryResult.Loaded(models.sortedBy { it.displayName }, clock.nowMs())
    }

    /**
     * OpenRouter's list is the richest of the four: `data[]` of `{id, name, description,
     * context_length, architecture{input_modalities, output_modalities}, pricing{prompt,
     * completion}}`. Only `:free` models are surfaced — the app is free-tier-only by policy
     * ([CloudModelCatalog]'s init block rejects any paid entry).
     */
    private suspend fun fetchOpenRouter(apiKey: String): DirectoryResult {
        val response = http.get("https://openrouter.ai/api/v1/models") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }
        failureOrNull(response.status)?.let { return it }
        val models = dataArray(response.bodyAsText()).mapNotNull { obj ->
            val id = obj.str("id") ?: return@mapNotNull null
            if (!id.endsWith(":free")) return@mapNotNull null
            val architecture = obj["architecture"] as? JsonObject
            decorate(
                DirectoryModel(
                    id = id,
                    displayName = obj.str("name") ?: id.substringAfterLast('/'),
                    description = obj.str("description").orEmpty().take(220),
                    owner = id.substringBefore('/'),
                    contextTokens = obj.int("context_length"),
                    inputModalities = architecture.stringList("input_modalities"),
                    outputModalities = architecture.stringList("output_modalities"),
                    pricingLabel = "Free",
                ),
                CloudPlatform.OPENROUTER,
            )
        }
        return DirectoryResult.Loaded(models.sortedBy { it.displayName }, clock.nowMs())
    }

    /**
     * Gemini's ListModels: `models[]` of `{name, displayName, description, inputTokenLimit,
     * outputTokenLimit, supportedGenerationMethods}`. Models that cannot generate content at all
     * (embedding-only, for instance) are dropped — they are not a choice a user could make here.
     */
    private suspend fun fetchGemini(apiKey: String): DirectoryResult {
        val response = http.get("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
        failureOrNull(response.status)?.let { return it }
        val root = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonObject }
            .getOrNull() ?: return DirectoryResult.Failed("Unexpected response shape")
        val array = root["models"]?.jsonArray ?: return DirectoryResult.Loaded(emptyList(), clock.nowMs())
        val models = array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj.str("name") ?: return@mapNotNull null
            val methods = obj.stringList("supportedGenerationMethods")
            if (methods.isNotEmpty() && methods.none { it.contains("generateContent", ignoreCase = true) }) {
                return@mapNotNull null
            }
            decorate(
                DirectoryModel(
                    id = name.removePrefix("models/"),
                    displayName = obj.str("displayName") ?: name.removePrefix("models/"),
                    description = obj.str("description").orEmpty().take(220),
                    owner = "Google",
                    contextTokens = obj.int("inputTokenLimit"),
                    maxOutputTokens = obj.int("outputTokenLimit"),
                    inputModalities = listOf("text"),
                    outputModalities = listOf("text"),
                    pricingLabel = "Free tier",
                ),
                CloudPlatform.GEMINI,
            )
        }
        return DirectoryResult.Loaded(models.sortedBy { it.displayName }, clock.nowMs())
    }

    /**
     * HF's router list: `data[]` of `{id, owned_by, providers[]}`. Unlike the other three this
     * covers image and audio models as well as text, so modality is inferred from the curated
     * catalog match rather than asserted here.
     */
    private suspend fun fetchHuggingFace(token: String): DirectoryResult {
        val response = http.get("https://router.huggingface.co/v1/models") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        failureOrNull(response.status)?.let { return it }
        val models = dataArray(response.bodyAsText()).mapNotNull { obj ->
            val id = obj.str("id") ?: return@mapNotNull null
            val providers = (obj["providers"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.str("provider") }
                ?.distinct()
                .orEmpty()
            decorate(
                DirectoryModel(
                    id = id,
                    displayName = id.substringAfterLast('/'),
                    description = if (providers.isEmpty()) "" else "Served by ${providers.joinToString(", ")}",
                    owner = obj.str("owned_by") ?: id.substringBefore('/'),
                    contextTokens = (obj["providers"] as? JsonArray)
                        ?.firstNotNullOfOrNull { (it as? JsonObject)?.int("context_length") },
                    pricingLabel = "Free tier",
                ),
                CloudPlatform.HF_INFERENCE,
            )
        }
        return DirectoryResult.Loaded(models.sortedBy { it.displayName }, clock.nowMs())
    }

    // ── Shared helpers ──────────────────────────────────────────────────────────────────

    /**
     * Attach runnability by matching the provider-native id against the curated catalog's
     * endpoints. A match means [GenerativeCloudService] has a payload builder for it; anything
     * else is listed for transparency but cannot be selected.
     */
    private fun decorate(model: DirectoryModel, platform: CloudPlatform): DirectoryModel {
        val match = CloudModelCatalog.providers.firstOrNull { provider ->
            provider.endpoint.equals(model.id, ignoreCase = true) && provider.platform.matches(platform)
        }
        if (match == null) {
            return model.copy(
                runnable = false,
                unsupportedReason = "No request format for this model yet — it is listed so you " +
                    "can see what your key covers.",
            )
        }
        val support = CloudModelContracts.forProvider(match).support
        if (support == ModelSupportLevel.UNSUPPORTED) {
            return model.copy(
                runnable = false,
                catalogProviderId = match.id,
                unsupportedReason = "Needs inputs this app has no UI for yet.",
            )
        }
        return model.copy(runnable = true, catalogProviderId = match.id)
    }

    /** HF's two platforms share one key and one router, so either catalog entry counts. */
    private fun CloudPlatform.matches(other: CloudPlatform): Boolean {
        val hf = setOf(CloudPlatform.HF_SPACE, CloudPlatform.HF_INFERENCE)
        return this == other || (this in hf && other in hf)
    }

    private fun failureOrNull(status: HttpStatusCode): DirectoryResult? = when {
        status.isSuccess() -> null
        status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden ->
            DirectoryResult.Unauthorized("Key rejected (${status.value}) — check it in Settings")
        status == HttpStatusCode.TooManyRequests ->
            DirectoryResult.Failed("Rate limited (429) — try again shortly")
        else -> DirectoryResult.Failed("Provider returned ${status.value}")
    }

    /** `{"data": [...]}` or a bare array — both shapes appear across these four providers. */
    private fun dataArray(raw: String): List<JsonObject> = runCatching {
        when (val root = json.parseToJsonElement(raw)) {
            is JsonObject -> root["data"]?.jsonArray
            is JsonArray -> root
            else -> null
        }?.mapNotNull { it as? JsonObject }.orEmpty()
    }.getOrDefault(emptyList())

    private fun JsonObject?.str(key: String): String? =
        this?.get(key)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }

    private fun JsonObject?.int(key: String): Int? =
        this?.get(key)?.jsonPrimitive?.content?.toIntOrNull()

    private fun JsonObject?.stringList(key: String): List<String> =
        (this?.get(key) as? JsonArray)?.mapNotNull { it.jsonPrimitive.content.takeIf(String::isNotBlank) }
            .orEmpty()

    companion object {
        /** Matches [GradioSchemaClient]'s schema cache — model lists change on the order of days. */
        const val DEFAULT_TTL_MS: Long = 60L * 60L * 1000L
    }
}
