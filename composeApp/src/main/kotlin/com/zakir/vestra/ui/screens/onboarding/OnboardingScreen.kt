package com.zakir.vestra.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.VestraColors

private data class OnboardingPage(val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(
        "Modest wear, on your phone",
        "Generate abaya, hijab, niqab, and Pakistani traditional wear with AI — fully offline after downloading the Pro model pack.",
    ),
    OnboardingPage(
        "Cast your perfect scene",
        "Set ethnicity, body type, hair coverage, color, and scenario. One garment photo becomes a full photoshoot.",
    ),
    OnboardingPage(
        "Shop, sell, and create",
        "Shoppers preview looks. Sellers batch listing shots with Save all. Creators use Create and Video studios — on-device or free cloud.",
    ),
    OnboardingPage(
        "Keys unlock free cloud",
        "In Settings, paste free Hugging Face, Groq, or OpenRouter tokens. Cloud models unlock automatically — local Lite/Pro never need a key.",
    ),
)

@Composable
fun OnboardingScreen(appSettings: AppSettings, onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val slide = pages[page]
    val shape = RoundedCornerShape(18.dp)

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "The Lookbook",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Modest wear · local AI",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(VestraColors.GlassFillStrong)
                    .border(1.dp, VestraColors.GlassBorder, shape)
                    .padding(14.dp),
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = VestraColors.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = slide.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "${page + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            if (page < pages.lastIndex) {
                Button(
                    onClick = { page += 1 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Next") }
                TextButton(onClick = {
                    appSettings.setOnboardingComplete()
                    onDone()
                }) { Text("Skip") }
            } else {
                Button(
                    onClick = {
                        appSettings.setOnboardingComplete()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Get started") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
