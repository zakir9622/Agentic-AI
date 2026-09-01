package com.zakir.vestra.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [LiteRtStatusIndicator] renders real state passed in — not simulated network/ping data (see
 * the flag raised against `ModelConfigScreen`'s fake pings during this port; this component is
 * clean by contrast). Smoke tests across every real state it can be in, plus one interaction
 * test confirming the "not installed" state routes to Packs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class LiteRtStatusIndicatorTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersWhenNotInstalled() {
        compose.setContent {
            LiteRtStatusIndicator(isInstalled = false, isLoaded = false)
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersWhenInstalledButNotWarm() {
        compose.setContent {
            LiteRtStatusIndicator(isInstalled = true, isLoaded = false)
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersWhileLoading() {
        compose.setContent {
            LiteRtStatusIndicator(isInstalled = true, isLoaded = false, isLoading = true)
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersWhenReady() {
        compose.setContent {
            LiteRtStatusIndicator(isInstalled = true, isLoaded = true, backend = "LiteRT GPU")
        }
        compose.waitForIdle()
    }

    @Test
    fun rendersOnError() {
        compose.setContent {
            LiteRtStatusIndicator(isInstalled = true, isLoaded = false, errorMessage = "Engine init failed")
        }
        compose.waitForIdle()
    }

    @Test
    fun tappingWhenNotInstalledOpensPacks() {
        var openedPacks = false
        compose.setContent {
            LiteRtStatusIndicator(
                isInstalled = false,
                isLoaded = false,
                onOpenPacks = { openedPacks = true },
                modifier = androidx.compose.ui.Modifier,
            )
        }
        compose.onNodeWithTag("litert_status_indicator").performClick()
        assertTrue(openedPacks)
    }
}
