package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zakir.vestra.shared.chat.MemoryRepository
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.storage.DurableStorage
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground

/**
 * The sub-pages Settings was split into.
 *
 * Settings had grown to one scroll carrying appearance, storage, permissions, four hub rows,
 * safety presets, four API-key fields with their own test buttons, durable-storage status, about,
 * help, changelog, diagnostics and the memory list — roughly six screen-heights on a phone, with
 * the hub rows stranded in the middle of it. Grouping models and telemetry out earlier proved the
 * shape; this finishes the job, so the hub is a list of destinations and nothing else.
 *
 * Each page reuses the *same* `LazyListScope` section function the hub used to call inline, so
 * this split moved zero setting logic — a page here is a scaffold plus one call. That is
 * deliberate: a regrouping that also rewrote the settings would have been impossible to review
 * against "nothing was lost", and the previous restructure did quietly orphan two controls.
 */
@Composable
private fun SettingsSubPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
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
                    title = title,
                    subtitle = subtitle,
                    navigation = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

/** Theme selection. One control, but it belongs beside the others rather than above them. */
@Composable
fun AppearanceScreen(appSettings: AppSettings, onBack: () -> Unit) {
    val appearance by appSettings.appearanceMode.collectAsState()
    SettingsSubPage(title = "Appearance", subtitle = "Theme", onBack = onBack) {
        settingsThemeSection(appSettings = appSettings, appearance = appearance)
    }
}

/** Safety presets applied to every image and video prompt, plus the two phrasing assists. */
@Composable
fun SafetyScreen(appSettings: AppSettings, onBack: () -> Unit) {
    SettingsSubPage(title = "Safety & content", subtitle = "Prompt guards · phrasing", onBack = onBack) {
        settingsSafetySection(appSettings = appSettings)
    }
}

/**
 * Caches, the usage ledger, exports, permissions and durable-storage status.
 *
 * `permissionEpoch` is re-read on every resume rather than once: the user can revoke camera or
 * storage access from system settings while this page is backgrounded, and a stale "Allowed"
 * would be a lie the page has no other way to correct.
 */
@Composable
fun StoragePrivacyScreen(
    appSettings: AppSettings,
    usageLedger: UsageLedger,
    onOpenApiKeys: () -> Unit,
    onBack: () -> Unit,
) {
    var clearingCache by remember { mutableStateOf(false) }
    var permissionEpoch by remember { mutableIntStateOf(0) }
    var durableReady by remember { mutableStateOf(DurableStorage.hasAllFilesAccess()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionEpoch += 1
                durableReady = DurableStorage.hasAllFilesAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsSubPage(title = "Storage & privacy", subtitle = "Caches · permissions", onBack = onBack) {
        settingsStoragePermissionsSection(
            clearingCache = clearingCache,
            onClearingCache = { clearingCache = it },
            usageLedger = usageLedger,
            permissionEpoch = permissionEpoch,
            onOpenApiKeys = onOpenApiKeys,
        )
        settingsDurableStatusSection(appSettings = appSettings, durableReady = durableReady)
    }
}

/** The durable facts the assistant carries between conversations, and the switch that stops it. */
@Composable
fun MemoryScreen(appSettings: AppSettings, memory: MemoryRepository, onBack: () -> Unit) {
    SettingsSubPage(title = "Memory", subtitle = "What the assistant remembers", onBack = onBack) {
        settingsMemorySection(appSettings = appSettings, memory = memory)
    }
}

/** Version, help, privacy, changelog and diagnostics. */
@Composable
fun AboutScreen(
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenDiagnostics: (() -> Unit)?,
    onBack: () -> Unit,
) {
    SettingsSubPage(title = "About & help", subtitle = "Version · docs · diagnostics", onBack = onBack) {
        settingsGeneralSection(
            onOpenHelp = onOpenHelp,
            onOpenPrivacy = onOpenPrivacy,
            onOpenChangelog = onOpenChangelog,
            onOpenDiagnostics = onOpenDiagnostics,
        )
    }
}
