package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.safety.SafetyPreset
import com.zakir.vestra.shared.safety.SafetyPresets
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Prompt-level safety preset picker — exact-match of lookbookweb's safety controls
 * (`src/lib/safety.ts` + its Settings/Studio safety row), adapted for on-device generation.
 * The active preset's guard clause is appended to every image-generation prompt before it
 * reaches the real generator (`GenerativeViewModel.generateImage`) — a real behavior change,
 * not a cosmetic setting.
 */
internal fun LazyListScope.settingsSafetySection(appSettings: AppSettings) {
    item(key = "safety-presets") {
        val selectedId by appSettings.safetyPresetId.collectAsState()
        GlassCard {
            GlassSectionLabel("IMAGE GENERATION SAFETY")
            Spacer(Modifier.height(4.dp))
            Text(
                "Applied to every image prompt before it's sent to a model — local or cloud.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            SafetyPresets.ALL.forEachIndexed { index, preset ->
                SafetyPresetRow(
                    preset = preset,
                    selected = preset.id == selectedId,
                    onSelect = { appSettings.setSafetyPresetId(preset.id) },
                    testTag = TestTags.safetyPreset(preset.id),
                )
                if (index != SafetyPresets.ALL.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun SafetyPresetRow(
    preset: SafetyPreset,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(shape)
            .background(if (selected) VestraColors.Accent.copy(alpha = 0.14f) else VestraColors.GlassFill)
            .border(
                1.dp,
                if (selected) VestraColors.Accent.copy(alpha = 0.55f) else VestraColors.GlassBorder,
                shape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.label, style = MaterialTheme.typography.titleSmall, color = VestraColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                preset.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = VestraColors.Accent)
        }
    }
}
