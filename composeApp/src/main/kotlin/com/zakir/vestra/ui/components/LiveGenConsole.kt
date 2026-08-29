package com.zakir.vestra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.time.formatDurationSeconds
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.VestraColors

@Composable
fun LiveGenConsole(lines: List<String>, generationStartedAtMs: Long? = null) {
    if (lines.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    GlassCard(modifier = Modifier.testTag(TestTags.LIVE_CONSOLE)) {
        val header = if (generationStartedAtMs != null) {
            val elapsed = ((System.currentTimeMillis() - generationStartedAtMs) / 1_000L).coerceAtLeast(0L)
            "LIVE · ${formatDurationSeconds(elapsed)}"
        } else {
            "LIVE"
        }
        GlassSectionLabel(header)
        val scroll = rememberScrollState()
        LaunchedEffect(lines.size) {
            scroll.animateScrollTo(scroll.maxValue)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(scroll),
        ) {
            lines.takeLast(24).forEach { line ->
                Text(
                    "· $line",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Docked, collapsed-by-default live log — a single line (the latest event) sitting right above
 * the composer instead of a persistent scrollable card competing with the result region above
 * it. Tap to expand the same scrollback [LiveGenConsole] shows, tap again to collapse.
 */
@Composable
fun DockedLiveLog(
    lines: List<String>,
    generationStartedAtMs: Long? = null,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    // Ticks once a second so the header keeps counting during a long quiet stage instead of
    // freezing until the next log line happens to arrive.
    var tick by remember(generationStartedAtMs) { mutableStateOf(0) }
    LaunchedEffect(generationStartedAtMs) {
        if (generationStartedAtMs == null) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(1_000)
            tick++
        }
    }
    val header = if (generationStartedAtMs != null) {
        @Suppress("UNUSED_EXPRESSION") tick
        val elapsed = ((System.currentTimeMillis() - generationStartedAtMs) / 1_000L).coerceAtLeast(0L)
        "LIVE · ${formatDurationSeconds(elapsed)}"
    } else {
        "LIVE"
    }
    GlassCard(
        modifier = modifier
            .testTag(TestTags.LIVE_CONSOLE)
            .clickable { expanded = !expanded },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                header,
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.Accent,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                lines.last(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse log" else "Expand log",
                tint = VestraColors.InkMuted,
                modifier = Modifier.height(18.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            val scroll = rememberScrollState()
            LaunchedEffect(lines.size) {
                scroll.animateScrollTo(scroll.maxValue)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(scroll)
                    .padding(top = 8.dp),
            ) {
                lines.takeLast(24).forEach { line ->
                    Text(
                        "· $line",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
