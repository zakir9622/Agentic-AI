package com.zakir.vestra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zakir.vestra.storage.ApiKeyDataStore
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiUsageDashboardCard(
    data: ApiKeyDataStore.ApiUsageDashboardData,
    onOpenSettings: (() -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Box(
        modifier = modifier
            .testTag(TestTags.API_USAGE_DASHBOARD_CARD)
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusTokens.md))
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(RadiusTokens.md))
            .padding(14.dp)
            .animateContentSize(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VestraColors.Accent.copy(alpha = 0.15f))
                            .border(1.dp, VestraColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = "API Usage",
                            tint = VestraColors.Accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Cloud API & Token Monitor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = VestraColors.Ink,
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VestraColors.Accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    "DataStore",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = VestraColors.Accent,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                        Text(
                            "${data.totalRequests} total requests · ${data.totalTokens} tokens estimated",
                            style = MaterialTheme.typography.bodySmall,
                            color = VestraColors.InkMuted,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag(TestTags.API_USAGE_EXPAND_BUTTON),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                            contentDescription = if (isExpanded) "Collapse usage details" else "Expand usage details",
                            tint = VestraColors.InkMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Stats Row (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickStatPill(
                    label = "Tokens",
                    value = if (data.totalTokens > 0) String.format(Locale.getDefault(), "%,d", data.totalTokens) else "0",
                    accent = VestraColors.Accent,
                    icon = Icons.Outlined.Speed,
                    modifier = Modifier.weight(1f),
                )
                QuickStatPill(
                    label = "Runs",
                    value = "${data.totalRequests}",
                    accent = VestraColors.SaffronDeep,
                    icon = Icons.Outlined.Cloud,
                    modifier = Modifier.weight(1f),
                )
                QuickStatPill(
                    // Was hardcoded "$0.00" while `data.totalEstCostUsd` sat unread.
                    label = "Spend",
                    value = String.format(Locale.getDefault(), "$%.2f", data.totalEstCostUsd),
                    accent = VestraColors.SaffronDeep,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.weight(1f),
                )
            }

            // Visual Token Proportion Bar
            if (data.totalTokens > 0) {
                Spacer(Modifier.height(12.dp))
                TokenDistributionBar(services = data.services, totalTokens = data.totalTokens)
            }

            // Expanded content: Provider Grid & Session History
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    HorizontalDivider(color = VestraColors.GlassBorder)
                    Spacer(Modifier.height(12.dp))

                    if (data.services.isNotEmpty()) {
                    Text(
                        "CONFIGURED CLOUD SERVICES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = VestraColors.InkMuted,
                    )
                    Spacer(Modifier.height(8.dp))

                    // Two per row, never five. `weight()` children have no intrinsic minimum,
                    // so the previous `weight(1f, fill = false)` with no `maxItemsInEachRow`
                    // packed all five services onto a single line and handed each ~52dp on a
                    // 360dp phone — which is what forced "0 reqs · 0 tok" to soft-wrap one
                    // character per line. `maxItemsInEachRow` is what makes FlowRow wrap here.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                        maxItemsInEachRow = 2,
                    ) {
                        data.services.forEach { service ->
                            ServiceUsageChip(
                                service = service,
                                modifier = Modifier
                                    .testTag(TestTags.apiUsageServiceCard(service.serviceKey))
                                    .weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    }


                    // Recent Session History
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "RECENT SESSION RUNS (${data.sessionHistory.size})",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = VestraColors.InkMuted,
                        )

                        if (data.sessionHistory.isNotEmpty() && onClearHistory != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onClearHistory)
                                    .testTag(TestTags.API_USAGE_CLEAR_HISTORY_BUTTON)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Clear session usage",
                                    tint = VestraColors.InkMuted,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VestraColors.InkMuted,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (data.sessionHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VestraColors.GlassFill)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No session runs recorded yet. Generate images, video, code, or chat to track usage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VestraColors.InkMuted,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            data.sessionHistory.take(8).forEach { event ->
                                SessionEventRow(event = event, timeFormat = timeFormat)
                            }
                        }
                    }

                    if (onOpenSettings != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VestraColors.GlassFill)
                                .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenSettings)
                                .testTag(TestTags.API_USAGE_SETTINGS_BUTTON)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = VestraColors.Accent,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Manage API Keys & Quotas",
                                style = MaterialTheme.typography.labelMedium,
                                color = VestraColors.Accent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatPill(
    label: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusTokens.sm))
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(RadiusTokens.sm))
            .padding(horizontal = SpacingTokens.xs, vertical = SpacingTokens.xs),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(SpacingTokens.xxs + 2.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VestraColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // maxLines=1 with the default Clip overflow hard-cut "Total Tokens"
                // mid-glyph in the ~56dp a third-width pill leaves for text.
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TokenDistributionBar(
    services: List<ApiKeyDataStore.ServiceUsageSummary>,
    totalTokens: Int,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "TOKEN DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = VestraColors.InkMuted,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formatCount(services.sumOf { it.tokensIn })} in · " +
                    "${formatCount(services.sumOf { it.tokensOut })} out",
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(VestraColors.GlassFill),
        ) {
            Row(Modifier.fillMaxWidth().height(6.dp)) {
                val activeServices = services.filter { it.totalTokens > 0 }
                if (activeServices.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(VestraColors.Accent.copy(alpha = 0.4f)),
                    )
                } else {
                    activeServices.forEach { service ->
                        val weight = (service.totalTokens.toFloat() / totalTokens.coerceAtLeast(1)).coerceIn(0.01f, 1f)
                        val color = serviceColor(service.serviceKey)
                        Box(
                            Modifier
                                .weight(weight)
                                .height(6.dp)
                                .background(color),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceUsageChip(
    service: ApiKeyDataStore.ServiceUsageSummary,
    modifier: Modifier = Modifier,
) {
    val accent = serviceColor(service.serviceKey)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusTokens.sm))
            .background(VestraColors.GlassFill)
            .border(
                1.dp,
                if (service.isConfigured) accent.copy(alpha = 0.35f) else VestraColors.GlassBorder,
                RoundedCornerShape(RadiusTokens.sm),
            )
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.xs + 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xxs + 2.dp),
        ) {
            Box(
                Modifier
                    .size(ControlTokens.dot)
                    .clip(CircleShape)
                    .background(if (service.isConfigured) accent else VestraColors.InkMuted),
            )
            // "Google Gemini" and "Hugging Face" both wrapped to two lines here before these
            // bounds existed. Every Text in this chip is now single-line and ellipsized.
            Text(
                service.serviceName,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(SpacingTokens.xxs))
        Text(
            if (service.isConfigured) "Active" else "Key not set",
            style = MaterialTheme.typography.labelSmall,
            color = if (service.isConfigured) accent else VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SpacingTokens.xxs))
        // Metrics stack rather than sharing one row. Five unweighted Texts in a single Row was
        // the other half of the vertical-wrap bug; two short lines always fit a half-width chip.
        Text(
            "${formatCount(service.requestCount)} req · ${formatCount(service.totalTokens)} tok",
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        val rate = service.successRate
        if (rate != null) {
            Text(
                "${(rate * 100).roundToInt()}% ok · ${service.avgLatencyMs} ms avg",
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Thousands separator so a six-figure token count doesn't blow out a half-width chip. */
private fun formatCount(value: Int): String =
    if (value >= 1000) String.format(Locale.getDefault(), "%,d", value) else value.toString()

@Composable
private fun SessionEventRow(
    event: ApiKeyDataStore.SessionUsageRecord,
    timeFormat: SimpleDateFormat,
) {
    val accent = serviceColor(event.serviceKey)
    val timeStr = remember(event.timestampMs) { timeFormat.format(Date(event.timestampMs)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VestraColors.GlassFill)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (event.success) accent else VestraColors.InkMuted),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.modelName.ifBlank { event.modelId },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = VestraColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        event.capability.lowercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = VestraColors.InkMuted,
                    )
                }
                Text(
                    "$timeStr · ${event.serviceName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = VestraColors.InkMuted,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.tokensIn > 0) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = "In",
                        tint = VestraColors.InkMuted,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        "${event.tokensIn}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = VestraColors.InkMuted,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (event.tokensOut > 0) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        contentDescription = "Out",
                        tint = accent,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        "${event.tokensOut}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (event.tokensIn == 0 && event.tokensOut == 0) {
                    Text(
                        "free",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = VestraColors.SaffronDeep,
                    )
                }
            }
            if (event.latencyMs > 0) {
                Text(
                    "${event.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = VestraColors.InkMuted,
                )
            }
        }
    }
}

private fun serviceColor(serviceKey: String): Color {
    return when (serviceKey.uppercase()) {
        "GEMINI" -> VestraColors.Accent
        "HF" -> VestraColors.SaffronDeep
        "GROQ" -> VestraColors.AccentSoft
        "OPENROUTER" -> VestraColors.ModalityCode
        "ON_DEVICE" -> VestraColors.SaffronDeep
        else -> VestraColors.Accent
    }
}
