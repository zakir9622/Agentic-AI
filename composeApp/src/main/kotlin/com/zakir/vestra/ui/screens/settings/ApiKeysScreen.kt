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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.ProviderConnectivityChecker
import com.zakir.vestra.shared.platformHttpClient
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.TokenPortals
import com.zakir.vestra.storage.DurableStorage
import com.zakir.vestra.storage.TokenSidecar
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassSnackbar
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SnackbarLevel
import com.zakir.vestra.ui.components.SpatialBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one place a whole set of cloud credentials can be entered, imported or cleared.
 *
 * This is a *move*, not a rewrite: the card, its four key fields, the clipboard sniffing, the
 * `tokens.json` import and the durable-storage fallback all behaved this way when they sat inline
 * in Settings. What changed is that they no longer sit between "Permissions" and "About" on a
 * screen the user opened to change the theme.
 *
 * It coexists with the per-provider key field on [ProviderModelsScreen] deliberately. That one is
 * for fixing one provider while looking at its model list; this one is for the paste-everything
 * case, and it is the only surface that can take a whole file at once — losing it would trade one
 * paste for four.
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun ApiKeysScreen(
    appSettings: AppSettings,
    freeCloudDiscovery: FreeCloudDiscovery,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectivityChecker = remember { ProviderConnectivityChecker(platformHttpClient()) }

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
    var clipboardHint by remember { mutableStateOf<String?>(null) }
    var durableReady by remember { mutableStateOf(DurableStorage.hasAllFilesAccess()) }

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
                // Coming back from a provider's console with a freshly copied key is the common
                // path, so the page offers to fill it rather than making the user paste.
                applyClipboardToken()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (confirmClearTokens) {
        AlertDialog(
            onDismissRequest = { confirmClearTokens = false },
            title = { Text("Clear API keys?") },
            text = {
                Text(
                    "Removes Hugging Face, Groq, OpenRouter and Gemini keys from this device. " +
                        "Cloud models will lock until you paste keys again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appSettings.clearApiTokens()
                        TokenSidecar.clearFile()
                        hfInput = ""
                        groqInput = ""
                        openRouterInput = ""
                        geminiInput = ""
                        keysSavedFlash = false
                        confirmClearTokens = false
                        GlassSnackbar.show("API keys cleared", SnackbarLevel.SUCCESS)
                    },
                    modifier = Modifier.testTag(TestTags.SETTINGS_CLEAR_TOKENS_CONFIRM),
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmClearTokens = false },
                    modifier = Modifier.testTag(TestTags.SETTINGS_CLEAR_TOKENS_CANCEL),
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
                    title = "API keys",
                    subtitle = "Credentials · import · test",
                    navigation = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

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
        }
    }
}
