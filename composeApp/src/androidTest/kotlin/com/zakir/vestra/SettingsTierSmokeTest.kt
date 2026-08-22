package com.zakir.vestra

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zakir.vestra.shared.domain.EngineTier
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
}
