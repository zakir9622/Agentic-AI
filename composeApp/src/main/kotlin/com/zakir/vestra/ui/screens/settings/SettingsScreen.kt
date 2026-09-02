package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground

/**
 * Settings: a list of destinations, and nothing else.
 *
 * This screen has been through two rounds of the same mistake. It began as one flowing scroll
 * carrying every setting the app has. The first pass moved everything model-shaped to
 * [ModelsScreen] / [DefaultModelsScreen], telemetry to [ApiMonitorScreen] and alerts to
 * [NotificationsScreen] — and then left appearance, storage, permissions, safety, four API-key
 * fields, durable-storage status, about and memory inline *around* the four new hub rows. The
 * result read worse than before: the same six screen-heights, now with navigation stranded in
 * the middle of it.
 *
 * So the rule here is absolute rather than a judgement call: **the hub contains hub rows.** A
 * setting that needs a control lives on a page. That is what makes the screen scannable — every
 * row is the same shape, the list fits without scrolling on a phone, and adding a tenth setting
 * costs one row instead of another screen-height.
 *
 * Ordering is by how often a setting is touched, not by category: models and keys are what a new
 * install needs, defaults and notifications are early-tuning, and appearance through about are
 * set-once.
 */
@Composable
@Suppress("LongParameterList")
fun SettingsScreen(
    packManager: ModelPackManager,
    onOpenModels: () -> Unit,
    onOpenApiKeys: () -> Unit,
    onOpenDefaultModels: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenApiMonitor: () -> Unit,
    onOpenSafety: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenMemory: (() -> Unit)?,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { packManager.refresh() }

    SpatialBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item(key = "top") {
                GlassTopBar(
                    title = LookbookCopy.STUDIO_SETTINGS,
                    subtitle = "Models · keys · privacy",
                    navigation = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            item(key = "generation") {
                GlassSectionLabel("GENERATION")
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Memory,
                    title = "Models",
                    description = "Cloud services and on-device packs.",
                    onClick = onOpenModels,
                    testTag = TestTags.SETTINGS_ROW_MODELS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Key,
                    title = "API keys",
                    description = "Hugging Face, Groq, OpenRouter and Gemini credentials.",
                    onClick = onOpenApiKeys,
                    testTag = TestTags.SETTINGS_ROW_API_KEYS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Tune,
                    title = "Default models",
                    description = "Which model Chat, Image, Video, Code and Audio each use.",
                    onClick = onOpenDefaultModels,
                    testTag = TestTags.SETTINGS_ROW_DEFAULT_MODELS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Security,
                    title = "Safety & content",
                    description = "Prompt guards and phrasing assists for Image and Video.",
                    onClick = onOpenSafety,
                    testTag = TestTags.SETTINGS_ROW_SAFETY,
                )
                Spacer(Modifier.height(20.dp))
            }

            item(key = "app") {
                GlassSectionLabel("APP")
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    description = "What the app is allowed to tell you, and when.",
                    onClick = onOpenNotifications,
                    testTag = TestTags.SETTINGS_ROW_NOTIFICATIONS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance",
                    description = "Light, dark, or follow the system.",
                    onClick = onOpenAppearance,
                    testTag = TestTags.SETTINGS_ROW_APPEARANCE,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Analytics,
                    title = "API monitor",
                    description = "Requests, tokens, latency and estimated spend.",
                    onClick = onOpenApiMonitor,
                    testTag = TestTags.SETTINGS_ROW_API_MONITOR,
                )
                Spacer(Modifier.height(20.dp))
            }

            item(key = "data") {
                GlassSectionLabel("YOUR DATA")
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Storage,
                    title = "Storage & privacy",
                    description = "Caches, usage ledger, exports and permissions.",
                    onClick = onOpenStorage,
                    testTag = TestTags.SETTINGS_ROW_STORAGE,
                )
                if (onOpenMemory != null) {
                    Spacer(Modifier.height(10.dp))
                    SettingsNavRow(
                        icon = Icons.Outlined.Psychology,
                        title = "Memory",
                        description = "Durable facts the assistant reuses between chats.",
                        onClick = onOpenMemory,
                        testTag = TestTags.SETTINGS_ROW_MEMORY,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "About & help",
                    description = "Version, FAQ, privacy policy, changelog and diagnostics.",
                    onClick = onOpenAbout,
                    testTag = TestTags.SETTINGS_ROW_ABOUT,
                )
            }
        }
    }
}
