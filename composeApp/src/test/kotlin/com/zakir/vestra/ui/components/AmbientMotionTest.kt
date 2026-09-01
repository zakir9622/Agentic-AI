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
 * [Modifier.floatSlow] and [Modifier.driftSlow] must both become true no-ops under reduced
 * motion — same discipline as [TiltModifierTest] — since both drive infinite ambient-orb
 * animations that would otherwise burn CPU pointlessly with the effect invisible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AmbientMotionTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setReduceMotion(enabled: Boolean, context: android.content.Context) {
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            if (enabled) 0f else 1f,
        )
    }

    @Test
    fun floatSlowIsIdentityWhenReduceMotionIsEnabled() {
        var base: Modifier? = null
        var floated: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(true, context)
            val b = remember { Modifier }
            val f = b.floatSlow()
            base = b
            floated = f
            Box(f)
        }
        compose.waitForIdle()
        assertSame("floatSlow() must return the exact base Modifier when motion is reduced", base, floated)
    }

    @Test
    fun floatSlowAddsWiringWhenReduceMotionIsDisabled() {
        var base: Modifier? = null
        var floated: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(false, context)
            val b = remember { Modifier }
            val f = b.floatSlow()
            base = b
            floated = f
            Box(f)
        }
        compose.waitForIdle()
        assertTrue("floatSlow() must chain additional modifiers when motion is enabled", floated !== base)
    }

    @Test
    fun driftSlowIsIdentityWhenReduceMotionIsEnabled() {
        var base: Modifier? = null
        var drifted: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(true, context)
            val b = remember { Modifier }
            val d = b.driftSlow()
            base = b
            drifted = d
            Box(d)
        }
        compose.waitForIdle()
        assertSame("driftSlow() must return the exact base Modifier when motion is reduced", base, drifted)
    }

    @Test
    fun driftSlowAddsWiringWhenReduceMotionIsDisabled() {
        var base: Modifier? = null
        var drifted: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(false, context)
            val b = remember { Modifier }
            val d = b.driftSlow()
            base = b
            drifted = d
            Box(d)
        }
        compose.waitForIdle()
        assertTrue("driftSlow() must chain additional modifiers when motion is enabled", drifted !== base)
    }
}
