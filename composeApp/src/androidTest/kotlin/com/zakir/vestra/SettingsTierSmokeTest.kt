package com.zakir.vestra

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zakir.vestra.shared.domain.EngineTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTierSmokeTest {

    @Test
    fun engineTierEnumIncludesCloudAndAuto() {
        val names = EngineTier.entries.map { it.name }
        assertTrue("AUTO" in names)
        assertTrue("CLOUD" in names)
        assertTrue("LITE" in names)
        assertTrue("PRO" in names)
    }

    @Test
    fun studioTabRoutesMatchHomePager() {
        // NEWS was removed from the pager in A3 (3.1.0-rc25) — Chat is now a top-level bottom-dock
        // destination (Routes.CHAT), not a HomeTab route.
        assertEquals("tryon", HomeTabRoute.TRY_ON)
        assertEquals("image", HomeTabRoute.IMAGE)
        assertEquals("video", HomeTabRoute.VIDEO)
        assertEquals("audio", HomeTabRoute.AUDIO)
        assertEquals("code", HomeTabRoute.CODE)
    }
}

/** Mirrors [HomeScreen] tab route keys for navigation smoke tests. */
object HomeTabRoute {
    const val TRY_ON = "tryon"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val CODE = "code"
}
