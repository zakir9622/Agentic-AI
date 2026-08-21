package com.zakir.vestra.ui.screens.usage

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudModelContracts
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.shared.usage.displayLabel
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsageScreen(
    usage: UsageLedger,
    onBack: () -> Unit,
) {
    val summary by usage.summary.collectAsState()
    val events by usage.events.collectAsState()
    val fmt = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())
    val failCount = summary.totalRequests - summary.successCount

    GlassScreen(title = "Cloud usage", subtitle = "Free-tier request ledger", onBack = onBack) {
        GlassCard {
            GlassSectionLabel("SUMMARY")
            Text(
                "${summary.totalRequests} requests · ${summary.successCount} ok · $failCount failed",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tokens in ${summary.totalTokensIn} · out ${summary.totalTokensOut}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "All cloud models are free-tier · \$0.00 estimated spend",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { usage.clear() }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear history")
            }
        }

        if (summary.byProvider.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GlassCard {
                GlassSectionLabel("BY MODEL / SERVICE")
                summary.byProvider.values.sortedByDescending { it.requests }.forEach { p ->
                    Spacer(Modifier.height(8.dp))
                    val catalog = CloudModelCatalog.byId(p.providerId)
                    val status = catalog?.let { CloudModelContracts.statusLabel(it) }
                    Text(
                        buildString {
                            append(p.displayName)
                            if (status != null) append(" · $status")
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${p.requests} runs · ${p.tokensIn + p.tokensOut} tokens · free",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    catalog?.usageNote?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard {
            GlassSectionLabel("RECENT")
            if (events.isEmpty()) {
                Text(
                    "No cloud usage yet. Run Create, Code, Video, or Cloud try-on.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                events.take(40).forEach { e ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${fmt.format(Date(e.timestampMs))} · ${e.providerName}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val capabilityLabel = runCatching {
                        AiCapability.valueOf(e.capability).displayLabel()
                    }.getOrDefault(e.capability)
                    Text(
                        buildString {
                            append(capabilityLabel)
                            append(" · ")
                            append(e.platform)
                            if (e.tokensIn + e.tokensOut > 0) {
                                append(" · ${e.tokensIn}→${e.tokensOut} tok")
                            }
                            append(" · free")
                            if (!e.success) append(" · failed")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (e.success) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    if (e.note.isNotBlank()) {
                        Text(
                            e.note.take(280),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (e.success) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
