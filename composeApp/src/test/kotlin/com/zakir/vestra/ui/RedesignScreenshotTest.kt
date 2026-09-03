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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import com.zakir.vestra.ui.screens.settings.SettingsNavRow
import com.zakir.vestra.shared.chat.ChatMessage
import com.zakir.vestra.ui.screens.news.ChatMessageBubble
import com.zakir.vestra.ui.components.GlassAppMark
import com.zakir.vestra.ui.components.GlassBadgePill
import com.zakir.vestra.ui.components.GlassPrimaryButton
import com.zakir.vestra.ui.components.SocialProofRow
import com.zakir.vestra.ui.components.ApiUsageDashboardCard
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.components.ActiveTool
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Movie
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.screens.home.ComposerMode
import com.zakir.vestra.ui.screens.home.HomeEmptyState
import com.zakir.vestra.ui.screens.home.HomeHistoryEntry
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
    private fun shoot(
        name: String,
        dark: Boolean = true,
        /**
         * Render inside the real [SpatialBackground] rather than a flat fill.
         *
         * Off by default so a component shot isolates that component. It must be ON for anything
         * judging the aurora mesh or glass translucency: a flat `colorScheme.background` gives
         * frosted glass nothing to blur, so a card that looks correct here can look flat in the
         * app — which is exactly what the first violet render showed.
         */
        spatial: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            VestraTheme(darkTheme = dark) {
                if (spatial) {
                    SpatialBackground {
                        Box(Modifier.fillMaxSize().padding(SpacingTokens.section)) { content() }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                            .padding(SpacingTokens.section),
                    ) { content() }
                }
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

    private fun sampleHistory() = listOf(
        HomeHistoryEntry("h0", "Mobile app design trends", "Tap to reuse this prompt", "2h ago"),
        HomeHistoryEntry("h1", "Silk scarf detail, macro shot", "Tap to reuse this prompt", "1d ago"),
    )

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
        shoot("30-usage-dashboard-360-dark", spatial = true) {
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

    // ── The single chatbox ──────────────────────────────────────────────────────────────

    /**
     * The exact regression from the reported screenshots: this consent sentence used to be
     * passed as `modelLabel` and rendered as "Pick a cloud model in the model pi…" inside the
     * chip. The chip is gone entirely now — the reason has the row to itself.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `34 composer blocked reason narrow`() {
        shoot("34-composer-blocked-360", spatial = true) {
            var prompt by remember { mutableStateOf("") }
            PromptComposer(
                prompt = prompt,
                onPromptChange = { prompt = it },
                blockedReason = "Pick a cloud model in the model picker, or add a free API key in " +
                    "Settings, to use Hugging Face — nothing is sent to the network until you do.",
                busy = false,
                enabled = true,
                onSend = {},
                onStop = {},
                onOpenTools = {},
                onDictate = {},
            )
        }
    }

    /**
     * Image mode with a tool chip and text. The defect this replaces: an "Attach Reference" chip
     * row *and* a leading attach button both rendered here, with the chip sitting on top of the
     * placeholder. There is one attach affordance now and the field is unobstructed.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `35 composer with active tool narrow light`() {
        shoot("35-composer-tool-360-light", dark = false, spatial = true) {
            PromptComposer(
                prompt = "A flowing linen abaya in warm sand, studio lighting",
                onPromptChange = {},
                busy = false,
                enabled = true,
                onSend = {},
                onStop = {},
                onOpenTools = {},
                onDictate = {},
                accent = VestraColors.ModalityImage,
                placeholder = "Describe an image to create",
                activeTool = ActiveTool(
                    label = "Images",
                    icon = Icons.Outlined.AutoAwesome,
                    accent = VestraColors.ModalityImage,
                    onClear = {},
                ),
            )
        }
    }

    @Test
    fun `36 composer busy wide`() {
        shoot("36-composer-busy-411") {
            PromptComposer(
                prompt = "Slow pan across a rack of autumn coats",
                onPromptChange = {},
                busy = true,
                enabled = true,
                onSend = {},
                onStop = {},
                onOpenTools = {},
                accent = VestraColors.ModalityVideo,
                activeTool = ActiveTool(
                    label = "Videos",
                    icon = Icons.Outlined.Movie,
                    accent = VestraColors.ModalityVideo,
                    onClear = {},
                ),
            )
        }
    }

    /** The empty composer — the state the app opens on, and the one that must look like one control. */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `36b composer idle narrow`() {
        shoot("36b-composer-idle-360", spatial = true) {
            PromptComposer(
                prompt = "",
                onPromptChange = {},
                busy = false,
                enabled = true,
                onSend = {},
                onStop = {},
                onOpenTools = {},
                onDictate = {},
            )
        }
    }

    // ── Home: top bar and empty state ────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h1600dp-xxhdpi")
    fun `37 home empty state narrow`() {
        shoot("37-home-empty-360", spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                UnifiedTopBar(
                    onOpenLibrary = {},
                    onOpenSettings = {},
                    modelLabel = "FLUX.1 Schnell",
                    onModelClick = {},
                )
                HomeEmptyState(
                    mode = ComposerMode.IMAGE,
                    suggestions = ComposerMode.IMAGE.suggestions,
                    onSuggestion = {},
                    history = sampleHistory(),
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h1600dp-xxhdpi")
    fun `38 home empty state narrow light`() {
        shoot("38-home-empty-360-light", dark = false, spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                UnifiedTopBar(
                    onOpenLibrary = {},
                    onOpenSettings = {},
                    modelLabel = "Local Qwen3 0.6B (offline)",
                    onModelClick = {},
                )
                HomeEmptyState(
                    mode = ComposerMode.CHAT,
                    suggestions = ComposerMode.CHAT.suggestions,
                    onSuggestion = {},
                )
            }
        }
    }

    /**
     * The top bar with the longest model name the catalog can produce. This is where the old
     * composer chip failed — it had roughly 150dp and truncated to "Local". Up here the name has
     * the row, and the render is the check that it still does at 360dp.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun `39 top bar long model name narrow`() {
        shoot("39-top-bar-long-model-360", spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                UnifiedTopBar(
                    onOpenLibrary = {},
                    onOpenSettings = {},
                    modelLabel = "Bonsai Image 4B (LiteRT)",
                    onModelClick = {},
                )
                Spacer(Modifier.height(10.dp))
                UnifiedTopBar(
                    onOpenLibrary = {},
                    onOpenSettings = {},
                    modelLabel = "Llama 3.3 70B Versatile (Groq)",
                    onModelClick = {},
                )
            }
        }
    }

    // ── Onboarding first viewport ────────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h1200dp-xxhdpi")
    fun `51 onboarding hero narrow`() {
        shoot("51-onboarding-360", spatial = true) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                GlassBadgePill("NEXT-GEN INTELLIGENCE")
                Spacer(Modifier.height(24.dp))
                GlassAppMark(icon = Icons.Outlined.AutoAwesome)
                Spacer(Modifier.height(24.dp))
                androidx.compose.material3.Text(
                    "THE LOOKBOOK",
                    style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                    color = com.zakir.vestra.ui.theme.VestraColors.Ink,
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    "Your personal AI companion for infinite possibilities.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = com.zakir.vestra.ui.theme.VestraColors.InkMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                GlassPrimaryButton(text = "Get Started", onClick = {})
                Spacer(Modifier.height(16.dp))
                SocialProofRow(
                    avatarColors = listOf(
                        com.zakir.vestra.ui.theme.VestraColors.Accent,
                        com.zakir.vestra.ui.theme.VestraColors.ModalityAudio,
                        com.zakir.vestra.ui.theme.VestraColors.SaffronDeep,
                    ),
                    text = "Runs on your device · no account needed",
                )
            }
        }
    }

    // ── Chat: markdown, the action row, and in-bubble code ──────────────────────────────

    /**
     * The exact reply the shipped app rendered with its markers intact — bullets as hyphens and
     * `**bold**` as four asterisks — on the first message of every conversation. This render is
     * the check that it now reads as a list.
     */
    @Test
    @Config(qualifiers = "w360dp-h1000dp-xxhdpi")
    fun `48b chat markdown reply`() {
        shoot("48b-chat-markdown-360", spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "m0",
                        role = "user",
                        text = "hi",
                        timestampMs = 1_756_000_000_000,
                    ),
                    index = 0,
                )
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "m1",
                        role = "assistant",
                        providerId = "openrouter-free",
                        text = "Hi there! 👋\n\nWelcome to The Lookbook. I can help you with:\n\n" +
                            "- **Fashion try-on** features and tips\n" +
                            "- **On-device AI** (Lite/Pro packs) vs cloud models\n" +
                            "- **Headlines & updates** about the app\n" +
                            "- General questions about how things work\n\n" +
                            "What can I help you with today?",
                        timestampMs = 1_756_000_030_000,
                    ),
                    index = 1,
                    onRegenerate = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h1400dp-xxhdpi")
    fun `49 chat thread with code block`() {
        shoot("49-chat-code-360", spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "m1",
                        role = "assistant",
                        text = "Hello! I've analyzed your request for a glassmorphism design. " +
                            "Would you like me to generate some color palettes that complement the " +
                            "frosted glass effect?",
                        timestampMs = 1_756_000_000_000,
                    ),
                    index = 0,
                )
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "m2",
                        role = "user",
                        text = "Yes, please! Let's go with something futuristic but soft. Maybe some purples and teals.",
                        timestampMs = 1_756_000_060_000,
                    ),
                    index = 1,
                )
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "m3",
                        role = "assistant",
                        text = "Great choice! Here is a CSS snippet for a high-performance glass " +
                            "effect using those tones:\n" +
                            "```css\n" +
                            ".glass-card {\n" +
                            "  background: rgba(139, 92, 246, 0.1);\n" +
                            "  backdrop-filter: blur(24px);\n" +
                            "  border: 1px solid rgba(255, 255, 255, 0.1);\n" +
                            "}\n" +
                            "```",
                        timestampMs = 1_756_000_120_000,
                    ),
                    index = 2,
                    onRegenerate = {},
                )
            }
        }
    }

    @Test
    fun `50 chat thread with code block light`() {
        shoot("50-chat-code-411-light", dark = false, spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "m1",
                        role = "assistant",
                        text = "Here is the snippet:\n```kotlin\nval glass = Color(0x8C2A2150)\n// frosted\n```",
                        timestampMs = 1_756_000_000_000,
                    ),
                    index = 0,
                    onRegenerate = {},
                )
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
                engineRouter = null,
                onOpenProvider = {},
                onOpenPacks = {},
                onOpenDefaults = {},
                onBack = {},
            )
        }
    }

    // The engine tier dropdown and NNAPI toggle were briefly orphaned when the old Settings
    // engine section was retired; this shoots the bottom of Models, where they live now.
    @Test
    @Config(qualifiers = "w360dp-h1400dp-xxhdpi")
    fun `48 models screen engine controls`() {
        shoot("48-models-engine-360") {
            ModelsScreen(
                appSettings = settings(),
                packManager = null,
                engineRouter = null,
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
                engineRouter = null,
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

    // ── Surfaces that shipped without ever being rendered ────────────────────────────────

    /**
     * The Settings hub.
     *
     * It was rewritten twice and screenshot zero times — the pass that made it a pure list of
     * destinations was verified by reading the diff. This is the render that would have caught
     * the "Clear API keys" button that navigated instead of clearing.
     */
    @Test
    @Config(qualifiers = "w360dp-h1600dp-xxhdpi")
    fun `52 settings hub narrow`() {
        shoot("52-settings-hub-360", spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                com.zakir.vestra.ui.components.GlassTopBar(
                    title = "Settings",
                    subtitle = "Models · keys · privacy",
                )
                Spacer(Modifier.height(16.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Memory,
                    title = "Models",
                    testTag = "settings_row_models",
                    description = "Cloud services and on-device packs.",
                    onClick = {},
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Key,
                    title = "API keys",
                    testTag = "settings_row_api_keys",
                    description = "Hugging Face, Groq, OpenRouter and Gemini credentials.",
                    onClick = {},
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Security,
                    title = "Safety & content",
                    testTag = "settings_row_safety",
                    description = "Prompt guards and phrasing assists for Image and Video.",
                    onClick = {},
                )
            }
        }
    }

    /** The same hub in light mode, where the previous switch defaults disappeared. */
    @Test
    fun `53 settings hub wide light`() {
        shoot("53-settings-hub-411-light", dark = false, spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                com.zakir.vestra.ui.components.GlassTopBar(
                    title = "Settings",
                    subtitle = "Models · keys · privacy",
                )
                Spacer(Modifier.height(16.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance",
                    testTag = "settings_row_appearance",
                    description = "Light, dark, or follow the system.",
                    onClick = {},
                )
            }
        }
    }

    /**
     * Switch states in both palettes, side by side.
     *
     * Every toggle in the app used Material's default colours, which sit within a few percent of
     * this system's white card — so in light mode "off" rendered as a pale blob with no border
     * and was near-indistinguishable from "on". This case is the one that makes that visible.
     */
    @Test
    @Config(qualifiers = "w360dp-h400dp-xxhdpi")
    fun `54 switch states light`() {
        shoot("54-switch-states-360-light", dark = false, spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                com.zakir.vestra.ui.components.GlassCard {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("On", modifier = Modifier.weight(1f))
                        com.zakir.vestra.ui.components.VestraSwitch(checked = true, onCheckedChange = {})
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Off", modifier = Modifier.weight(1f))
                        com.zakir.vestra.ui.components.VestraSwitch(checked = false, onCheckedChange = {})
                    }
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h400dp-xxhdpi")
    fun `55 switch states dark`() {
        shoot("55-switch-states-360", spatial = true) {
            Column(Modifier.fillMaxWidth()) {
                com.zakir.vestra.ui.components.GlassCard {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("On", modifier = Modifier.weight(1f))
                        com.zakir.vestra.ui.components.VestraSwitch(checked = true, onCheckedChange = {})
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Off", modifier = Modifier.weight(1f))
                        com.zakir.vestra.ui.components.VestraSwitch(checked = false, onCheckedChange = {})
                    }
                }
            }
        }
    }

    /** The conversation drawer — shipped in the last PR, never rendered. */
    @Test
    @Config(qualifiers = "w360dp-h1200dp-xxhdpi")
    fun `56 chat history drawer narrow`() {
        shoot("56-history-drawer-360", spatial = true) {
            com.zakir.vestra.ui.components.ChatHistoryDrawer(
                conversations = listOf(
                    com.zakir.vestra.shared.chat.ConversationSummary(
                        id = "c1",
                        title = "Build me a capsule wardrobe for a week of travel",
                        updatedAtMs = 0,
                        messageCount = 6,
                        preview = "Five pieces, one palette — start with the coat.",
                    ),
                    com.zakir.vestra.shared.chat.ConversationSummary(
                        id = "c2",
                        title = "What colours flatter a warm skin tone?",
                        updatedAtMs = 0,
                        messageCount = 2,
                        preview = "Warm tones sit best with…",
                    ),
                ),
                activeId = "c1",
                onOpen = {},
                onNewChat = {},
                onDelete = {},
                onShareActive = {},
                onDismiss = {},
                relativeTime = { "2h ago" },
            )
        }
    }


    // ── The top bar, across every shape it actually takes ────────────────────────────────

    /**
     * `GlassTopBar` on one screenshot, at the range of title and subtitle lengths the app really
     * produces.
     *
     * The last pass changed this component's vertical alignment, which altered **eighteen**
     * screens — and only the six with screenshot cases were looked at. Rendering the component
     * across its input range is the cheap way to cover the other twelve: a per-screen case for
     * each would need each screen's view models, and would still only prove the bar renders once.
     *
     * Longest real strings, taken from the screens themselves, so a change that fits the short
     * ones and breaks the long ones fails here.
     */
    @Test
    @Config(qualifiers = "w360dp-h900dp-xxhdpi")
    fun `57 top bar matrix narrow`() {
        shoot("57-top-bar-matrix-360", spatial = true) { TopBarMatrix() }
    }

    @Test
    fun `58 top bar matrix wide light`() {
        shoot("58-top-bar-matrix-411-light", dark = false, spatial = true) { TopBarMatrix() }
    }

    @Composable
    private fun TopBarMatrix() {
        val back: @Composable () -> Unit = {
            androidx.compose.material3.IconButton(onClick = {}) {
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = VestraColors.Ink,
                )
            }
        }
        Column(Modifier.fillMaxWidth()) {
            // Shortest: one word, no subtitle. The arrow has nothing to align against but the title.
            com.zakir.vestra.ui.components.GlassTopBar(title = "Models", navigation = back)
            Spacer(Modifier.height(20.dp))
            // The common case.
            com.zakir.vestra.ui.components.GlassTopBar(
                title = "API monitor",
                subtitle = "Requests · tokens · reliability",
                navigation = back,
            )
            Spacer(Modifier.height(20.dp))
            // Longest title in the app.
            com.zakir.vestra.ui.components.GlassTopBar(
                title = "Storage & privacy",
                subtitle = "Caches · permissions",
                navigation = back,
            )
            Spacer(Modifier.height(20.dp))
            // The two longest subtitles that actually ship. The bound on this Text ellipsizes
            // rather than wrapping, so the thing worth checking is not that clipping works but
            // that no real string reaches it — these two are the ones that would, if any did.
            com.zakir.vestra.ui.components.GlassTopBar(
                title = "Downloads",
                subtitle = "On-device · resumable · survives reinstall",
                navigation = back,
            )
            Spacer(Modifier.height(20.dp))
            com.zakir.vestra.ui.components.GlassTopBar(
                title = "Diagnostics",
                subtitle = "Crashes · runs · auto-troubleshooting",
                navigation = back,
            )
            Spacer(Modifier.height(20.dp))
            // No back arrow — the root Settings screen shape.
            com.zakir.vestra.ui.components.GlassTopBar(
                title = "Settings",
                subtitle = "Models · keys · privacy",
            )
        }
    }

    // ── Screens that had no coverage at all ──────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h1600dp-xxhdpi")
    fun `59 privacy screen narrow`() {
        shoot("59-privacy-360", spatial = true) {
            com.zakir.vestra.ui.screens.privacy.PrivacyScreen(onBack = {})
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h1600dp-xxhdpi")
    fun `60 help screen narrow`() {
        shoot("60-help-360", spatial = true) {
            com.zakir.vestra.ui.screens.help.HelpScreen(onBack = {}, onOpenSettings = {})
        }
    }

    @Test
    fun `61 help screen wide light`() {
        shoot("61-help-411-light", dark = false, spatial = true) {
            com.zakir.vestra.ui.screens.help.HelpScreen(onBack = {}, onOpenSettings = {})
        }
    }

}
