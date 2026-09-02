package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Which model each kind of generation uses by default.
 *
 * These six selections already existed and were already persisted — [AppSettings] has had one
 * `*ProviderId` flow per capability all along. What they lacked was a place that presents them as
 * one decision; they were five `ExposedDropdownMenuBox`es buried mid-scroll in the main Settings
 * screen, each showing cloud models only. Here each row opens the same [ModelPickerSheet] the
 * composer uses, so on-device and cloud models are chosen from one list with full metadata.
 */
@Composable
fun DefaultModelsScreen(
    appSettings: AppSettings,
    freeCloudDiscovery: FreeCloudDiscovery?,
    packManager: ModelPackManager?,
    onBack: () -> Unit,
) {
    // Chat has no capability of its own — it routes through CODE, matching ChatViewModel and the
    // home screen's picker. TRY_ON is omitted because nothing navigates to try-on today.
    val rows = listOf(
        ModalityRow("Chat", "Conversation and questions", AiCapability.CODE),
        ModalityRow("Image", "New images from a prompt", AiCapability.IMAGE_GEN),
        ModalityRow("Image edit", "Changes to a reference photo", AiCapability.IMAGE_EDIT),
        ModalityRow("Video", "Short generated clips", AiCapability.VIDEO),
        ModalityRow("Code", "Programming answers", AiCapability.CODE),
        ModalityRow("Audio", "Speech from text", AiCapability.AUDIO),
    )

    var picking by remember { mutableStateOf<ModalityRow?>(null) }
    val packStates by packManager?.states?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }

    // Every *ProviderId flow is collected so a selection made in the sheet re-renders its row
    // immediately; `selectionId()` alone reads a value Compose has no reason to recompose on.
    val imageGenId by appSettings.imageGenProviderId.collectAsState()
    val imageEditId by appSettings.imageEditProviderId.collectAsState()
    val codeId by appSettings.codeProviderId.collectAsState()
    val videoId by appSettings.videoProviderId.collectAsState()
    val audioId by appSettings.audioProviderId.collectAsState()

    fun selectionFor(capability: AiCapability): String = when (capability) {
        AiCapability.IMAGE_GEN -> imageGenId
        AiCapability.IMAGE_EDIT -> imageEditId
        AiCapability.CODE -> codeId
        AiCapability.VIDEO -> videoId
        AiCapability.AUDIO -> audioId
        AiCapability.TRY_ON -> appSettings.selectionId(AiCapability.TRY_ON)
    }

    fun labelFor(id: String, capability: AiCapability): String =
        LocalModelCatalog.byId(id)?.displayName
            ?: CloudModelCatalog.byId(id)?.displayName
            ?: appSettings.selectedProvider(capability).displayName

    GlassScreen(
        title = "Default models",
        subtitle = "One choice per kind of generation",
        onBack = onBack,
    ) {
        GlassCard {
            GlassSectionLabel("DEFAULTS")
            Text(
                "The composer's model chip still overrides any of these for a single message — " +
                    "this is what it starts from.",
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.sm))
            rows.forEach { row ->
                val id = selectionFor(row.capability)
                DefaultModelRow(
                    row = row,
                    modelLabel = labelFor(id, row.capability),
                    isLocal = id.startsWith("local-"),
                    onClick = { picking = row },
                )
                Spacer(Modifier.height(SpacingTokens.xs))
            }
        }
        Spacer(Modifier.height(SpacingTokens.xxl))
    }

    picking?.let { row ->
        val capability = row.capability
        val models = remember(capability, freeCloudDiscovery) {
            freeCloudDiscovery?.selectable(appSettings, capability)
                ?: CloudModelCatalog.forCapability(capability)
        }
        val onDeviceEntries = remember(capability, packStates) {
            LocalModelCatalog.forStudioPicker(capability).map { entry ->
                val packReady = entry.packId?.let { packStates[it]?.isReady() == true } == true
                OnDevicePickerEntry(
                    id = entry.id,
                    displayName = entry.displayName,
                    detail = entry.testingNote,
                    ready = LocalModelCatalog.studioEntryReady(entry, packReady),
                    statusLabel = LocalModelCatalog.studioStatusLabel(entry, packReady),
                    sizeLabel = entry.approxSizeLabel,
                    license = entry.license,
                    offlineAfterInstall = entry.offlineAfterInstall,
                )
            }
        }
        ModelPickerSheet(
            title = "${row.title} model",
            models = models,
            selectedId = selectionFor(capability),
            onDeviceEntries = onDeviceEntries,
            health = appSettings.modelHealth,
            hasCredential = { appSettings.cloudUsable(it) },
            onSelect = { chosen -> appSettings.setProviderFor(capability, chosen.id) },
            onSelectDevice = { entry ->
                if (entry.ready) appSettings.setLocalGenerator(capability, entry.id)
            },
            onDismiss = { picking = null },
        )
    }
}

private data class ModalityRow(val title: String, val description: String, val capability: AiCapability)

private fun AppSettings.setProviderFor(capability: AiCapability, id: String) = when (capability) {
    AiCapability.IMAGE_GEN -> setImageGenProvider(id)
    AiCapability.IMAGE_EDIT -> setImageEditProvider(id)
    AiCapability.CODE -> setCodeProvider(id)
    AiCapability.VIDEO -> setVideoProvider(id)
    AiCapability.AUDIO -> setAudioProvider(id)
    AiCapability.TRY_ON -> setCloudProvider(id)
}

@Composable
private fun DefaultModelRow(row: ModalityRow, modelLabel: String, isLocal: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.defaultModelRow(row.capability.name))
            .clip(RoundedCornerShape(RadiusTokens.md))
            .background(VestraColors.GlassFill)
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.title,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(ControlTokens.dot)
                        .clip(CircleShape)
                        .background(if (isLocal) VestraColors.SaffronDeep else VestraColors.Accent),
                )
                Spacer(Modifier.size(SpacingTokens.xxs + 2.dp))
                Text(
                    modelLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
