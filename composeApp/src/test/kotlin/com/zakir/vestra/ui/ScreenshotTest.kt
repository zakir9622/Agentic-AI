package com.zakir.vestra.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.Composable
import com.russhwolf.settings.Settings
import com.zakir.vestra.shared.chat.ChatMessage
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.news.NewsItem
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.components.LiteRtStatusIndicator
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.QuickPromptCarousel
import com.zakir.vestra.ui.components.QuickPromptItem
import com.zakir.vestra.ui.screens.news.ChatEmptyState
import com.zakir.vestra.ui.screens.news.ChatMessageBubble
import com.zakir.vestra.ui.screens.news.ChatTypingIndicator
import com.zakir.vestra.ui.screens.news.NewsHeadlinesBar
import com.zakir.vestra.ui.screens.settings.settingsCloudKeysSection
import com.zakir.vestra.ui.screens.settings.settingsMemorySection
import com.zakir.vestra.ui.screens.settings.settingsSafetySection
import com.zakir.vestra.ui.theme.VestraTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Real pixel screenshots of the UI, rendered on the JVM.
 *
 * GraphicsMode.NATIVE makes Robolectric rasterise for real, so drawing the view yields actual
 * pixels rather than a blank buffer. That gives a way to *look* at the UI in an environment with
 * no device, emulator or KVM — which is how the vertical-text regression reached a release build
 * unnoticed.
 *
 * PNGs land in composeApp/build/screenshots/.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w411dp-h914dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

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

    private fun shoot(name: String, content: @Composable () -> Unit) {
        // Deliberately NOT captureToImage(): that goes through forceRedraw(), which blocks on a
        // real window draw callback that never fires without a surface, so it always times out
        // under Robolectric. Drawing the decor view straight onto a software Canvas produces the
        // same pixels with no window involved.
        //
        // The clock is driven manually because the UI runs infinite animations (accent glow), so
        // the composition never reports idle and any wait-for-idle would hang.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            VestraTheme(darkTheme = true) {
                Box(
                    androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
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

    /**
     * The composer docked at the bottom of a studio pane. Used to shoot the header status box
     * that sat above it (a long provider string beside chips that once regressed into
     * one-character-wide columns of vertical text) — that box was deleted entirely in 3.1.6
     * (it could show a stale/wrong model name; the composer's own model chip is now the sole
     * status indicator), so only the composer shape remains worth a screenshot here.
     */
    @Test
    fun studioComposerDock() {
        shoot("03-studio-composer-dock") {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 10.dp, top = 4.dp),
            ) {
                PromptComposer(
                    prompt = "i want a russian girl riding a horse",
                    onPromptChange = {},
                    modelLabel = "Local tiny-SD (offline)",
                    assistCount = 1,
                    busy = false,
                    enabled = true,
                    onModelClick = {},
                    onAssistsClick = {},
                    onSend = {},
                    onStop = {},
                )
            }
        }
    }

    /** Code output: prose separated from fenced blocks, each block copyable. */
    @Test
    fun codeOutputBlocks() {
        val answer = """
            Use a frosted card like this:

            ```kotlin
            @Composable
            fun GlassCard(content: @Composable () -> Unit) {
                Surface(shape = RoundedCornerShape(24.dp)) { content() }
            }
            ```

            Then build it:

            ```bash
            ./gradlew :composeApp:assembleSideloadDebug
            ```
        """.trimIndent()
        shoot("07-code-output-blocks") {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.padding(18.dp),
            ) {
                com.zakir.vestra.ui.components.GlassSectionLabel("CODE · 412 free tokens")
                com.zakir.vestra.ui.components.CodeOutput(text = answer)
            }
        }
    }

    /** Produced-audio list with inline playback controls. */
    @Test
    fun audioClipList() {
        val clips = listOf(
            com.zakir.vestra.audio.AudioClip(
                path = "/tmp/voice_1787500000000.wav",
                kind = com.zakir.vestra.audio.AudioClipKind.CONVERTED,
                savedAtMs = 1787500000000L,
                bytes = 1_482_112,
                durationMs = 14_000,
            ),
            com.zakir.vestra.audio.AudioClip(
                path = "/tmp/mic_1787499000000.wav",
                kind = com.zakir.vestra.audio.AudioClipKind.RECORDING,
                savedAtMs = 1787499000000L,
                bytes = 962_560,
                durationMs = 9_000,
            ),
            com.zakir.vestra.audio.AudioClip(
                path = "/tmp/sys_tts_1787498000000.wav",
                kind = com.zakir.vestra.audio.AudioClipKind.SPEECH,
                savedAtMs = 1787498000000L,
                bytes = 331_776,
                durationMs = 3_000,
            ),
        )
        shoot("08-audio-clip-list") {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.padding(18.dp),
            ) {
                com.zakir.vestra.ui.components.GlassSectionLabel("CLIPS")
                com.zakir.vestra.ui.components.AudioClipList(
                    clips = clips,
                    onShare = {},
                    onDelete = {},
                )
            }
        }
    }

    /** Warm-up states — written but never seen rendered until now. */
    @Test
    fun warmupLoading() {
        shoot("09-warmup-loading") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                com.zakir.vestra.ui.components.GlassCard {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = androidx.compose.ui.Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = com.zakir.vestra.ui.theme.VestraColors.Accent,
                        )
                        androidx.compose.foundation.layout.Spacer(
                            androidx.compose.ui.Modifier.width(10.dp),
                        )
                        androidx.compose.foundation.layout.Column {
                            androidx.compose.material3.Text(
                                "Initializing Local Qwen3 0.6B (fast)",
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                color = com.zakir.vestra.ui.theme.VestraColors.Ink,
                            )
                            androidx.compose.material3.Text(
                                "First load only — this can take up to a minute.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = com.zakir.vestra.ui.theme.VestraColors.InkMuted,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun warmupReadyAndFailed() {
        shoot("10-warmup-ready-failed") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                com.zakir.vestra.ui.components.GlassPill(
                    text = "Local Qwen3 0.6B (fast) · loaded and ready",
                    active = true,
                )
                androidx.compose.foundation.layout.Spacer(
                    androidx.compose.ui.Modifier.height(12.dp),
                )
                com.zakir.vestra.ui.components.GlassErrorBanner(
                    message = "Local image gen (tiny-SD) could not load: Local SD-Turbo weights " +
                        "incomplete (unet.onnx). Re-download local-sdturbo-v1.",
                    onRetry = {},
                    retryLabel = "Retry load",
                    onDismiss = null,
                )
            }
        }
    }

    /** The composer as it renders docked at the bottom of the studio. */
    @Test
    fun composerDock() {
        shoot("05-composer-dock") {
            PromptComposer(
                prompt = "Emerald abaya in a Lahore bazaar, soft afternoon light",
                onPromptChange = {},
                modelLabel = "Local tiny-SD (offline)",
                assistCount = 2,
                busy = false,
                enabled = true,
                onModelClick = {},
                onAssistsClick = {},
                onSend = {},
                onStop = {},
                placeholder = "Describe the image…",
            )
        }
    }

    @Test
    fun composerDockBusy() {
        shoot("06-composer-dock-busy") {
            PromptComposer(
                prompt = "Emerald abaya in a Lahore bazaar",
                onPromptChange = {},
                modelLabel = "Local Qwen3 0.6B (offline)",
                assistCount = 0,
                busy = true,
                enabled = true,
                onModelClick = {},
                onSend = {},
                onStop = {},
            )
        }
    }

    // --- 3.1.1 GoogleLookBookUI-ported UI, rendered for real to confirm the port visually ---
    // bottomDockFloatingPill was removed with LookbookBottomBar — the app has no bottom dock any
    // more (see UnifiedMainScreen.kt).

    @Test
    fun chatBubbleUserAndAssistant() {
        shoot("12-chat-bubbles") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "1",
                        role = "user",
                        text = "Discuss modest winter layering for a client shoot in Lahore.",
                        timestampMs = 1787500000000L,
                    ),
                    index = 0,
                )
                ChatMessageBubble(
                    message = ChatMessage(
                        id = "2",
                        role = "assistant",
                        text = "Layer a merino base under a structured wool abaya, keep the palette " +
                            "muted, and add a textured shawl for warmth without bulk.",
                        timestampMs = 1787500030000L,
                        providerId = "local-gemma-4-e2b-v1",
                    ),
                    index = 1,
                    modelDisplayName = "Local Gemma 4 (offline)",
                )
            }
        }
    }

    @Test
    fun chatTypingIndicator() {
        shoot("13-chat-typing-indicator") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                ChatTypingIndicator(modelLabel = "Local Gemma 4 (offline)")
            }
        }
    }

    @Test
    fun chatEmptyStateWithStarterPrompts() {
        shoot("14-chat-empty-state") {
            ChatEmptyState(onPromptSelected = {})
        }
    }

    @Test
    fun newsHeadlinesBarExpanded() {
        shoot("15-news-headlines-bar") {
            NewsHeadlinesBar(
                newsItems = listOf(
                    NewsItem(id = "1", title = "Modest fashion trends for 2026", link = "https://example.com/1", publishedMs = 0L, source = "Vogue"),
                    NewsItem(id = "2", title = "On-device AI reshapes mobile creative tools", link = "https://example.com/2", publishedMs = 0L, source = "TechCrunch"),
                    NewsItem(id = "3", title = "Runway silhouettes: structured drape returns", link = "https://example.com/3", publishedMs = 0L, source = "WWD"),
                ),
                refreshing = false,
                onRefresh = {},
                onHeadlineClick = { _, _ -> },
            )
        }
    }

    @Test
    fun quickPromptCarouselChips() {
        shoot("16-quick-prompt-carousel") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                QuickPromptCarousel(
                    prompts = listOf(
                        QuickPromptItem("Discuss this headline for modest fashion and on-device AI: Modest fashion trends for 2026", "Vogue"),
                        QuickPromptItem("What can this app do on-device?", "HELP"),
                    ),
                    onSelectPrompt = {},
                )
            }
        }
    }

    @Test
    fun liteRtStatusIndicatorReady() {
        shoot("17-litert-status-ready") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                LiteRtStatusIndicator(
                    modelName = "LiteRT Gemma 4 2B",
                    isInstalled = true,
                    isLoaded = true,
                    backend = "LiteRT GPU / CPU Fallback",
                )
            }
        }
    }

    @Test
    fun liteRtStatusIndicatorNotInstalled() {
        shoot("18-litert-status-not-installed") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                LiteRtStatusIndicator(
                    modelName = "LiteRT Gemma 4 2B",
                    isInstalled = false,
                    isLoaded = false,
                )
            }
        }
    }

    @Test
    fun cloudKeysConnectivityTest() {
        val settings = AppSettings(MemorySettings()).apply {
            setGroqApiKey("gsk_example_key_not_real")
        }
        val checker = com.zakir.vestra.shared.cloud.ProviderConnectivityChecker(
            com.zakir.vestra.shared.platformHttpClient(),
        )
        shoot("20-cloud-keys-connectivity") {
            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
            ) {}
            LazyColumn {
                settingsCloudKeysSection(
                    appSettings = settings,
                    connectivityChecker = checker,
                    hfTokenSaved = false,
                    hfInput = "",
                    groqInput = "gsk_example_key_not_real",
                    openRouterInput = "",
                    onHfInput = {},
                    onGroqInput = {},
                    onOpenRouterInput = {},
                    keysSavedFlash = false,
                    clipboardHint = null,
                    durableReady = false,
                    onApplyClipboard = { false },
                    onOpenPortal = {},
                    onSaveTokens = {},
                    importTokensLauncher = launcher,
                    onKeysLoadedFromDocuments = {},
                )
            }
        }
    }

    @Test
    fun liteRtStatusIndicatorError() {
        shoot("19-litert-status-error") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                LiteRtStatusIndicator(
                    modelName = "LiteRT Gemma 4 2B",
                    isInstalled = true,
                    isLoaded = false,
                    errorMessage = "Engine init failed: GPU delegate unavailable, CPU fallback also failed.",
                )
            }
        }
    }

    @Test
    fun safetyPresetSection() {
        val settings = AppSettings(MemorySettings())
        shoot("22-safety-presets") {
            LazyColumn {
                settingsSafetySection(appSettings = settings)
            }
        }
    }

    @Test
    fun solidCardOpaqueSurface() {
        shoot("21-solid-card") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                com.zakir.vestra.ui.components.SolidCard {
                    androidx.compose.material3.Text(
                        "A dense reading surface — solid-card exact match: opaque fill, same rim/shadow as GlassCard.",
                    )
                }
            }
        }
    }

    @Test
    fun contextBudgetBarUnderBudget() {
        val budget = com.zakir.vestra.shared.chat.ContextBudget.evaluate(
            usedTokens = 512,
            modelId = "local-qwen3-06b-v1",
        )
        shoot("23-context-budget-under") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                com.zakir.vestra.ui.screens.news.ContextBudgetBar(budget = budget, hasDraft = true)
            }
        }
    }

    @Test
    fun contextBudgetBarWillTruncate() {
        val budget = com.zakir.vestra.shared.chat.ContextBudget.evaluate(
            usedTokens = 9_000,
            modelId = "openrouter-free",
        )
        shoot("24-context-budget-truncate") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(18.dp)) {
                com.zakir.vestra.ui.screens.news.ContextBudgetBar(budget = budget, hasDraft = true)
            }
        }
    }

    // SafetyConfirmDialog is not screenshot-tested here: AlertDialog opens its own platform
    // Dialog window, and shoot() only rasterizes the activity's own decor view — the same
    // window-layer limitation PrivacyBlurFlowTest documents for ModalBottomSheet. It came back
    // a blank frame when tried, so it's verified instead by SafetyConfirmDialogTest's real
    // assertIsDisplayed()/performClick() interaction tests.

    @Test
    fun memorySectionEmptyState() {
        val settings = AppSettings(MemorySettings())
        val memory = com.zakir.vestra.shared.chat.MemoryRepository(MemorySettings())
        shoot("26-memory-empty") {
            LazyColumn {
                settingsMemorySection(appSettings = settings, memory = memory)
            }
        }
    }

    @Test
    fun memorySectionWithFacts() {
        val settings = AppSettings(MemorySettings())
        val memory = com.zakir.vestra.shared.chat.MemoryRepository(MemorySettings())
        memory.addFacts(listOf("Prefers dark mode", "Works with Kotlin", "Building a modest-fashion app"))
        shoot("27-memory-with-facts") {
            LazyColumn {
                settingsMemorySection(appSettings = settings, memory = memory)
            }
        }
    }
}
