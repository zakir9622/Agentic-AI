package com.zakir.vestra.ui.components

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [Modifier.tilt3d] must become a true no-op — the exact same [Modifier] instance, not just a
 * behaviorally-similar one — the moment the user has reduced motion enabled, matching every
 * other animation in this app (`rememberReduceMotion()`). With motion enabled it must actually
 * chain onto the base modifier rather than silently dropping the gesture/graphicsLayer wiring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class TiltModifierTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun identityModifierWhenReduceMotionIsEnabled() {
        var base: Modifier? = null
        var tilted: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            val b = remember { Modifier }
            val t = b.tilt3d()
            base = b
            tilted = t
            Box(t)
        }
        compose.waitForIdle()
        assertSame("tilt3d() must return the exact base Modifier when motion is reduced", base, tilted)
    }

    @Test
    fun addsWiringWhenReduceMotionIsDisabled() {
        var base: Modifier? = null
        var tilted: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            val b = remember { Modifier }
            val t = b.tilt3d()
            base = b
            tilted = t
            Box(t)
        }
        compose.waitForIdle()
        assertTrue(
            "tilt3d() must chain additional modifiers when motion is enabled",
            tilted !== base,
        )
    }
}
