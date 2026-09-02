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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Every cloud service and on-device model in one place.
 *
 * Model configuration used to be scattered across the main Settings scroll — an engine-tier
 * dropdown and pack picker in one section, four API-key fields in another, five per-capability
 * model dropdowns in a third. This page owns all of it, and the main scroll keeps only settings
 * that aren't about models.
 *
 * Cloud rows lead to [ProviderModelsScreen], where that provider's key is entered and its live
 * `/models` list is fetched.
 */
@Composable
fun ModelsScreen(
    appSettings: AppSettings,
    packManager: ModelPackManager?,
    engineRouter: EngineRouter?,
    onOpenProvider: (CloudPlatform) -> Unit,
    onOpenPacks: () -> Unit,
    onOpenDefaults: () -> Unit,
    onBack: () -> Unit,
) {
    val selectedTier by appSettings.engineTier.collectAsState()
    val preferNnapi by appSettings.preferNnapi.collectAsState()
    val hfToken by appSettings.hfToken.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()
    val geminiKey by appSettings.geminiApiKey.collectAsState()
    val packStates by packManager?.states?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }

    val services = listOf(
        CloudServiceRow(CloudPlatform.GEMINI, "Google Gemini", "Chat · code", !geminiKey.isNullOrBlank()),
        CloudServiceRow(CloudPlatform.HF_INFERENCE, "Hugging Face", "Image · video · audio", !hfToken.isNullOrBlank()),
        CloudServiceRow(CloudPlatform.GROQ, "Groq", "Fast chat · code", !groqKey.isNullOrBlank()),
        CloudServiceRow(CloudPlatform.OPENROUTER, "OpenRouter", "Many labs, free tier", !openRouterKey.isNullOrBlank()),
    )
    val configuredCount = services.count { it.configured }

    GlassScreen(
        title = "Models",
        subtitle = "Cloud services · on-device packs",
        onBack = onBack,
    ) {
        GlassCard(onClick = onOpenDefaults) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GlassSectionLabel("DEFAULTS")
                    Text("Default model per type", style = MaterialTheme.typography.titleMedium, color = VestraColors.Ink)
                    Text(
                        "Which model Chat, Image, Video, Code and Audio each use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VestraColors.InkMuted,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = VestraColors.InkMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        GlassCard(modifier = Modifier.testTag(TestTags.MODELS_CLOUD_SECTION)) {
            GlassSectionLabel("CLOUD SERVICES")
            Text(
                if (configuredCount == 0) {
                    "No keys yet. Open a service to add one — every model this app uses is free-tier."
                } else {
                    "$configuredCount of ${services.size} configured. Open one to manage its key and browse its models."
                },
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.sm))
            services.forEach { service ->
                CloudServiceCard(service = service, onClick = { onOpenProvider(service.platform) })
                Spacer(Modifier.height(SpacingTokens.xs))
            }
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        // These two lived in the old Settings scroll's engine section. When that section moved
        // here they nearly went missing entirely — `engineTier` and `preferNnapi` still drive
        // real behaviour (which local try-on engine runs, and whether ONNX attaches NNAPI), so
        // removing their only UI would have silently frozen both at whatever they were.
        GlassCard {
            GlassSectionLabel("ON-DEVICE ENGINE")
            Text(
                "Which local engine tier runs, and how it talks to the hardware. Cloud is never " +
                    "chosen by Auto.",
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.xs))
            if (engineRouter != null) {
                EngineDropdown(
                    selected = selectedTier,
                    availability = { tier ->
                        if (tier == EngineTier.AUTO) Availability.Ready else engineRouter.availability(tier)
                    },
                    onSelect = appSettings::setEngineTier,
                )
                Spacer(Modifier.height(SpacingTokens.sm))
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Prefer NNAPI",
                        style = MaterialTheme.typography.titleSmall,
                        color = VestraColors.Ink,
                    )
                    Text(
                        // NNAPI stays opt-in while GPU/NPU/speculative decoding default on:
                        // its documented failure mode is a process-killing SIGSEGV that bypasses
                        // the fallback the others degrade through. See AppSettings' doc comments.
                        "Off by default — safer on Pixel. Turn on only if try-on is stable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VestraColors.InkMuted,
                    )
                }
                Switch(checked = preferNnapi, onCheckedChange = appSettings::setPreferNnapi)
            }
        }
        GlassCard(modifier = Modifier.testTag(TestTags.MODELS_ON_DEVICE_SECTION)) {
            GlassSectionLabel("ON-DEVICE")
            Text(
                "These run with no network once their pack is downloaded.",
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.sm))
            // Every capability's studio-selectable local models, deduplicated — the catalog
            // lists a model once per capability it serves, and a flat list here would repeat
            // e.g. SD-Turbo under both Image and Image edit.
            val localEntries = remember {
                AiCapability.entries
                    .flatMap { LocalModelCatalog.forStudioPicker(it) }
                    .distinctBy { it.id }
            }
            if (localEntries.isEmpty()) {
                Text(
                    "No on-device models available for this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                )
            }
            localEntries.forEach { entry ->
                val packReady = entry.packId?.let { packStates[it]?.isReady() == true } == true
                OnDeviceModelRow(
                    name = entry.displayName,
                    status = LocalModelCatalog.studioStatusLabel(entry, packReady),
                    detail = listOfNotNull(
                        entry.approxSizeLabel.takeIf { it.isNotBlank() },
                        entry.license.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    ready = LocalModelCatalog.studioEntryReady(entry, packReady),
                )
                Spacer(Modifier.height(SpacingTokens.xs))
            }
            Spacer(Modifier.height(SpacingTokens.xxs))
            GlassSecondaryButton(text = "Manage model packs", onClick = onOpenPacks)
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        Spacer(Modifier.height(SpacingTokens.xxl))
    }
}

private data class CloudServiceRow(
    val platform: CloudPlatform,
    val name: String,
    val description: String,
    val configured: Boolean,
)

@Composable
private fun CloudServiceCard(service: CloudServiceRow, onClick: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.md)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.modelsProviderRow(service.platform.name))
            .clip(shape)
            .background(VestraColors.GlassFill)
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Box(
            Modifier
                .size(ControlTokens.dot)
                .clip(CircleShape)
                .background(if (service.configured) VestraColors.SaffronDeep else VestraColors.InkMuted),
        )
        Column(Modifier.weight(1f)) {
            Text(
                service.name,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // Status and description share one line rather than competing for width with
                // each other and the chevron — at 360dp there is not room for two columns.
                buildString {
                    append(if (service.configured) "Key set" else "No key")
                    append(" · ")
                    append(service.description)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (service.configured) VestraColors.SaffronDeep else VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = VestraColors.InkMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun OnDeviceModelRow(name: String, status: String, detail: String, ready: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusTokens.md))
            .background(VestraColors.GlassFill)
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Box(
            Modifier
                .size(ControlTokens.dot)
                .clip(CircleShape)
                .background(if (ready) VestraColors.SaffronDeep else VestraColors.InkMuted),
        )
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
