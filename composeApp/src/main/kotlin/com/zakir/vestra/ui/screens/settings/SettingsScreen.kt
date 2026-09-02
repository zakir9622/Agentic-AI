package com.zakir.vestra.ui.screens.settings

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import com.zakir.vestra.ui.TestTags
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.packs.PackHandshakeWires
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.TokenPortals
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.storage.DurableStorage
import com.zakir.vestra.storage.TokenSidecar
import com.zakir.vestra.ui.components.GlassSnackbar
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SnackbarLevel
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.util.rememberPackDownloadStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings hub. Reached from the unified main screen's top-right gear icon.
 *
 * This was one flowing scroll carrying every setting the app has — engine tiers and pack
 * downloads, four API-key fields, five per-capability model dropdowns, safety presets, storage,
 * permissions, memory and about, in one column. Everything model-shaped moved out to
 * [ModelsScreen] / [DefaultModelsScreen], usage telemetry to [ApiMonitorScreen], and
 * notifications to [NotificationsScreen]; what stays here is either a hub row pointing at one of
 * those or a setting that genuinely isn't about models.
 *
 * The API-key card stays in place deliberately, even though each provider page also has its own
 * key field: this is the one screen that can take a whole tokens.json at once, and losing that
 * would trade one paste for four.
 */
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    packManager: ModelPackManager,
    freeCloudDiscovery: FreeCloudDiscovery,
    usageLedger: UsageLedger,
    memoryRepository: com.zakir.vestra.shared.chat.MemoryRepository? = null,
    onOpenPacks: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenChangelog: () -> Unit = {},
    onOpenDiagnostics: (() -> Unit)? = null,
    onOpenModels: () -> Unit = {},
    onOpenDefaultModels: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenApiMonitor: () -> Unit = {},
    onBack: () -> Unit,
) {
    val connectivityChecker = remember {
        com.zakir.vestra.shared.cloud.ProviderConnectivityChecker(com.zakir.vestra.shared.platformHttpClient())
    }
    val sectionTitle = LookbookCopy.STUDIO_SETTINGS
    val sectionSubtitle = "Models · keys · notifications · privacy"
    val context = LocalContext.current
    val selectedTier by appSettings.engineTier.collectAsState()
    val appearance by appSettings.appearanceMode.collectAsState()
    val packStates by packManager.states.collectAsState()
    val packCatalogError by packManager.lastError.collectAsState()
    val startDownload = rememberPackDownloadStarter(showToast = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { packManager.refresh() }

    val tryOnId by appSettings.cloudProviderId.collectAsState()
    val imageGenId by appSettings.imageGenProviderId.collectAsState()
    val imageEditId by appSettings.imageEditProviderId.collectAsState()
    val codeId by appSettings.codeProviderId.collectAsState()
    val videoId by appSettings.videoProviderId.collectAsState()
    val audioId by appSettings.audioProviderId.collectAsState()

    val hfToken by appSettings.hfToken.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()
    val geminiKey by appSettings.geminiApiKey.collectAsState()

    var hfInput by remember(hfToken) { mutableStateOf(hfToken.orEmpty()) }
    var groqInput by remember(groqKey) { mutableStateOf(groqKey.orEmpty()) }
    var openRouterInput by remember(openRouterKey) { mutableStateOf(openRouterKey.orEmpty()) }
    var geminiInput by remember(geminiKey) { mutableStateOf(geminiKey.orEmpty()) }
    var keysSavedFlash by remember { mutableStateOf(false) }
    var showTokenWizard by remember { mutableStateOf(false) }
    var confirmClearTokens by remember { mutableStateOf(false) }
    var clearingCache by remember { mutableStateOf(false) }
    var durableReady by remember { mutableStateOf(DurableStorage.hasAllFilesAccess()) }
    var clipboardHint by remember { mutableStateOf<String?>(null) }
    var permissionEpoch by remember { mutableStateOf(0) }

    val importTokensLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                TokenSidecar.importFromUri(context, uri, appSettings)
            }
            hfInput = appSettings.hfToken.value.orEmpty()
            groqInput = appSettings.groqApiKey.value.orEmpty()
            openRouterInput = appSettings.openRouterApiKey.value.orEmpty()
            geminiInput = appSettings.geminiApiKey.value.orEmpty()
            val dataStore = (context.applicationContext as? com.zakir.vestra.VestraApp)?.apiKeyDataStore
                ?: com.zakir.vestra.storage.ApiKeyDataStore(context)
            dataStore.saveAll(
                appSettings.hfToken.value,
                appSettings.groqApiKey.value,
                appSettings.openRouterApiKey.value,
                appSettings.geminiApiKey.value,
            )
            keysSavedFlash = count > 0
            GlassSnackbar.show(
                if (count > 0) "Imported $count key(s) from file" else "No HF/Groq/OpenRouter/Gemini keys found in file",
                if (count > 0) SnackbarLevel.SUCCESS else SnackbarLevel.WARNING,
            )
        }
    }

    fun applyClipboardToken(): Boolean {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return false
        val raw = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        val detected = TokenSidecar.detectClipboardToken(raw) ?: run {
            clipboardHint = null
            return false
        }
        when (detected.first) {
            TokenPortals.Kind.HF -> hfInput = detected.second
            TokenPortals.Kind.GROQ -> groqInput = detected.second
            TokenPortals.Kind.OPENROUTER -> openRouterInput = detected.second
            TokenPortals.Kind.GEMINI -> geminiInput = detected.second
        }
        clipboardHint = "Detected ${detected.first.name} key from clipboard — tap Save API keys"
        return true
    }

    fun openPortal(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            GlassSnackbar.show("No browser available", SnackbarLevel.ERROR)
        }
    }

    fun saveTokens() {
        val hf = hfInput.trim().ifBlank { null }
        val groq = groqInput.trim().ifBlank { null }
        val openRouter = openRouterInput.trim().ifBlank { null }
        val gemini = geminiInput.trim().ifBlank { null }
        appSettings.setHfToken(hf)
        appSettings.setGroqApiKey(groq)
        appSettings.setOpenRouterApiKey(openRouter)
        appSettings.setGeminiApiKey(gemini)
        // A key actually typed in and saved here is genuine interactive consent to use cloud
        // models — unlike TokenSidecar's boot-time restore, which calls the same setters.
        if (hf != null || groq != null || openRouter != null || gemini != null) {
            appSettings.confirmCloudConsentFromApiKeyEntry()
        }
        keysSavedFlash = true
        clipboardHint = null
        val saved = TokenSidecar.persist(context, appSettings)
        scope.launch {
            val dataStore = (context.applicationContext as? com.zakir.vestra.VestraApp)?.apiKeyDataStore
                ?: com.zakir.vestra.storage.ApiKeyDataStore(context)
            dataStore.saveAll(hf, groq, openRouter, gemini)
        }
        if (!saved && !DurableStorage.hasAllFilesAccess()) {
            GlassSnackbar.show(
                "Tokens saved securely in DataStore and app. Download a model pack to enable durable storage so keys survive reinstall.",
                SnackbarLevel.INFO,
            )
        }
        scope.launch {
            runCatching { freeCloudDiscovery.refreshRouterDiscovery(appSettings) }
        }
        if (hfInput.isNotBlank()) showTokenWizard = true
    }

    if (showTokenWizard) {
        TokenSetupWizard(
            onDismiss = { showTokenWizard = false },
            onOpenPortal = { openPortal(it) },
            hfConfigured = hfInput.isNotBlank(),
            groqConfigured = groqInput.isNotBlank(),
            openRouterConfigured = openRouterInput.isNotBlank(),
            geminiConfigured = geminiInput.isNotBlank(),
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                durableReady = DurableStorage.hasAllFilesAccess()
                permissionEpoch += 1
                applyClipboardToken()
                if (durableReady) {
                    scope.launch { packManager.refresh() }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val localPackChoices = remember { LocalModelCatalog.entries.filter { it.packId != null && it.runnable } }
    var selectedPackId by remember {
        val preferred = when (appSettings.engineTier.value) {
            EngineTier.LITE -> localPackChoices.firstOrNull { it.engineTier == EngineTier.LITE }?.packId
            EngineTier.PRO -> localPackChoices.firstOrNull { it.engineTier == EngineTier.PRO }?.packId
            EngineTier.AUTO, EngineTier.CLOUD -> null
        }
        mutableStateOf(preferred ?: localPackChoices.firstOrNull()?.packId.orEmpty())
    }
    var handshakeBusy by remember { mutableStateOf(false) }
    var handshakeDetail by remember { mutableStateOf<String?>(null) }
    var handshakeOk by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(selectedTier) {
        val match = when (selectedTier) {
            EngineTier.LITE -> localPackChoices.firstOrNull { it.engineTier == EngineTier.LITE }?.packId
            EngineTier.PRO -> localPackChoices.firstOrNull { it.engineTier == EngineTier.PRO }?.packId
            else -> null
        }
        if (match != null && selectedPackId != match) {
            selectedPackId = match
        }
    }

    if (confirmClearTokens) {
        AlertDialog(
            onDismissRequest = { confirmClearTokens = false },
            title = { Text("Clear API keys?") },
            text = { Text("Removes Hugging Face, Groq, and OpenRouter keys from this device. Cloud models will lock until you paste keys again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        appSettings.clearApiTokens()
                        TokenSidecar.clearFile()
                        hfInput = ""
                        groqInput = ""
                        openRouterInput = ""
                        keysSavedFlash = false
                        confirmClearTokens = false
                        GlassSnackbar.show("API keys cleared", SnackbarLevel.SUCCESS)
                    },
                    modifier = Modifier.testTag(com.zakir.vestra.ui.TestTags.SETTINGS_CLEAR_TOKENS_CONFIRM),
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmClearTokens = false },
                    modifier = Modifier.testTag(com.zakir.vestra.ui.TestTags.SETTINGS_CLEAR_TOKENS_CANCEL),
                ) { Text("Cancel") }
            },
        )
    }

    SpatialBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item(key = "top") {
                GlassTopBar(
                    title = sectionTitle,
                    subtitle = sectionSubtitle,
                    navigation = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            // Section order matches lookbookweb's settings.tsx (A4.9): appearance & accessibility
            // → device/engine lab → provider/cloud settings → diagnostics → "what the assistant
            // remembers" → about/changelog. Account and a sample-data toggle are omitted per that
            // same audit — no accounts exist in this app, and there's no demo-content banner to
            // gate a toggle for.
            settingsThemeSection(
                appSettings = appSettings,
                appearance = appearance,
            )
            settingsStoragePermissionsSection(
                clearingCache = clearingCache,
                onClearingCache = { clearingCache = it },
                usageLedger = usageLedger,
                permissionEpoch = permissionEpoch,
                onConfirmClearTokens = { confirmClearTokens = true },
            )

            item(key = "hub") {
                SettingsNavRow(
                    icon = Icons.Outlined.Memory,
                    title = "Models",
                    description = "Cloud services, API keys and on-device packs.",
                    onClick = onOpenModels,
                    testTag = TestTags.SETTINGS_ROW_MODELS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Tune,
                    title = "Default models",
                    description = "Which model Chat, Image, Video, Code and Audio each use.",
                    onClick = onOpenDefaultModels,
                    testTag = TestTags.SETTINGS_ROW_DEFAULT_MODELS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    description = "What the app is allowed to tell you, and when.",
                    onClick = onOpenNotifications,
                    testTag = TestTags.SETTINGS_ROW_NOTIFICATIONS,
                )
                Spacer(Modifier.height(10.dp))
                SettingsNavRow(
                    icon = Icons.Outlined.Analytics,
                    title = "API monitor",
                    description = "Requests, tokens, latency and estimated spend.",
                    onClick = onOpenApiMonitor,
                    testTag = TestTags.SETTINGS_ROW_API_MONITOR,
                )
                Spacer(Modifier.height(14.dp))
            }

            // Applies to every image generation regardless of local/cloud routing (see
            // GenerativeViewModel.generateImage).
            settingsSafetySection(appSettings = appSettings)

            settingsCloudKeysSection(
                appSettings = appSettings,
                connectivityChecker = connectivityChecker,
                hfTokenSaved = !hfToken.isNullOrBlank(),
                hfInput = hfInput,
                groqInput = groqInput,
                openRouterInput = openRouterInput,
                geminiInput = geminiInput,
                onHfInput = { hfInput = it },
                onGroqInput = { groqInput = it },
                onOpenRouterInput = { openRouterInput = it },
                onGeminiInput = { geminiInput = it },
                keysSavedFlash = keysSavedFlash,
                clipboardHint = clipboardHint,
                durableReady = durableReady,
                onApplyClipboard = { applyClipboardToken() },
                onOpenPortal = ::openPortal,
                onSaveTokens = ::saveTokens,
                importTokensLauncher = importTokensLauncher,
                onKeysLoadedFromDocuments = { count ->
                    hfInput = appSettings.hfToken.value.orEmpty()
                    groqInput = appSettings.groqApiKey.value.orEmpty()
                    openRouterInput = appSettings.openRouterApiKey.value.orEmpty()
                    geminiInput = appSettings.geminiApiKey.value.orEmpty()
                    keysSavedFlash = count > 0
                },
            )

            settingsDurableStatusSection(
                appSettings = appSettings,
                durableReady = durableReady,
            )

            settingsGeneralSection(
                onOpenHelp = onOpenHelp,
                onOpenPrivacy = onOpenPrivacy,
                onOpenChangelog = onOpenChangelog,
                onOpenDiagnostics = onOpenDiagnostics,
            )
            if (memoryRepository != null) {
                settingsMemorySection(appSettings = appSettings, memory = memoryRepository)
            }
        }
    }
}
