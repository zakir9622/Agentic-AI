package com.zakir.vestra.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.storage.ApiKeyDataStore
import com.zakir.vestra.ui.components.ApiUsageDashboardCard
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.screens.home.ComposerMode
import com.zakir.vestra.ui.screens.home.HomeEmptyState
import com.zakir.vestra.ui.screens.home.ModalityChipRow
import com.zakir.vestra.ui.screens.home.UnifiedTopBar
import com.zakir.vestra.ui.screens.settings.ApiMonitorScreen
import com.zakir.vestra.ui.screens.settings.DefaultModelsScreen
import com.zakir.vestra.ui.screens.settings.ModelsScreen
import com.zakir.vestra.ui.screens.settings.NotificationsScreen
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pixel screenshots of the surfaces reworked in the professional-UI pass.
 *
 * Two things this suite exists to catch, both of which shipped unnoticed before because nothing
 * outside the running app ever rendered these composables:
 *
 * 1. **Text that wraps or clips instead of ellipsizing.** The usage dashboard's service tiles
 *    laid `"0 reqs · 0 tok"` out one character per line on a narrow screen. Every case here runs
 *    at **360dp** as well as the suite default of 411dp, because the bug only appears at the
 *    narrower width — a 411dp-only shot would have passed straight over it.
 * 2. **Light-mode regressions.** `ScreenshotTest` renders dark only. Several fixes here change
 *    fill/tone relationships (the composer's recessed field, the filled modality chip), and those
 *    are exactly the kind of thing that reads fine in one palette and vanishes in the other.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w411dp-h914dp-xxhdpi")
class RedesignScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** In-memory [Settings] so a real [AppSettings] can be built without Android prefs. */
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

    /**
     * Same mechanics as [ScreenshotTest.shoot] — see its comment for why `captureToImage()` and
     * `autoAdvance` are avoided — plus a [dark] switch, because half the point here is checking
     * both palettes.
     */
    private fun shoot(name: String, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            VestraTheme(darkTheme = dark) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .padding(SpacingTokens.section),
                ) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(750)

        val view = compose.activity.window.decorView
        if (view.width == 0 || view.height == 0) {
            val w = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
            val h = android.view.View.MeasureSpec.makeMeasureSpec(2400, android.view.View.MeasureSpec.EXACTLY)
            view.measure(w, h)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(android.graphics.Canvas(bitmap))

        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("screenshot: ${File(dir, "$name.png").absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    private fun settings() = AppSettings(MemorySettings())

    /**
     * Five configured services with the counts that used to break. The real list is always
     * exactly these five (`ApiKeyDataStore` hardcodes them), so this is the production shape,
     * not a contrived worst case.
     */
    private fun usageData(): ApiKeyDataStore.ApiUsageDashboardData {
        val services = listOf(
            Triple("GEMINI", "Google Gemini", true),
            Triple("HF", "Hugging Face", true),
            Triple("GROQ", "Groq", false),
            Triple("OPENROUTER", "OpenRouter", false),
            Triple("ON_DEVICE", "On-Device", true),
        ).mapIndexed { index, (key, name, configured) ->
            ApiKeyDataStore.ServiceUsageSummary(
                serviceKey = key,
                serviceName = name,
                isConfigured = configured,
                requestCount = index * 7,
                successCount = index * 7,
                tokensIn = index * 1_100,
                tokensOut = index * 900,
                totalTokens = index * 2_000,
                avgLatencyMs = (index * 130).toLong(),
            )
        }
        return ApiKeyDataStore.ApiUsageDashboardData(
            totalRequests = services.sumOf { it.requestCount },
            successfulRequests = services.sumOf { it.successCount },
            totalTokensIn = services.sumOf { it.tokensIn },
            totalTokensOut = services.sumOf { it.tokensOut },
            totalTokens = services.sumOf { it.totalTokens },
            totalEstCostUsd = 0.0,
            services = services,
            avgLatencyMs = 240L,
        )
    }

    // ── The usage dashboard: the vertical-text bug's home ────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `30 usage dashboard narrow dark`() {
        shoot("30-usage-dashboard-360-dark") {
            ApiUsageDashboardCard(data = usageData(), initiallyExpanded = true)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `31 usage dashboard narrow light`() {
        shoot("31-usage-dashboard-360-light", dark = false) {
            ApiUsageDashboardCard(data = usageData(), initiallyExpanded = true)
        }
    }

    @Test
    fun `32 usage dashboard wide dark`() {
        shoot("32-usage-dashboard-411-dark") {
            ApiUsageDashboardCard(data = usageData(), initiallyExpanded = true)
        }
    }

    /** Zero state: every count is 0, which is where the one-character columns showed up. */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `33 usage dashboard empty narrow`() {
        shoot("33-usage-dashboard-empty-360") {
            ApiUsageDashboardCard(
                data = ApiKeyDataStore.ApiUsageDashboardData(
                    services = listOf(
                        ApiKeyDataStore.ServiceUsageSummary("GEMINI", "Google Gemini", false),
                        ApiKeyDataStore.ServiceUsageSummary("HF", "Hugging Face", false),
                        ApiKeyDataStore.ServiceUsageSummary("GROQ", "Groq", false),
                        ApiKeyDataStore.ServiceUsageSummary("OPENROUTER", "OpenRouter", false),
                        ApiKeyDataStore.ServiceUsageSummary("ON_DEVICE", "On-Device", true),
                    ),
                ),
                initiallyExpanded = true,
            )
        }
    }

    // ── The composer: model name vs blocked reason ───────────────────────────────────────

    /**
     * The exact regression from the reported screenshots: this consent sentence used to be passed
     * as `modelLabel` and rendered as "Pick a cloud model in the model pi…" inside the chip.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `34 composer blocked reason narrow`() {
        shoot("34-composer-blocked-360") {
            var prompt by remember { mutableStateOf("") }
            PromptComposer(
                prompt = prompt,
                onPromptChange = { prompt = it },
                modelLabel = "FLUX.1 schnell",
                blockedReason = "Pick a cloud model in the model picker, or add a free API key in " +
                    "Settings, to use Hugging Face — nothing is sent to the network until you do.",
                busy = false,
                enabled = true,
                onSend = {},
                onStop = {},
                onModelClick = {},
                placeholder = "Ask Lookbook to create anything…",
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `35 composer ready narrow light`() {
        shoot("35-composer-ready-360-light", dark = false) {
            PromptComposer(
                prompt = "A flowing linen abaya in warm sand, studio lighting",
                onPromptChange = {},
                modelLabel = "Bonsai Image 4B (LiteRT) · Ready offline",
                busy = false,
                enabled = true,
                onSend = {},
                onStop = {},
                onModelClick = {},
                onAddReference = {},
                placeholder = "Ask Lookbook to create anything…",
            )
        }
    }

    @Test
    fun `36 composer busy wide`() {
        shoot("36-composer-busy-411") {
            PromptComposer(
                prompt = "Slow pan across a rack of autumn coats",
                onPromptChange = {},
                modelLabel = "LTX Video (ZeroGPU)",
                busy = true,
                enabled = true,
                onSend = {},
                onStop = {},
                onModelClick = {},
            )
        }
    }

    // ── Home: top bar, empty state, modality chips ───────────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `37 home empty state narrow`() {
        shoot("37-home-empty-360") {
            Column(Modifier.fillMaxWidth()) {
                UnifiedTopBar(onOpenLibrary = {}, onOpenSettings = {})
                HomeEmptyState(
                    mode = ComposerMode.IMAGE,
                    suggestions = ComposerMode.IMAGE.suggestions,
                    onSuggestion = {},
                )
                Spacer(Modifier.height(12.dp))
                ModalityChipRow(selected = ComposerMode.IMAGE, onSelect = {})
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `38 home empty state narrow light`() {
        shoot("38-home-empty-360-light", dark = false) {
            Column(Modifier.fillMaxWidth()) {
                UnifiedTopBar(onOpenLibrary = {}, onOpenSettings = {})
                HomeEmptyState(
                    mode = ComposerMode.CHAT,
                    suggestions = ComposerMode.CHAT.suggestions,
                    onSuggestion = {},
                )
                Spacer(Modifier.height(12.dp))
                ModalityChipRow(selected = ComposerMode.CHAT, onSelect = {})
            }
        }
    }

    /** Every mode selected in turn — the five chips must fit one row at 360dp in all cases. */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `39 modality chips all states narrow`() {
        shoot("39-modality-chips-360") {
            Column(Modifier.fillMaxWidth()) {
                ComposerMode.entries.forEach { mode ->
                    ModalityChipRow(selected = mode, onSelect = {})
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    // ── Settings sub-pages ───────────────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `40 notifications screen narrow`() {
        shoot("40-notifications-360") {
            NotificationsScreen(appSettings = settings(), onBack = {})
        }
    }

    @Test
    fun `41 notifications screen wide light`() {
        shoot("41-notifications-411-light", dark = false) {
            NotificationsScreen(appSettings = settings(), onBack = {})
        }
    }

    // `packManager` is null here — the screen is written to degrade to "no packs" rather than
    // require a real ModelPackManager, which needs Android file system and network.
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `42 models screen narrow`() {
        shoot("42-models-360") {
            ModelsScreen(
                appSettings = settings(),
                packManager = null,
                onOpenProvider = {},
                onOpenPacks = {},
                onOpenDefaults = {},
                onBack = {},
            )
        }
    }

    @Test
    fun `43 models screen wide light`() {
        shoot("43-models-411-light", dark = false) {
            ModelsScreen(
                appSettings = settings(),
                packManager = null,
                onOpenProvider = {},
                onOpenPacks = {},
                onOpenDefaults = {},
                onBack = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `44 default models screen narrow`() {
        shoot("44-default-models-360") {
            DefaultModelsScreen(
                appSettings = settings(),
                freeCloudDiscovery = null,
                packManager = null,
                onBack = {},
            )
        }
    }

    @Test
    fun `45 default models screen wide light`() {
        shoot("45-default-models-411-light", dark = false) {
            DefaultModelsScreen(
                appSettings = settings(),
                freeCloudDiscovery = null,
                packManager = null,
                onBack = {},
            )
        }
    }

    // ApiMonitorScreen reads its data from VestraApp, which does not exist under a plain
    // test Application — so this renders its genuine zero state, which is what a fresh
    // install sees and the case the "0% success" trap lives in.
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `46 api monitor empty narrow`() {
        shoot("46-api-monitor-empty-360") {
            ApiMonitorScreen(onBack = {}, onOpenKeys = {})
        }
    }

    @Test
    fun `47 api monitor empty wide light`() {
        shoot("47-api-monitor-411-light", dark = false) {
            ApiMonitorScreen(onBack = {}, onOpenKeys = {})
        }
    }
}
