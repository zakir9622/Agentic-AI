package com.zakir.vestra.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.zakir.vestra.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real Compose UI tests for [LookbookBottomBar] (A3) — the dock must render all five
 * destinations and tapping each one must invoke the matching callback, including the raised
 * center Create FAB (which shares [BottomBarDestination.CREATE]'s testTag but isn't part of the
 * regular item row).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class BottomBarNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allFiveDestinationsAreRendered() {
        compose.setContent {
            LookbookBottomBar(selected = BottomBarDestination.HOME, onSelect = {})
        }
        compose.onNodeWithTag(TestTags.BOTTOM_BAR).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_HOME).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_LIBRARY).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_CREATE).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_CHAT).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_SETTINGS).assertExists()
    }

    @Test
    fun tappingEachItemInvokesItsOwnDestination() {
        var lastSelected: BottomBarDestination? = null
        compose.setContent {
            LookbookBottomBar(selected = BottomBarDestination.HOME, onSelect = { lastSelected = it })
        }

        val cases = listOf(
            TestTags.BOTTOM_BAR_LIBRARY to BottomBarDestination.LIBRARY,
            TestTags.BOTTOM_BAR_CREATE to BottomBarDestination.CREATE,
            TestTags.BOTTOM_BAR_CHAT to BottomBarDestination.CHAT,
            TestTags.BOTTOM_BAR_SETTINGS to BottomBarDestination.SETTINGS,
            TestTags.BOTTOM_BAR_HOME to BottomBarDestination.HOME,
        )
        cases.forEach { (tag, expected) ->
            lastSelected = null
            compose.onNodeWithTag(tag).performClick()
            assertEquals("tapping $tag should select $expected", expected, lastSelected)
        }
    }

    @Test
    fun rendersWithoutCrashingWhenNoDestinationIsSelected() {
        // The try-on capture flow, nested Settings sections, Packs, Usage, Help, and Privacy
        // aren't any of the five dock destinations — VestraNavHost passes `selected = null` for
        // those. The bar must still render every item rather than crashing on a null selection.
        compose.setContent {
            LookbookBottomBar(selected = null, onSelect = {})
        }
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_HOME).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_SETTINGS).assertExists()
    }
}
