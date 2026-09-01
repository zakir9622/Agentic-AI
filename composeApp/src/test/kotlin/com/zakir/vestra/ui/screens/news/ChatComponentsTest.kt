package com.zakir.vestra.ui.screens.news

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zakir.vestra.shared.chat.ChatMessage
import com.zakir.vestra.shared.news.NewsItem
import com.zakir.vestra.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The chat UI ported from GoogleLookBookUI's ChatComponents.kt (bubble, typing indicator, empty
 * state, headlines bar) — dropped the token-throughput metrics block since our [ChatMessage]
 * doesn't carry ttft/duration/tokensOut fields (the source repo's did); everything kept here
 * renders from real state only, no simulated data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class ChatComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun userBubbleRendersWithItsOwnTestTag() {
        val msg = ChatMessage(id = "1", role = "user", text = "Hello", timestampMs = 0L)
        compose.setContent {
            ChatMessageBubble(message = msg, index = 0)
        }
        compose.onNodeWithTag(TestTags.chatMessageBubble(0, "user")).assertExists()
        compose.onNodeWithText("Hello").assertExists()
    }

    @Test
    fun assistantBubbleShowsProviderIdWhenNoDisplayNameGiven() {
        val msg = ChatMessage(id = "2", role = "assistant", text = "Hi there", timestampMs = 0L, providerId = "local-gemma")
        compose.setContent {
            ChatMessageBubble(message = msg, index = 0)
        }
        compose.onNodeWithText("local-gemma").assertExists()
    }

    @Test
    fun typingIndicatorRenders() {
        compose.setContent {
            ChatTypingIndicator(modelLabel = "Test Model")
        }
        compose.onNodeWithTag(TestTags.CHAT_TYPING_INDICATOR).assertExists()
    }

    @Test
    fun emptyStateStarterPromptInvokesCallback() {
        var selected: String? = null
        compose.setContent {
            ChatEmptyState(onPromptSelected = { selected = it })
        }
        compose.onNodeWithTag(TestTags.chatStarterPrompt(0)).performClick()
        assertEquals(true, selected != null)
    }

    @Test
    fun headlinesBarRendersItemsAndInvokesClickCallback() {
        var clickedIndex = -1
        val items = listOf(
            NewsItem(id = "1", title = "Modest fashion trends", link = "https://example.com/1", publishedMs = 0L, source = "Vogue"),
            NewsItem(id = "2", title = "On-device AI in fashion", link = "https://example.com/2", publishedMs = 0L, source = "TechCrunch"),
        )
        compose.setContent {
            NewsHeadlinesBar(
                newsItems = items,
                refreshing = false,
                onRefresh = {},
                onHeadlineClick = { _, index -> clickedIndex = index },
            )
        }
        compose.onNodeWithTag(TestTags.chatHeadlineCard(1)).performClick()
        assertEquals(1, clickedIndex)
    }

    @Test
    fun headlinesBarRefreshButtonInvokesCallback() {
        var refreshed = false
        compose.setContent {
            NewsHeadlinesBar(
                newsItems = emptyList(),
                refreshing = false,
                onRefresh = { refreshed = true },
                onHeadlineClick = { _, _ -> },
            )
        }
        compose.onNodeWithTag(TestTags.CHAT_REFRESH_BUTTON).performClick()
        assertEquals(true, refreshed)
    }

    @Test
    fun memoryPillHiddenAtZeroFacts() {
        compose.setContent {
            MemoryPill(factCount = 0)
        }
        compose.onNodeWithTag(TestTags.CHAT_MEMORY_PILL).assertDoesNotExist()
    }

    @Test
    fun memoryPillShowsSingularCopyForOneFact() {
        compose.setContent {
            MemoryPill(factCount = 1)
        }
        compose.onNodeWithText("Remembering 1 thing").assertExists()
    }

    @Test
    fun memoryPillShowsPluralCopyForMultipleFacts() {
        compose.setContent {
            MemoryPill(factCount = 3)
        }
        compose.onNodeWithTag(TestTags.CHAT_MEMORY_PILL).assertExists()
        compose.onNodeWithText("Remembering 3 things").assertExists()
    }
}
