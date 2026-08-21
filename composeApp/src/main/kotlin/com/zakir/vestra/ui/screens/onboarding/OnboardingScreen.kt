package com.zakir.vestra.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.components.GlassPrimaryButton
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
    val shape = RoundedCornerShape(24.dp)

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "The Lookbook",
                style = MaterialTheme.typography.displaySmall,
                color = VestraColors.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Modest wear · local AI",
                style = MaterialTheme.typography.labelLarge,
                color = VestraColors.Accent,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = slide,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 5 }) togetherWith
                        (fadeOut() + slideOutHorizontally { -it / 5 })
                },
                label = "onboardSlide",
                modifier = Modifier.fillMaxWidth(),
            ) { current ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            Brush.verticalGradient(
                                listOf(VestraColors.GlassFillStrong, VestraColors.GlassFill),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(VestraColors.GlassHighlight, VestraColors.GlassBorder),
                            ),
                            shape,
                        )
                        .padding(20.dp),
                ) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VestraColors.Ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = current.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = VestraColors.InkMuted,
                        textAlign = TextAlign.Start,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "${page + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            if (page < pages.lastIndex) {
                GlassPrimaryButton(
                    text = "Next",
                    onClick = { page += 1 },
                )
                TextButton(onClick = {
                    appSettings.setOnboardingComplete()
                    onDone()
                }) { Text("Skip") }
            } else {
                GlassPrimaryButton(
                    text = "Enter the atelier",
                    onClick = {
                        appSettings.setOnboardingComplete()
                        onDone()
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
