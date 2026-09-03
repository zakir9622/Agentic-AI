package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.zakir.vestra.VestraApp
import com.zakir.vestra.storage.ApiKeyDataStore
import com.zakir.vestra.ui.components.ApiUsageDashboardCard
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Cloud API and token usage, as its own Settings destination.
 *
 * This lived pinned to the top of the home screen, above an empty conversation thread — a
 * telemetry panel as the first thing a new user saw, with a second copy rendered as the
 * empty-state item below it. Both copies are gone from home; this is where it lives now.
 */
@Composable
fun ApiMonitorScreen(onBack: () -> Unit, onOpenKeys: () -> Unit) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as? VestraApp }
    val dataStore = app?.apiKeyDataStore
    // The free-tier ledger is a different count from the DataStore's token history: it tracks
    // cloud *requests* against provider quotas. It was the one thing the retired UsageScreen
    // showed that this screen did not, so it comes across rather than being dropped.
    val ledgerSummary by app?.usageLedger?.summary?.collectAsState() ?: remember { mutableStateOf(null) }
    val data by dataStore?.usageDashboardFlow?.collectAsState(initial = ApiKeyDataStore.ApiUsageDashboardData())
        ?: remember { mutableStateOf(ApiKeyDataStore.ApiUsageDashboardData()) }
    val scope = rememberCoroutineScope()

    GlassScreen(
        title = "API monitor",
        subtitle = "Requests · tokens · reliability",
        onBack = onBack,
    ) {
        // One empty state, not three. With nothing recorded the screen used to stack a
        // RELIABILITY card saying "Nothing recorded yet…", a dashboard of three zeros, and a
        // RECENT SESSION RUNS card saying "No session runs recorded yet…" — the same sentence
        // twice, around a row of noughts. An empty screen gets one invitation to act.
        if (data.totalRequests == 0) {
            GlassCard {
                GlassSectionLabel("NOTHING RECORDED YET")
                Text(
                    "Every image, clip, answer and audio run lands here with its token count, " +
                        "latency and outcome. Nothing has run on this device so far.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                )
                Spacer(Modifier.height(SpacingTokens.sm))
                GlassSecondaryButton(text = "Manage API keys", onClick = onOpenKeys)
            }
            Spacer(Modifier.height(SpacingTokens.xxl))
            return@GlassScreen
        }

        GlassCard {
            GlassSectionLabel("RELIABILITY")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                HeadlineStat(
                    // successRate is null only when totalRequests is 0, returned above —
                    // a fresh install must never read as "0% success".
                    value = data.successRate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                    label = "Succeeded",
                    modifier = Modifier.weight(1f),
                )
                HeadlineStat(
                    value = if (data.avgLatencyMs > 0) "${data.avgLatencyMs} ms" else "—",
                    label = "Avg latency",
                    modifier = Modifier.weight(1f),
                )
                HeadlineStat(
                    value = "${data.totalTokensIn}/${data.totalTokensOut}",
                    label = "Tokens in/out",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        ledgerSummary?.takeIf { it.totalRequests > 0 }?.let { summary ->
            GlassCard {
                GlassSectionLabel("FREE-TIER CLOUD REQUESTS")
                Text(
                    "${summary.totalRequests} requests · ${summary.successCount} ok · " +
                        "${summary.totalRequests - summary.successCount} failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = VestraColors.Ink,
                )
                Text(
                    "Tokens in ${summary.totalTokensIn} · out ${summary.totalTokensOut}",
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                )
            }
            Spacer(Modifier.height(SpacingTokens.sm))
        }

        ApiUsageDashboardCard(
            data = data,
            onOpenSettings = onOpenKeys,
            onClearHistory = { scope.launch { dataStore?.clearSessionUsageHistory() } },
            initiallyExpanded = true,
        )
        Spacer(Modifier.height(SpacingTokens.xxl))
    }
}

@Composable
private fun HeadlineStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = VestraColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
