package com.zakir.vestra.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.content.LookbookCopy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import com.zakir.vestra.ui.components.GlassAppMark
import com.zakir.vestra.ui.components.GlassBadgePill
import com.zakir.vestra.ui.components.SocialProofRow
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.components.GlassPrimaryButton
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.VestraColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private data class OnboardingPage(val title: String, val body: String)

// Two of these pages described an app that no longer exists, and nothing caught it because
// onboarding is the one screen a returning user never sees again. It was found by finally
// running the app and reading the first thing it says: it promised "four studios, each opening
// as a separate screen from Home", which is exactly the architecture the single-chatbox redesign
// removed, and told people to "tap the model chip in any studio" after the chip moved to the top
// bar. Copy that teaches a deleted UI is worse than no copy: it is a wrong map handed out at the
// door.
private val pages = listOf(
    OnboardingPage(
        "One box, every kind of output",
        "Type what you want and send. Tap + to switch between chat, image, video, code and audio, " +
            "or to attach a photo — it is the same conversation either way.",
    ),
    OnboardingPage(
        "First run loads the model",
        "The send button shows a spinner while a model is loading — that's normal, not stuck. " +
            "Once it's ready, on-device models keep working fully offline.",
    ),
    OnboardingPage(
        "Local by default, cloud if you want",
        "Tap the model name at the top to switch between on-device and free cloud models. " +
            "Nothing reaches the network until you pick a cloud model or add a key.",
    ),
    OnboardingPage(
        "Cloud keys are optional",
        "Paste free Hugging Face, Groq, or OpenRouter tokens in Settings to unlock cloud models. " +
            "On-device generation never needs a key.",
    ),
)

@Composable
fun OnboardingScreen(appSettings: AppSettings, onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val slide = pages[page]

    SpatialBackground {
        Column(
            Modifier
                .fillMaxSize()
                .testTag(com.zakir.vestra.ui.TestTags.ONBOARDING_SCREEN)
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(SpacingTokens.xxl))

            // Reference-style first viewport: eyebrow badge, glass app mark, wordmark, promise.
            // The old header stacked a wordmark and tagline flush-left above a collage strip;
            // this centres the identity so the first screen reads as a product opening rather
            // than a page heading.
            GlassBadgePill("NEXT-GEN INTELLIGENCE")
            Spacer(Modifier.height(SpacingTokens.xl))
            GlassAppMark(
                icon = Icons.Outlined.AutoAwesome,
                contentDescription = null,
            )
            Spacer(Modifier.height(SpacingTokens.xl))
            Text(
                LookbookCopy.PRODUCT_NAME.uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = VestraColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SpacingTokens.xs))
            Text(
                LookbookCopy.PRODUCT_TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = VestraColors.InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Collage strip — first viewport brand + atmosphere (no secondary marketing clutter).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(VestraColors.AtelierContainer, VestraColors.AtelierCanvas),
                        ),
                    ),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                        .size(110.dp, 150.dp)
                        .rotate(-10f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3D2A18), VestraColors.SaffronDeep),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(120.dp, 160.dp)
                        .rotate(4f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E2430), VestraColors.AccentSoft.copy(alpha = 0.7f)),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(100.dp, 140.dp)
                        .rotate(14f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2C1810), Color(0xFF5C3A22)),
                            ),
                        ),
                )
            }

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
                Column(Modifier.fillMaxWidth()) {
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

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) VestraColors.Accent else VestraColors.InkMuted.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            if (page < pages.lastIndex) {
                GlassPrimaryButton(
                    text = "Continue",
                    onClick = { page += 1 },
                    modifier = Modifier.testTag(com.zakir.vestra.ui.TestTags.ONBOARDING_CONTINUE),
                )
                TextButton(
                    onClick = {
                        appSettings.setOnboardingComplete()
                        onDone()
                    },
                    modifier = Modifier.testTag(com.zakir.vestra.ui.TestTags.ONBOARDING_SKIP),
                ) { Text("Skip") }
            } else {
                GlassPrimaryButton(
                    text = "Get started",
                    onClick = {
                        appSettings.setOnboardingComplete()
                        onDone()
                    },
                    modifier = Modifier.testTag(com.zakir.vestra.ui.TestTags.ONBOARDING_GET_STARTED),
                )
                Spacer(Modifier.height(SpacingTokens.md))
                SocialProofRow(
                    avatarColors = listOf(
                        VestraColors.Accent,
                        VestraColors.ModalityAudio,
                        VestraColors.SaffronDeep,
                    ),
                    text = LookbookCopy.ONBOARDING_SOCIAL_PROOF,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
