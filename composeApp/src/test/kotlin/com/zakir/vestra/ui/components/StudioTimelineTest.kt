package com.zakir.vestra.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.ui.GenerativeViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [StudioTurnBubble] rendering across every [GenerativeState] subtype a [GenerativeViewModel.StudioTurn.result]
 * can hold, plus the null (in-progress/typing-indicator) case — a real regression guard for the
 * 3.1.6 conversation-timeline redesign, which reuses [ResultPane]'s per-subtype rendering inside
 * a scrolling turn history instead of a single current-result card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class StudioTimelineTest {

    @get:Rule
    val compose = createComposeRule()

    private fun turn(result: GenerativeState?) = GenerativeViewModel.StudioTurn(
        id = "turn-1",
        prompt = "a test prompt",
        timestampMs = 0L,
        capability = AiCapability.IMAGE_GEN,
        result = result,
    )

    @Test
    fun rendersTypingIndicatorWhenResultIsStillNull() {
        compose.setContent {
            StudioTurnBubble(
                turn = turn(null),
                index = 0,
                isLatest = true,
                accent = androidx.compose.ui.graphics.Color.Red,
                generationStartedAtMs = null,
                onRetry = null,
                onDismiss = null,
                retryLabel = "Retry",
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersImageReadyResult() {
        compose.setContent {
            StudioTurnBubble(
                turn = turn(GenerativeState.ImageReady(path = "/tmp/fake.png", providerId = "local-test")),
                index = 0,
                isLatest = true,
                accent = androidx.compose.ui.graphics.Color.Red,
                generationStartedAtMs = null,
                onRetry = {},
                onDismiss = {},
                retryLabel = "Retry",
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersCodeReadyResult() {
        compose.setContent {
            StudioTurnBubble(
                turn = turn(
                    GenerativeState.CodeReady(
                        text = "fun main() {}",
                        tokensIn = 3,
                        tokensOut = 5,
                        providerId = "local-test",
                    ),
                ),
                index = 0,
                isLatest = true,
                accent = androidx.compose.ui.graphics.Color.Red,
                generationStartedAtMs = null,
                onRetry = {},
                onDismiss = {},
                retryLabel = "Retry",
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersFailedResult() {
        compose.setContent {
            StudioTurnBubble(
                turn = turn(GenerativeState.Failed(message = "Something went wrong")),
                index = 0,
                isLatest = true,
                accent = androidx.compose.ui.graphics.Color.Red,
                generationStartedAtMs = null,
                onRetry = {},
                onDismiss = {},
                retryLabel = "Retry",
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun olderTurnRendersWithoutRetryOrDismissCallbacks() {
        // Mirrors how UnifiedMainScreen calls this for every turn except the
        // latest: onRetry/onDismiss forced to null so history stays view-only.
        compose.setContent {
            StudioTurnBubble(
                turn = turn(GenerativeState.ImageReady(path = "/tmp/fake.png", providerId = "local-test")),
                index = 0,
                isLatest = false,
                accent = androidx.compose.ui.graphics.Color.Red,
                generationStartedAtMs = null,
                onRetry = null,
                onDismiss = null,
                retryLabel = "Retry",
            )
        }
        compose.waitForIdle()
    }
}
