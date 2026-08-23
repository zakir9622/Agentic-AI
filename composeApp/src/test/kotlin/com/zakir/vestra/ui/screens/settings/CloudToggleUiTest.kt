package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.settings.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real Compose UI tests for the cloud master toggle, run on the JVM via Robolectric — no
 * device, emulator, or KVM required. These render the actual composable and drive it the way
 * a person would, rather than only asserting on the settings object underneath.
 */
@RunWith(RobolectricTestRunner::class)
// Substitute a plain Application: the real VestraApp.onCreate builds the whole DI graph and
// calls Environment.isExternalStorageManager(), which throws under Robolectric (no storage
// volumes). These tests render one composable in isolation and need none of that.
@Config(sdk = [35], application = android.app.Application::class)
class CloudToggleUiTest {

    @get:Rule
    val compose = createComposeRule()

    private class MemorySettings : Settings {
        private val map = mutableMapOf<String, Any?>()
        override val keys: Set<String> get() = map.keys
        override val size: Int get() = map.size
        override fun clear() = map.clear()
        override fun remove(key: String) { map.remove(key) }
        override fun hasKey(key: String): Boolean = map.containsKey(key)
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = map[key] as? Int
        override fun putLong(key: String, value: Long) { map[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = map[key] as? Long
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
        override fun getStringOrNull(key: String): String? = map[key] as? String
        override fun putFloat(key: String, value: Float) { map[key] = value }
        override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = map[key] as? Float
        override fun putDouble(key: String, value: Double) { map[key] = value }
        override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
        override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
    }

    private fun renderToggle(appSettings: AppSettings) {
        compose.setContent {
            LazyColumn {
                settingsCloudMasterToggleSection(appSettings = appSettings)
            }
        }
    }

    @Test
    fun toggleRendersOffByDefaultWithLocalOnlyCopy() {
        val appSettings = AppSettings(MemorySettings())
        renderToggle(appSettings)

        compose.onNodeWithText("Enable cloud models").assertExists()
        compose.onNodeWithText("Off by default", substring = true).assertExists()
        compose.onNode(androidx.compose.ui.test.isToggleable()).assertIsOff()
        assertFalse(appSettings.cloudModelsEnabled.value)
    }

    @Test
    fun tappingTheSwitchTurnsCloudOnAndUpdatesTheCopy() {
        val appSettings = AppSettings(MemorySettings())
        renderToggle(appSettings)

        compose.onNode(androidx.compose.ui.test.isToggleable()).performClick()

        assertTrue("switch tap must reach AppSettings", appSettings.cloudModelsEnabled.value)
        compose.onNode(androidx.compose.ui.test.isToggleable()).assertIsOn()
        // Copy must follow state, not stay stuck on the off-state sentence.
        compose.onNodeWithText("Cloud generation is on", substring = true).assertExists()
    }

    @Test
    fun switchReflectsAlreadyEnabledStateOnFirstRender() {
        val appSettings = AppSettings(MemorySettings())
        appSettings.setCloudModelsEnabled(true)
        renderToggle(appSettings)

        compose.onNode(androidx.compose.ui.test.isToggleable()).assertIsOn()
        compose.onNodeWithText("Cloud generation is on", substring = true).assertExists()
    }
}
