package com.zakir.vestra.shared.safety

import com.zakir.vestra.shared.cloud.CloudConfig
import com.zakir.vestra.shared.wardrobe.TextFileStore
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AI-content report intake (Play AI-Generated Content policy requires an
 * in-app reporting affordance). Reports queue locally so reporting works
 * offline, then flush to the `report` Edge Function whenever we're online.
 */
class ReportQueue(
    private val store: TextFileStore,
    private val http: HttpClient,
    private val config: CloudConfig,
    private val appVersion: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun submit(reason: ReportReason, details: String?, engineTier: String?) {
        val report = PendingReport(
            reason = reason.wire,
            details = details?.take(2000),
            engineTier = engineTier,
            appVersion = appVersion,
        )
        persist(load() + report)
        flush()
    }

    /** Attempts delivery of everything queued; failures stay queued. */
    suspend fun flush() {
        if (!config.isConfigured) return
        val pending = load()
        if (pending.isEmpty()) return
        val remaining = pending.filterNot { report -> send(report) }
        persist(remaining)
    }

    private suspend fun send(report: PendingReport): Boolean = runCatching {
        http.post(config.reportUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
            header("apikey", config.anonKey)
            setBody(json.encodeToString(report))
        }.status.isSuccess()
    }.getOrDefault(false)

    private fun load(): List<PendingReport> =
        store.read(QUEUE_FILE)?.let { content ->
            runCatching { json.decodeFromString<List<PendingReport>>(content) }.getOrNull()
        } ?: emptyList()

    private fun persist(reports: List<PendingReport>) {
        store.write(QUEUE_FILE, json.encodeToString(reports))
    }

    private companion object {
        const val QUEUE_FILE = "report_queue.json"
    }
}

enum class ReportReason(val wire: String, val label: String) {
    SEXUAL("sexual_content", "Sexual content"),
    VIOLENCE("violence", "Violent content"),
    LIKENESS("likeness_misuse", "Misuses someone's likeness"),
    OTHER("other", "Something else"),
}

@Serializable
private data class PendingReport(
    val reason: String,
    val details: String? = null,
    @SerialName("engine_tier") val engineTier: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
)
