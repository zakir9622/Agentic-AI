package com.zakir.vestra.ui.screens.help

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.cloud.ModelSupportLevel
import com.zakir.vestra.shared.content.HelpCatalog
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassPrimaryButton
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.StatusChip
import com.zakir.vestra.ui.components.StatusTone
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.openAppSystemSettings
import com.zakir.vestra.ui.util.openNotificationSettings

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) { HelpCatalog.search(query) }
    val grouped = remember(results) { results.groupBy { it.category } }

    GlassScreen(
        title = LookbookCopy.STUDIO_HELP,
        subtitle = "Guides · permissions · recovery",
        onBack = onBack,
    ) {
        GlassCard {
            GlassSectionLabel("PRODUCT")
            Text(LookbookCopy.PRODUCT_NAME, style = MaterialTheme.typography.titleLarge)
            Text(
                LookbookCopy.PRODUCT_BLURB,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search Help and FAQ" },
            singleLine = true,
            label = { Text("Search Help") },
            placeholder = { Text("e.g. token, pack, queue, camera") },
        )

        Spacer(Modifier.height(12.dp))
        GlassCard {
            GlassSectionLabel("QUICK ACTIONS")
            // One filled button, not four. These are all shortcuts of equal rank, and rendering
            // them as four stacked accent gradients made the least important block on the screen
            // the loudest thing on it. Only the in-app destination stays primary; the three that
            // leave for system settings or a mail client are secondary, which is also the honest
            // signal that they hand you off somewhere else.
            GlassPrimaryButton(
                text = LookbookCopy.ACTION_OPEN_SETTINGS,
                onClick = onOpenSettings,
            )
            Spacer(Modifier.height(8.dp))
            GlassSecondaryButton(
                text = "App permission settings",
                onClick = { context.openAppSystemSettings() },
            )
            Spacer(Modifier.height(8.dp))
            GlassSecondaryButton(
                text = "Notification settings",
                onClick = { context.openNotificationSettings() },
            )
            Spacer(Modifier.height(8.dp))
            GlassSecondaryButton(
                text = LookbookCopy.ACTION_CONTACT_SUPPORT,
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:${LookbookCopy.SUPPORT_EMAIL}")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "The Lookbook support")
                    }
                    runCatching { context.startActivity(intent) }
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        GlassCard {
            GlassSectionLabel("CLOUD MODEL READINESS")
            Text(
                "Live app contracts for curated free models. Prefer Ready; Degraded may queue; Unsupported is blocked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            // Name and status on one line, schema note demoted beneath it. As a single joined
            // string these twenty-two rows were an unbroken grey wall in which the status — the
            // only part that tells you whether to pick the model — was the hardest word to find.
            HelpCatalog.modelReadiness().forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = VestraColors.GlassBorder,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = VestraColors.Ink,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(row.statusLabel, row.support.tone())
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    row.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (grouped.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "No topics match “$query”. Try pack, token, or permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            grouped.forEach { (category, topics) ->
                Spacer(Modifier.height(16.dp))
                GlassSectionLabel(category.uppercase())
                topics.forEach { topic ->
                    GlassCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(topic.question, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            topic.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** The readiness levels the Help list shows, mapped onto the app's good/warn/bad triple. */
private fun ModelSupportLevel.tone(): StatusTone = when (this) {
    ModelSupportLevel.READY -> StatusTone.GOOD
    ModelSupportLevel.DEGRADED -> StatusTone.WARN
    ModelSupportLevel.UNSUPPORTED -> StatusTone.BAD
}
