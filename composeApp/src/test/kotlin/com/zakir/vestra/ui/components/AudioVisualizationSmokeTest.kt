package com.zakir.vestra.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [AudioLevelMeter] and [SpectrumScope] render off externally-supplied data (no live
 * `AudioRecord`/`Visualizer` capture here — see `AndroidLatencyCalibrator`'s doc comment for why
 * that device I/O is unverified in this environment). These are smoke tests confirming the
 * Canvas drawing code itself doesn't crash across the value ranges it'll actually see: silence,
 * full-scale, changing amplitude, and both empty/populated FFT output.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AudioVisualizationSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun audioLevelMeterRendersAtZeroAmplitude() {
        compose.setContent {
            AudioLevelMeter(amplitude = 0f)
        }
        compose.waitForIdle()
    }

    @Test
    fun audioLevelMeterRendersAtFullAmplitude() {
        compose.setContent {
            AudioLevelMeter(amplitude = 1f)
        }
        compose.waitForIdle()
    }

    @Test
    fun audioLevelMeterRendersAtMidAmplitude() {
        compose.setContent {
            AudioLevelMeter(amplitude = 0.5f)
        }
        compose.waitForIdle()
    }

    @Test
    fun spectrumScopeRendersWithEmptyMagnitudes() {
        compose.setContent {
            SpectrumScope(magnitudes = FloatArray(0))
        }
        compose.waitForIdle()
    }

    @Test
    fun spectrumScopeRendersWithPopulatedMagnitudes() {
        val mags = FloatArray(128) { i -> if (i == 20) 1f else 0.05f }
        compose.setContent {
            SpectrumScope(magnitudes = mags)
        }
        compose.waitForIdle()
    }

    @Test
    fun spectrumScopeRendersWithAllZeroMagnitudes() {
        compose.setContent {
            SpectrumScope(magnitudes = FloatArray(64))
        }
        compose.waitForIdle()
    }
}
