package com.zakir.vestra.shared.chat

import kotlin.math.ceil

/**
 * Per-model context-window token budgets, exact-port of lookbookweb's `src/lib/tokens.ts`
 * concept adapted to this app's real chat-capable models. Windows below are each model's
 * published native context length (not the extended-context figure some support via YaRN,
 * since neither this app's cloud clients nor the LiteRT-LM packs request that extension).
 * A model not in [windows] — e.g. OpenRouter's free router, which proxies to a rotating,
 * unpublished set of underlying models — gets the honest, conservative [FALLBACK_WINDOW]
 * rather than a guessed number.
 */
object ContextBudget {
    private val windows: Map<String, Int> = mapOf(
        // Local — LiteRT-LM packs (LocalModelCatalog), native context per published model card.
        "local-qwen3-06b-v1" to 32_768,
        "local-gemma-4-e2b-v1" to 32_768,
        "local-gemma-v1" to 32_768,
        // Cloud — CODE capability, the capability News/Chat dispatches through.
        "qwen25-coder-7b-hf" to 32_768,
        "qwen25-coder-hf" to 32_768,
        "llama33-70b-groq" to 128_000,
    )

    /** Used for any model id not in [windows] (e.g. `openrouter-free`'s rotating router). */
    const val FALLBACK_WINDOW: Int = 8_192

    fun windowFor(modelId: String?): Int = modelId?.let { windows[it] } ?: FALLBACK_WINDOW

    /** True when [modelId] has a real, cataloged window rather than the conservative fallback. */
    fun isKnownModel(modelId: String?): Boolean = modelId != null && windows.containsKey(modelId)

    // ~4 characters per token is the standard calibration for English-prose/code BPE tokenizers
    // absent a real tokenizer — the same approximation lookbookweb's own tokens.ts uses.
    private const val CHARS_PER_TOKEN = 4.0

    /**
     * Token estimate for [text]. Uses [tokenizer] (a real BPE encoder, e.g. a loaded
     * `BonsaiTokenizer.encode(text).size`) when the caller has one available; falls back to the
     * calibrated character heuristic otherwise. Never returns 0 for non-empty text.
     */
    fun estimateTokens(text: String, tokenizer: ((String) -> Int)? = null): Int {
        if (text.isEmpty()) return 0
        tokenizer?.let { return it(text) }
        return ceil(text.length / CHARS_PER_TOKEN).toInt().coerceAtLeast(1)
    }

    data class Budget(
        val usedTokens: Int,
        val windowTokens: Int,
        val remainingTokens: Int,
        val willTruncate: Boolean,
        val isKnownWindow: Boolean,
    )

    /**
     * Evaluates [usedTokens] (system prompt + history + the live draft, all summed by the
     * caller) against [modelId]'s window, reserving [reserveForReply] tokens of headroom for
     * the model's own response so the warning fires before a real truncation would happen, not
     * exactly at the edge.
     */
    fun evaluate(usedTokens: Int, modelId: String?, reserveForReply: Int = 512): Budget {
        val window = windowFor(modelId)
        val remaining = window - usedTokens - reserveForReply
        return Budget(
            usedTokens = usedTokens,
            windowTokens = window,
            remainingTokens = remaining,
            willTruncate = remaining < 0,
            isKnownWindow = isKnownModel(modelId),
        )
    }
}
