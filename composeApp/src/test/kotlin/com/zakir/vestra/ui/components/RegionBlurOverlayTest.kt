package com.zakir.vestra.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class RegionBlurOverlayTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun draggingAddsARegion() {
        var captured: List<BlurRegion> = emptyList()
        compose.setContent {
            var regions by remember { mutableStateOf<List<BlurRegion>>(emptyList()) }
            RegionBlurOverlay(
                regions = regions,
                onRegionsChange = {
                    regions = it
                    captured = it
                },
                modifier = Modifier.size(200.dp),
            )
        }

        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_CANVAS).performTouchInput {
            val start = center - Offset(40f, 40f)
            down(start)
            // Several small steps, not one big jump: detectDragGestures needs to cross touch
            // slop and see repeated movement before it recognizes a drag rather than a tap.
            for (step in 1..10) {
                moveTo(start + Offset(8f * step, 8f * step))
            }
            up()
        }
        compose.waitForIdle()

        assertEquals(1, captured.size)
        assertTrue(
            "region should have non-zero size",
            captured.first().rect.width > 0f && captured.first().rect.height > 0f,
        )
    }

    @Test
    fun aTinyDragDoesNotAddARegion() {
        var captured: List<BlurRegion> = emptyList()
        compose.setContent {
            var regions by remember { mutableStateOf<List<BlurRegion>>(emptyList()) }
            RegionBlurOverlay(
                regions = regions,
                onRegionsChange = {
                    regions = it
                    captured = it
                },
                modifier = Modifier.size(200.dp),
            )
        }

        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_CANVAS).performTouchInput {
            down(center)
            moveTo(center + Offset(1f, 1f))
            up()
        }
        compose.waitForIdle()

        assertEquals(0, captured.size)
    }

    @Test
    fun rendersCorrectlyAfterRegionsAreClearedExternally() {
        // "Clear regions" itself lives in PrivacyBlurSheet — RegionBlurOverlay is a controlled
        // component, so clearing means the caller passes an empty list back in. This guards that
        // re-composing from one drawn region down to zero doesn't crash.
        var regions by mutableStateOf(listOf(BlurRegion(Rect(0f, 0f, 20f, 20f))))
        compose.setContent {
            RegionBlurOverlay(
                regions = regions,
                onRegionsChange = { regions = it },
                modifier = Modifier.size(200.dp),
            )
        }
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_CANVAS).assertExists()

        regions = emptyList()
        compose.waitForIdle()
        compose.onNodeWithTag(TestTags.PRIVACY_BLUR_CANVAS).assertExists()
    }
}
