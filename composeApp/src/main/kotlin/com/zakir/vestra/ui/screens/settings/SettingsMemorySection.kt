package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.chat.MemoryFact
import com.zakir.vestra.shared.chat.MemoryRepository
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.VestraSwitch
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.VestraColors

/**
 * "What the assistant remembers" (Part B.1) — the view/edit/delete panel for durable facts
 * News/Chat has extracted from past conversations via the local chat model. Nothing shown here
 * ever left the device: [MemoryRepository] stores facts in the same local `Settings` backing
 * every other repository in this app.
 */
internal fun LazyListScope.settingsMemorySection(appSettings: AppSettings, memory: MemoryRepository) {
    item(key = "chat-memory") {
        val enabled by appSettings.memoryEnabled.collectAsState()
        val facts by memory.facts.collectAsState()
        GlassCard {
            GlassSectionLabel("WHAT THE ASSISTANT REMEMBERS")
            Spacer(Modifier.height(4.dp))
            Text(
                "Durable facts News/Chat picks up from your conversations — never sent off " +
                    "this device — re-used to make future replies more relevant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Remember new facts", style = MaterialTheme.typography.titleSmall)
                VestraSwitch(
                    checked = enabled,
                    onCheckedChange = appSettings::setMemoryEnabled,
                    modifier = Modifier.testTag(TestTags.MEMORY_ENABLED_SWITCH),
                )
            }
            if (facts.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Nothing remembered yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TestTags.MEMORY_EMPTY_STATE),
                )
            } else {
                Spacer(Modifier.height(10.dp))
                facts.forEach { fact ->
                    MemoryFactRow(fact = fact, onRemove = { memory.remove(fact.id) })
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(
                    onClick = { memory.clear() },
                    modifier = Modifier.testTag(TestTags.MEMORY_CLEAR_ALL_BUTTON),
                ) { Text("Clear all") }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@androidx.compose.runtime.Composable
private fun MemoryFactRow(fact: MemoryFact, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.memoryFactRow(fact.id))
            .clip(shape)
            .background(VestraColors.GlassFill)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            fact.text,
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.Ink,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.testTag(TestTags.memoryFactRemove(fact.id)),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Forget this", tint = VestraColors.InkMuted)
        }
    }
}
