package com.zakir.vestra.shared.engine.litert

import com.russhwolf.settings.Settings
import java.io.File

/**
 * Durable counterpart to [LiteRtLmEngineCache]'s in-memory `failed` map — that map resets on
 * every cold start, so a deterministically-broken pack (e.g. a vision-encoder signature the SDK
 * rejects) still silently re-attempts native init once per app launch before self-correcting.
 *
 * Keyed on the spec plus the model file's on-disk byte length, so a future redownload/republish
 * (which will almost certainly change the file's size) automatically invalidates the stale
 * "known broken" verdict — no explicit eviction plumbing needed on top of the existing
 * [LiteRtLmEngineCache.evictModelPath]/[LiteRtLmEngineCache.clearAll] calls.
 */
class LiteRtLmFailureStore(private val settings: Settings) {

    fun isKnownFailed(spec: LiteRtLmEngineCache.EngineSpec): String? {
        val raw = settings.getStringOrNull(keyFor(spec)) ?: return null
        val (lengthPart, reason) = raw.split("|", limit = 2).let {
            it.getOrElse(0) { "" } to it.getOrElse(1) { "" }
        }
        val currentLength = runCatching { File(spec.modelPath).length() }.getOrDefault(-1L)
        if (lengthPart.toLongOrNull() != currentLength) {
            // File on disk no longer matches what failed before — don't trust a stale verdict.
            settings.remove(keyFor(spec))
            return null
        }
        return reason.ifBlank { null }
    }

    fun recordFailure(spec: LiteRtLmEngineCache.EngineSpec, reason: String) {
        val length = runCatching { File(spec.modelPath).length() }.getOrDefault(-1L)
        settings.putString(keyFor(spec), "$length|$reason")
    }

    private fun keyFor(spec: LiteRtLmEngineCache.EngineSpec): String =
        "litert_lm_failure_v1:${spec.modelPath}|gpu=${spec.useGpu}|vision=${spec.visionEnabled}" +
            "|audio=${spec.audioEnabled}|tools=${spec.toolsKey}"
}
