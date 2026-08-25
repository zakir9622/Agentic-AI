package com.zakir.vestra.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zakir.vestra.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** [QuickPromptCarousel] — tapping a chip must invoke [QuickPromptCarousel]'s callback with that chip's prompt text. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class QuickPromptCarouselTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tappingAChipInvokesItsOwnPrompt() {
        var selected: String? = null
        val prompts = listOf(
            QuickPromptItem("Discuss winter layering", "STYLE"),
            QuickPromptItem("Compare on-device vs cloud", "PERF"),
        )
        compose.setContent {
            QuickPromptCarousel(prompts = prompts, onSelectPrompt = { selected = it })
        }
        compose.onNodeWithTag(TestTags.quickPromptChip(1)).performClick()
        assertEquals("Compare on-device vs cloud", selected)
    }

    @Test
    fun disabledCarouselDoesNotInvokeCallback() {
        var selected: String? = null
        compose.setContent {
            QuickPromptCarousel(
                prompts = listOf(QuickPromptItem("Discuss winter layering")),
                onSelectPrompt = { selected = it },
                enabled = false,
            )
        }
        compose.onNodeWithTag(TestTags.quickPromptChip(0)).performClick()
        assertNull(selected)
    }

    @Test
    fun emptyPromptListRendersNothing() {
        compose.setContent {
            QuickPromptCarousel(prompts = emptyList(), onSelectPrompt = {})
        }
        compose.onAllNodesWithTag(TestTags.QUICK_PROMPT_CAROUSEL).assertCountEquals(0)
    }

    @Test
    fun titleIsShownAboveTheChips() {
        compose.setContent {
            QuickPromptCarousel(
                prompts = listOf(QuickPromptItem("Discuss winter layering")),
                onSelectPrompt = {},
                title = "QUICK PROMPTS",
            )
        }
        compose.onNodeWithText("QUICK PROMPTS").assertExists()
    }
}
