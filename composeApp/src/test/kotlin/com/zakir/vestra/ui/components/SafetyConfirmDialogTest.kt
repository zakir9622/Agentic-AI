package com.zakir.vestra.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zakir.vestra.shared.safety.SafetyPresets
import com.zakir.vestra.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SafetyConfirmDialog] gates image generation for presets whose [SafetyPreset.confirm][
 * com.zakir.vestra.shared.safety.SafetyPreset.confirm] is true — this is the real confirmation
 * step that field promises (a code-review finding on the initial Part B.3 landing found the
 * field declared and asserted in unit tests, but never actually checked by any UI code).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class SafetyConfirmDialogTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersPresetLabelAndBothActions() {
        compose.setContent {
            SafetyConfirmDialog(preset = SafetyPresets.BLUR_IDENTITIES, onConfirm = {}, onCancel = {})
        }
        compose.onNodeWithText("Confirm: Blur identities").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SAFETY_PRESET_CONFIRM_GENERATE).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SAFETY_PRESET_CONFIRM_CANCEL).assertIsDisplayed()
    }

    @Test
    fun generateButton_invokesOnConfirm_notOnCancel() {
        var confirmed = false
        var cancelled = false
        compose.setContent {
            SafetyConfirmDialog(
                preset = SafetyPresets.REDACT_DETAILS,
                onConfirm = { confirmed = true },
                onCancel = { cancelled = true },
            )
        }
        compose.onNodeWithTag(TestTags.SAFETY_PRESET_CONFIRM_GENERATE).performClick()
        assertEquals(true, confirmed)
        assertFalse(cancelled)
    }

    @Test
    fun cancelButton_invokesOnCancel_notOnConfirm() {
        var confirmed = false
        var cancelled = false
        compose.setContent {
            SafetyConfirmDialog(
                preset = SafetyPresets.BLUR_IDENTITIES,
                onConfirm = { confirmed = true },
                onCancel = { cancelled = true },
            )
        }
        compose.onNodeWithTag(TestTags.SAFETY_PRESET_CONFIRM_CANCEL).performClick()
        assertEquals(true, cancelled)
        assertFalse(confirmed)
    }
}
