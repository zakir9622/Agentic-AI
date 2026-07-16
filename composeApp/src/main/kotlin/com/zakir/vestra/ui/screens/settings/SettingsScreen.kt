package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.settings.AppSettings

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    onBack: () -> Unit,
) {
    val selectedTier by appSettings.engineTier.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "GENERATION ENGINE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            EngineTier.entries.forEach { tier ->
                val availability = if (tier == EngineTier.AUTO) {
                    Availability.Ready
                } else {
                    engineRouter.availability(tier)
                }
                val enabled = availability == Availability.Ready
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = tier == selectedTier,
                            enabled = enabled,
                            onClick = { appSettings.setEngineTier(tier) },
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = tier == selectedTier,
                        enabled = enabled,
                        onClick = { appSettings.setEngineTier(tier) },
                    )
                    Column {
                        Text(tier.label(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = tier.description(availability),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun EngineTier.label(): String = when (this) {
    EngineTier.AUTO -> "Auto"
    EngineTier.LITE -> "Lite — on device"
    EngineTier.PRO -> "Pro — on device"
    EngineTier.CLOUD -> "Cloud"
}

private fun EngineTier.description(availability: Availability): String {
    val base = when (this) {
        EngineTier.AUTO -> "Best available on-device engine. Never uses the network."
        EngineTier.LITE -> "Fast try-on that works offline on every supported phone."
        EngineTier.PRO -> "Highest on-device realism. Needs the Pro model pack and a flagship chip."
        EngineTier.CLOUD -> "Maximum quality. Your images are uploaded for processing and deleted after."
    }
    return when (availability) {
        Availability.Ready -> base
        is Availability.Unavailable -> when (availability.reason) {
            UnavailableReason.PACK_NOT_INSTALLED -> "$base\nModel pack not installed."
            UnavailableReason.DEVICE_NOT_CAPABLE -> "$base\nThis device doesn't meet the requirements."
            UnavailableReason.OFFLINE -> "$base\nYou're offline."
            UnavailableReason.NOT_CONFIGURED -> "$base\nComing soon."
        }
    }
}
