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
 * [Modifier.press3d] and [Modifier.lift3d] must both become true no-ops under reduced motion,
 * same discipline as [TiltModifierTest] — see that class's doc for why identity (not just
 * behavioral similarity) is asserted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PressModifierTest {

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
    fun press3dIsIdentityWhenReduceMotionIsEnabled() {
        var base: Modifier? = null
        var pressed: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(true, context)
            val b = remember { Modifier }
            val p = b.press3d()
            base = b
            pressed = p
            Box(p)
        }
        compose.waitForIdle()
        assertSame("press3d() must return the exact base Modifier when motion is reduced", base, pressed)
    }

    @Test
    fun press3dAddsWiringWhenReduceMotionIsDisabled() {
        var base: Modifier? = null
        var pressed: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(false, context)
            val b = remember { Modifier }
            val p = b.press3d()
            base = b
            pressed = p
            Box(p)
        }
        compose.waitForIdle()
        assertTrue("press3d() must chain additional modifiers when motion is enabled", pressed !== base)
    }

    @Test
    fun lift3dIsIdentityWhenReduceMotionIsEnabled() {
        var base: Modifier? = null
        var lifted: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(true, context)
            val b = remember { Modifier }
            val l = b.lift3d()
            base = b
            lifted = l
            Box(l)
        }
        compose.waitForIdle()
        assertSame("lift3d() must return the exact base Modifier when motion is reduced", base, lifted)
    }

    @Test
    fun lift3dAddsWiringWhenReduceMotionIsDisabled() {
        var base: Modifier? = null
        var lifted: Modifier? = null
        compose.setContent {
            val context = LocalContext.current
            setReduceMotion(false, context)
            val b = remember { Modifier }
            val l = b.lift3d()
            base = b
            lifted = l
            Box(l)
        }
        compose.waitForIdle()
        assertTrue("lift3d() must chain additional modifiers when motion is enabled", lifted !== base)
    }
}
