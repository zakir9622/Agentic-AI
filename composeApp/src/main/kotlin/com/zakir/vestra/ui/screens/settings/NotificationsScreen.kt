package com.zakir.vestra.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.hasPostNotificationsPermission
import com.zakir.vestra.ui.util.openNotificationSettings

/**
 * Notification preferences.
 *
 * Settings previously showed notification state as one read-only status line with no way to act
 * on it, and the only thing that ever posted was the pack-download worker. Both halves changed:
 * generations now post their own results ([com.zakir.vestra.notify.GenerationNotifier]), and this
 * screen is where the OS grant and the per-category preferences are controlled.
 *
 * The two are deliberately separate. A preference being on does not imply the OS grant, and the
 * screen says so rather than showing switches that silently do nothing.
 */
@Composable
fun NotificationsScreen(appSettings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    // Re-read the grant on every resume: the user can revoke it from system settings while this
    // screen is backgrounded, and a stale "Allowed" here would be a lie.
    var permissionEpoch by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionEpoch++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val granted = remember(permissionEpoch) { context.hasPostNotificationsPermission() }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }
    var showRationale by remember { mutableStateOf(false) }

    val notifyComplete by appSettings.notifyOnGenerationComplete.collectAsState()
    val notifyFailed by appSettings.notifyOnGenerationFailed.collectAsState()
    val notifyPacks by appSettings.notifyOnPackDownload.collectAsState()

    GlassScreen(
        title = "Notifications",
        subtitle = "Permissions · alerts",
        onBack = onBack,
    ) {
        GlassCard(modifier = Modifier.testTag(TestTags.NOTIFICATIONS_PERMISSION_CARD)) {
            GlassSectionLabel("SYSTEM PERMISSION")
            Text(
                if (granted) "Allowed" else "Blocked",
                style = MaterialTheme.typography.titleMedium,
                color = if (granted) VestraColors.SaffronDeep else VestraColors.Danger,
            )
            Text(
                if (granted) {
                    "Android is letting the app post notifications. The switches below decide which ones."
                } else {
                    "Android is blocking every notification from this app, so the switches below " +
                        "have no effect until you allow them."
                },
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.sm))
            // No SDK guard: minSdk is 35, so POST_NOTIFICATIONS is always a runtime permission
            // here. An `SDK_INT >= 33` check is dead code and lint flags it as such.
            if (!granted) {
                GlassSecondaryButton(text = "Allow notifications", onClick = { showRationale = true })
                Spacer(Modifier.height(SpacingTokens.xs))
            }
            GlassSecondaryButton(
                text = "Open system notification settings",
                onClick = { context.openNotificationSettings() },
                modifier = Modifier.testTag(TestTags.NOTIFICATIONS_SYSTEM_SETTINGS_BUTTON),
            )
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        GlassCard {
            GlassSectionLabel("GENERATIONS")
            Text(
                "A cloud Space can queue for minutes and a cold on-device model takes tens of " +
                    "seconds, so leaving the app mid-generation is normal. These only fire while " +
                    "the app is in the background — a result already on screen doesn't need an alert.",
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.sm))
            NotificationToggle(
                title = "When a generation finishes",
                description = "Images, clips, audio and answers.",
                checked = notifyComplete,
                enabled = granted,
                onCheckedChange = appSettings::setNotifyOnGenerationComplete,
                testTag = TestTags.NOTIFY_GENERATION_COMPLETE_SWITCH,
            )
            NotificationToggle(
                title = "When a generation fails",
                description = "So a queued Space that times out doesn't go unnoticed.",
                checked = notifyFailed,
                enabled = granted,
                onCheckedChange = appSettings::setNotifyOnGenerationFailed,
                testTag = TestTags.NOTIFY_GENERATION_FAILED_SWITCH,
            )
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        GlassCard {
            GlassSectionLabel("MODEL PACKS")
            NotificationToggle(
                title = "Download progress",
                description = "Multi-gigabyte packs download in the background and report progress here.",
                checked = notifyPacks,
                enabled = granted,
                onCheckedChange = appSettings::setNotifyOnPackDownload,
                testTag = TestTags.NOTIFY_PACK_DOWNLOAD_SWITCH,
            )
        }
        Spacer(Modifier.height(SpacingTokens.xxl))
    }

    if (showRationale) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(LookbookCopy.PERM_NOTIFICATIONS_TITLE) },
            text = { Text(LookbookCopy.PERM_NOTIFICATIONS_BODY) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showRationale = false
                        requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                ) { Text("Continue") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRationale = false }) {
                    Text("Not now")
                }
            },
        )
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = VestraColors.Ink)
            Text(description, style = MaterialTheme.typography.bodySmall, color = VestraColors.InkMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // Disabled without the OS grant rather than hidden: the preference is still real and
            // persists, it just cannot take effect yet, and hiding it would make the screen look
            // like it has fewer features than it does.
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
        )
    }
    Spacer(Modifier.height(SpacingTokens.xs))
}
