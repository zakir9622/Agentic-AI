package com.zakir.vestra.shared.cloud

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Runtime model health with exponential cooldown after failures.
 */
class ModelHealthTracker(
    private val settings: Settings,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun recordSuccess(providerId: String) {
        val entry = load(providerId).copy(
            consecutiveFailures = 0,
            lastSuccessMs = nowMs(),
            cooldownUntilMs = 0L,
        )
        save(providerId, entry)
    }

    fun recordFailure(providerId: String) {
        val prev = load(providerId)
        val failures = prev.consecutiveFailures + 1
        val cooldownMs = cooldownFor(failures)
        save(
            providerId,
            prev.copy(
                consecutiveFailures = failures,
                lastFailureMs = nowMs(),
                cooldownUntilMs = nowMs() + cooldownMs,
            ),
        )
    }

    fun isInCooldown(providerId: String): Boolean =
        load(providerId).cooldownUntilMs > nowMs()

    fun cooldownRemainingMs(providerId: String): Long =
        (load(providerId).cooldownUntilMs - nowMs()).coerceAtLeast(0L)

    fun observedLabel(providerId: String): String? {
        val entry = load(providerId)
        val now = nowMs()
        return when {
            entry.cooldownUntilMs > now -> {
                val mins = ((entry.cooldownUntilMs - now) / 60_000L).coerceAtLeast(1L)
                "Cooling down · ${mins}m"
            }
            entry.consecutiveFailures >= 3 -> "Degraded · ${entry.consecutiveFailures} recent failures"
            entry.lastSuccessMs > 0L -> {
                val mins = ((now - entry.lastSuccessMs) / 60_000L).coerceAtLeast(0L)
                if (mins <= 2) "Ready · verified just now" else "Ready · verified ${mins}m ago"
            }
            else -> null
        }
    }

    fun effectiveSupport(provider: CloudModelProvider): ModelSupportLevel {
        val static = CloudModelContracts.forProvider(provider).support
        if (static == ModelSupportLevel.UNSUPPORTED) return static
        if (isInCooldown(provider.id)) return ModelSupportLevel.DEGRADED
        if (load(provider.id).consecutiveFailures >= 3) return ModelSupportLevel.DEGRADED
        return static
    }

    companion object {
        private const val KEY = "model_health_v1"

        fun cooldownFor(consecutiveFailures: Int): Long = when (consecutiveFailures) {
            1 -> 30_000L
            2 -> 120_000L
            3 -> 600_000L
            4 -> 1_800_000L
            else -> 3_600_000L
        }
    }

    @Serializable
    private data class HealthEntry(
        val consecutiveFailures: Int = 0,
        val lastFailureMs: Long = 0L,
        val lastSuccessMs: Long = 0L,
        val cooldownUntilMs: Long = 0L,
    )

    private fun load(providerId: String): HealthEntry =
        settings.getStringOrNull("$KEY:$providerId")?.let { raw ->
            runCatching { json.decodeFromString<HealthEntry>(raw) }.getOrNull()
        } ?: HealthEntry()

    private fun save(providerId: String, entry: HealthEntry) {
        settings.putString("$KEY:$providerId", json.encodeToString(entry))
    }

    private fun nowMs(): Long = System.currentTimeMillis()
}
