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
 * Real Compose UI tests for [LookbookBottomBar] — three destinations only (Home, Library,
 * Settings). Image/Video/Audio/Code and Chat are reached from Home's tool grid, not from this
 * bar, so it has no Create FAB or Chat slot to test any more.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class BottomBarNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allThreeDestinationsAreRendered() {
        compose.setContent {
            LookbookBottomBar(selected = BottomBarDestination.HOME, onSelect = {})
        }
        compose.onNodeWithTag(TestTags.BOTTOM_BAR).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_HOME).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_LIBRARY).assertExists()
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
        // The isolated modality screens (Image/Video/Audio/Code), Chat, the try-on capture flow,
        // nested Settings sections, Packs, Usage, Help, and Privacy aren't any of the three dock
        // destinations — VestraNavHost passes `selected = null` for those. The bar must still
        // render every item rather than crashing on a null selection.
        compose.setContent {
            LookbookBottomBar(selected = null, onSelect = {})
        }
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_HOME).assertExists()
        compose.onNodeWithTag(TestTags.BOTTOM_BAR_SETTINGS).assertExists()
    }
}
