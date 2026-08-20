package com.zakir.vestra.ui.screens.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.domain.PackState
import com.zakir.vestra.shared.domain.PackStatus
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.packs.PackDownloadWorker
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import kotlinx.coroutines.launch

/** Model pack management: install/update/remove the engine packs. */
@Composable
fun PacksScreen(
    packManager: ModelPackManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val states by packManager.states.collectAsState()
    val lastError by packManager.lastError.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { packManager.refresh() }

    GlassScreen(title = "Model packs", subtitle = "Open-source · on-device", onBack = onBack) {
        Text(
            "These open-source packs turn your phone into a local AI device. Once installed, try-on needs no internet and uses \$0 tokens.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (states.isEmpty()) {
            GlassCard {
                Text(
                    lastError?.let { "Couldn't load the pack catalog — $it" }
                        ?: "Couldn't load the pack catalog. Connect once to fetch it — installed packs keep working offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { scope.launch { packManager.refresh() } }) { Text("Retry") }
            }
        }

        states.values.forEach { state ->
            Spacer(Modifier.height(12.dp))
            PackCard(
                state = state,
                onInstall = { PackDownloadWorker.enqueue(context, state.pack.id) },
                onUninstall = { packManager.uninstall(state.pack.id) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PackCard(
    state: PackState,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
) {
    GlassCard {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(state.pack.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                formatBytes(state.pack.totalBytes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            state.pack.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.pack.devOnly) {
            Spacer(Modifier.height(8.dp))
            Text(
                "DEV ONLY — " + (
                    state.pack.licenseNotice
                        ?: "Research-licensed weights. Private testing only; never ships in the published app."
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(12.dp))
        when (state.status) {
            PackStatus.NOT_INSTALLED -> Button(onClick = onInstall) { Text("Download") }
            PackStatus.UPDATE_AVAILABLE -> Button(onClick = onInstall) { Text("Update") }
            PackStatus.DOWNLOADING -> {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PackStatus.INSTALLED -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Installed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                OutlinedButton(onClick = onUninstall) { Text("Remove") }
            }
            PackStatus.INCOMPATIBLE -> Text(
                "This device doesn't meet the pack's requirements.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1e6)
    else -> "%.0f KB".format(bytes / 1e3)
}
