package com.zakir.vestra.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.SpatialBackground
import kotlinx.coroutines.launch

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
        "Local or cloud AI",
        "Pro runs on-device (private, free). Cloud unlocks IDM-VTON, Leffa, and more via free Hugging Face Spaces — configure in Settings.",
    ),
)

@Composable
fun OnboardingScreen(appSettings: AppSettings, onDone: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Text("THE LOOKBOOK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val slide = pages[page]
                GlassCard(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(slide.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        slide.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "${pagerState.currentPage + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (pagerState.currentPage < pages.lastIndex) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
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
            Spacer(Modifier.height(24.dp))
        }
    }
}
