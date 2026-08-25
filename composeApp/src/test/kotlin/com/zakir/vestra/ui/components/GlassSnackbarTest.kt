package com.zakir.vestra.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.zakir.vestra.ui.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A7 — [GlassSnackbarHost] is the top-center Compose replacement for Android's bottom-anchored,
 * unstyled `Toast`; [GlassSnackbar] is the global bus any call site (Composable or plain
 * Kotlin, e.g. [com.zakir.vestra.media.MediaExport]) posts messages onto.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class GlassSnackbarTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun show_deliversMessageToHost() {
        compose.setContent {
            GlassSnackbarHost()
        }
        GlassSnackbar.show("Saved", SnackbarLevel.SUCCESS)

        compose.waitUntilAtLeastOneExists(hasTestTag(TestTags.GLASS_SNACKBAR), timeoutMillis = 3_000)
        compose.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun show_secondMessageReplacesFirst() {
        compose.setContent {
            GlassSnackbarHost()
        }
        GlassSnackbar.show("First", SnackbarLevel.INFO)
        compose.waitUntilAtLeastOneExists(hasTestTag(TestTags.GLASS_SNACKBAR), timeoutMillis = 3_000)

        GlassSnackbar.show("Second", SnackbarLevel.ERROR)
        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithText("Second").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Second").assertExists()
    }
}
