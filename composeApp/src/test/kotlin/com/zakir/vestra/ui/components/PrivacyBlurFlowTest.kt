package com.zakir.vestra.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
 * UI-level tests for [PrivacyBlurContent] (the [PrivacyBlurSheet]'s content, rendered directly —
 * see that composable's doc comment for why: Robolectric's Compose test harness doesn't reliably
 * dispatch click actions into a live `ModalBottomSheet`'s window layer).
 *
 * Uses `qualifiers = "w360dp-h800dp"` because Robolectric's default root window (320x470px) is
 * smaller than a real phone screen — too small to fit the image preview above the "Save
 * original"/"Save blurred" row, which pushed those buttons off-screen and gave them a clipped
 * zero-size `boundsInRoot`, silently swallowing clicks with no exception.
 *
 * Deliberately does not exercise the "Save blurred" path with `autoBlur` on — that calls
 * `FaceBlurProcessor`, which needs ML Kit's real on-device detector and is unverified in this
 * environment (see `AndroidLatencyCalibrator`'s doc comment for the same caveat pattern). "Save
 * original" is a pure pass-through with no image processing, so it's fully testable here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w360dp-h800dp")
class PrivacyBlurFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContentSized(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            Box(Modifier.fillMaxSize()) { content() }
        }
    }

    @Test
    fun contentRendersTheExpectedControls() {
        setContentSized {
            PrivacyBlurContent(imagePath = "/fake/generated.jpg", onSaved = {})
        }
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_TOGGLE).assertExists()
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_APPLY).assertExists()
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_CANVAS).assertExists()
    }

    @Test
    fun autoBlurToggleDefaultsOnAndCanBeTurnedOff() {
        setContentSized {
            PrivacyBlurContent(imagePath = "/fake/generated.jpg", onSaved = {})
        }
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_TOGGLE).assertIsOn()
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_TOGGLE).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_TOGGLE).assertIsOff()
    }

    @Test
    fun saveOriginalPassesThroughTheOriginalPathUnmodified() {
        var savedPath: String? = null
        val original = "/fake/generated.jpg"
        setContentSized {
            PrivacyBlurContent(imagePath = original, onSaved = { savedPath = it })
        }
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_SAVE_ORIGINAL).performClick()
        compose.waitForIdle()
        assertEquals(original, savedPath)
    }
}
