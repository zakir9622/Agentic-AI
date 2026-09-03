package com.zakir.vestra.ui.screens.privacy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Offline Privacy Policy — Play / enterprise reviewers can read without network.
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    GlassScreen(
        title = LookbookCopy.ACTION_OPEN_PRIVACY,
        subtitle = "On-device · free cloud optional",
        onBack = onBack,
    ) {
        GlassCard {
            GlassSectionLabel("THE LOOKBOOK")
            Text(
                "Last updated: 2026-08-21",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                PRIVACY_INTRO,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PRIVACY_SECTIONS.forEach { section ->
                Spacer(Modifier.height(16.dp))
                Text(
                    section.heading,
                    style = MaterialTheme.typography.titleSmall,
                    color = VestraColors.Ink,
                )
                Spacer(Modifier.height(6.dp))
                section.points.forEach { point ->
                    Row(Modifier.padding(bottom = 4.dp)) {
                        // A hanging indent, not a bullet glyph inside the paragraph. Run as one
                        // string, a wrapped bullet's second line started under the bullet itself,
                        // so consecutive points ran together into a block of prose.
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassSecondaryButton(
            text = "Open hosted copy (GitHub)",
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(LookbookCopy.PRIVACY_URL)),
                    )
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One headed group of the policy.
 *
 * The policy used to be a single `\n`-joined constant rendered by one `Text`. Its six section
 * headings were therefore styled exactly like the body they headed, so a legal document with
 * real structure rendered as an unbroken grey wall. The wording below is unchanged from that
 * constant, character for character — only the rendering differs.
 */
private data class PolicySection(val heading: String, val points: List<String>)

private const val PRIVACY_INTRO =
    "The Lookbook (package com.zakir.vestra) generates modest-fashion try-on looks " +
        "and optional free-tier cloud studio outputs (image, video, code)."

private val PRIVACY_SECTIONS = listOf(
    PolicySection(
        "On your device",
        listOf(
            "Photos you pick or capture are processed on-device when using Lite or Pro packs. " +
                "They stay in app-private storage (or Documents/TheLookbook when durable storage is enabled).",
            "Generated looks are marked AI-generated (watermark on store builds + metadata).",
            "Deleting the app removes private storage; durable Documents copies remain until you delete them.",
        ),
    ),
    PolicySection(
        "Optional free-tier cloud",
        listOf(
            "Only when you select Cloud try-on or run Image / Video / Code studio: prompt text and " +
                "attached images go over HTTPS to the selected Hugging Face Space, Groq, or OpenRouter job. " +
                "Provider retention policies apply. The Lookbook does not keep uploads on a Lookbook server.",
            "Images are re-encoded before upload to strip EXIF when possible.",
            "API keys stay on your device and are sent only to the matching provider when you generate.",
        ),
    ),
    PolicySection(
        "Content reports",
        listOf(
            "Reports (reason + file path) are stored on this device only. " +
                "Export them from Settings → Storage & privacy.",
        ),
    ),
    PolicySection(
        "What we never do",
        listOf(
            "No ads, trackers, or analytics SDKs.",
            "No sale of personal data for advertising.",
            "No contacts, precise location, or browsing history collection.",
        ),
    ),
    PolicySection(
        "Children",
        listOf(
            "Not directed at children under 13. " +
                "Likeness consent prohibits generating anyone without permission.",
        ),
    ),
    PolicySection("Contact", listOf(LookbookCopy.SUPPORT_EMAIL)),
)
