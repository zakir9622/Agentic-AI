package com.zakir.vestra.ui.screens.changelog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zakir.vestra.BuildConfig
import com.zakir.vestra.shared.content.ChangelogParser
import com.zakir.vestra.shared.content.ChangelogRelease
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassEmptyState
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSectionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A4.10 — release history read directly from the app's own `CHANGELOG.md` (bundled as a real
 * asset at build time, not a hand-maintained parallel list — see `copyChangelogAsset` in
 * `composeApp/build.gradle.kts`), so this screen can never drift out of sync with what actually
 * shipped. lookbookweb's `changelog.tsx` also carries "upgrade instructions" — adapted here
 * since this app ships as an APK, not a git pull.
 */
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val releases by produceState(initialValue = emptyList<ChangelogRelease>()) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val text = context.assets.open("CHANGELOG.md").bufferedReader().readText()
                ChangelogParser.parse(text)
            }.getOrDefault(emptyList())
        }
    }

    GlassScreen(
        title = "Changelog",
        subtitle = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        onBack = onBack,
    ) {
        GlassCard {
            GlassSectionLabel("INSTALL THE LATEST RELEASE")
            Text(
                "This app ships as an APK, not a git checkout — download the newest release " +
                    "from the project's GitHub Releases page and install it manually (or through " +
                    "your preferred sideload manager). The app never auto-updates itself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))

        if (releases.isEmpty()) {
            GlassEmptyState(message = "Changelog unavailable.")
        } else {
            releases.forEachIndexed { index, release ->
                ReleaseCard(release)
                if (index != releases.lastIndex) Spacer(Modifier.height(14.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReleaseCard(release: ChangelogRelease) {
    GlassCard(modifier = Modifier.testTag(TestTags.changelogRelease(release.version))) {
        GlassSectionLabel(release.version)
        Spacer(Modifier.height(6.dp))
        Text(
            release.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
