package com.zakir.vestra.ui.screens.settings

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zakir.vestra.BuildConfig
import com.zakir.vestra.media.CacheCleanup
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelContracts
import com.zakir.vestra.shared.cloud.CloudModelProvider
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.ModelSupportLevel
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.PackStatus
import com.zakir.vestra.shared.engine.Availability
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.UnavailableReason
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppearanceMode
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.settings.TokenPortals
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.storage.DurableStorage
import com.zakir.vestra.storage.TokenSidecar
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassFormDefaults
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassTopBar
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.hasCameraPermission
import com.zakir.vestra.ui.util.hasPostNotificationsPermission
import com.zakir.vestra.ui.util.rememberPackDownloadStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SettingsSection {
    HUB,
    CLOUD,
    ENGINES,
    APPEARANCE,
    ALL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    packManager: ModelPackManager,
    freeCloudDiscovery: FreeCloudDiscovery,
    usageLedger: UsageLedger,
    onOpenPacks: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDiagnostics: (() -> Unit)? = null,
    onBack: () -> Unit,
    section: SettingsSection = SettingsSection.ALL,
    onNavigateSection: ((SettingsSection) -> Unit)? = null,
) {
    if (section == SettingsSection.HUB) {
        SettingsHubScreen(
            onBack = onBack,
            onOpenCloud = { onNavigateSection?.invoke(SettingsSection.CLOUD) },
            onOpenEngines = { onNavigateSection?.invoke(SettingsSection.ENGINES) },
            onOpenAppearance = { onNavigateSection?.invoke(SettingsSection.APPEARANCE) },
            onOpenUsage = onOpenUsage,
            onOpenHelp = onOpenHelp,
            onOpenPrivacy = onOpenPrivacy,
            onOpenDiagnostics = onOpenDiagnostics,
        )
        return
    }

    val showCloud = section == SettingsSection.ALL || section == SettingsSection.CLOUD
    val showEngines = section == SettingsSection.ALL || section == SettingsSection.ENGINES
    val showAppearance = section == SettingsSection.ALL || section == SettingsSection.APPEARANCE
    val showGeneral = section == SettingsSection.ALL
    val sectionTitle = when (section) {
        SettingsSection.CLOUD -> "Cloud models & keys"
        SettingsSection.ENGINES -> "Engines & packs"
        SettingsSection.APPEARANCE -> "Appearance & privacy"
        else -> LookbookCopy.STUDIO_SETTINGS
    }
    val sectionSubtitle = when (section) {
        SettingsSection.CLOUD -> "HF · Groq · OpenRouter"
        SettingsSection.ENGINES -> "Lite · Pro · Cloud tier"
        SettingsSection.APPEARANCE -> "Theme · storage · permissions"
        else -> "API keys · engines · models · help"
    }
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

    val hfToken by appSettings.hfToken.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()

    var hfInput by remember(hfToken) { mutableStateOf(hfToken.orEmpty()) }
    var groqInput by remember(groqKey) { mutableStateOf(groqKey.orEmpty()) }
    var openRouterInput by remember(openRouterKey) { mutableStateOf(openRouterKey.orEmpty()) }
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
            keysSavedFlash = count > 0
            Toast.makeText(
                context,
                if (count > 0) "Imported $count key(s) from file" else "No HF/Groq/OpenRouter keys found in file",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun applyClipboardToken() {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        val raw = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        val detected = TokenSidecar.detectClipboardToken(raw) ?: run {
            clipboardHint = null
            return
        }
        when (detected.first) {
            TokenPortals.Kind.HF -> hfInput = detected.second
            TokenPortals.Kind.GROQ -> groqInput = detected.second
            TokenPortals.Kind.OPENROUTER -> openRouterInput = detected.second
        }
        clipboardHint = "Detected ${detected.first.name} key from clipboard — tap Save API keys"
    }

    fun openPortal(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveTokens() {
        appSettings.setHfToken(hfInput.trim().ifBlank { null })
        appSettings.setGroqApiKey(groqInput.trim().ifBlank { null })
        appSettings.setOpenRouterApiKey(openRouterInput.trim().ifBlank { null })
        keysSavedFlash = true
        clipboardHint = null
        val saved = TokenSidecar.persist(context, appSettings)
        if (!saved && !DurableStorage.hasAllFilesAccess()) {
            Toast.makeText(
                context,
                "Tokens saved in-app. Enable durable storage below so they survive reinstall.",
                Toast.LENGTH_LONG,
            ).show()
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
    // Keep pack dropdown aligned when engine tier changes elsewhere.
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
                        Toast.makeText(context, "API keys cleared", Toast.LENGTH_SHORT).show()
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearTokens = false }) { Text("Cancel") }
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

            if (showGeneral) {
            item(key = "help") {
                GlassCard(onClick = onOpenHelp) {
                    GlassSectionLabel("HELP")
                    Text(LookbookCopy.STUDIO_HELP, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Searchable FAQ for packs, API keys, permissions, queues, and recovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "diagnostics") {
                GlassCard(onClick = { onOpenDiagnostics?.invoke() }) {
                    GlassSectionLabel("DIAGNOSTICS")
                    Text("Run history", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export JSON of local + cloud generations when reporting issues.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            }

            if (showGeneral) {
            item(key = "about") {
                GlassCard {
                    GlassSectionLabel("ABOUT")
                    Text(LookbookCopy.PRODUCT_NAME, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        LookbookCopy.PRODUCT_BLURB,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth()) {
                        Text(LookbookCopy.ACTION_OPEN_HELP)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenPrivacy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(LookbookCopy.ACTION_OPEN_PRIVACY)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            }

            if (showCloud) {
            // —— Keys first (this is where HF / Groq / OpenRouter go) ——
            item(key = "keys") {
                GlassCard {
                    GlassSectionLabel("API KEYS")
                    Text(
                        "Create a free classic HF Write/Read key with Inference Providers (not fine-grained discussion-only), copy it, then Save — or import tokens.json / tokens.txt. Clipboard keys are detected automatically. Local Lite/Pro packs never need a key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { openPortal(TokenSidecar.Portal.HF) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Hugging Face") }
                        OutlinedButton(
                            onClick = { openPortal(TokenSidecar.Portal.GROQ) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Groq") }
                        OutlinedButton(
                            onClick = { openPortal(TokenSidecar.Portal.OPENROUTER) },
                            modifier = Modifier.weight(1f),
                        ) { Text("OpenRouter") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            applyClipboardToken()
                            if (clipboardHint == null) {
                                Toast.makeText(
                                    context,
                                    "No Hugging Face / Groq / OpenRouter key found on clipboard",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Paste key from clipboard")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            importTokensLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "text/*",
                                    "*/*",
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Import tokens from JSON / TXT file")
                    }
                    if (durableReady) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val count = withContext(Dispatchers.IO) {
                                        TokenSidecar.autoFetchFromDocuments(
                                            appSettings,
                                            overwriteExisting = true,
                                        )
                                    }
                                    hfInput = appSettings.hfToken.value.orEmpty()
                                    groqInput = appSettings.groqApiKey.value.orEmpty()
                                    openRouterInput = appSettings.openRouterApiKey.value.orEmpty()
                                    keysSavedFlash = count > 0
                                    Toast.makeText(
                                        context,
                                        if (count > 0) {
                                            "Loaded $count key(s) from Documents/TheLookbook"
                                        } else {
                                            "No tokens.json / tokens.txt found in Documents/TheLookbook"
                                        },
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Auto-fetch from Documents/TheLookbook")
                        }
                    }
                    clipboardHint?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = VestraColors.Accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    KeyField("Hugging Face API key", hfInput) { hfInput = it }
                    KeyField("Groq API key", groqInput) { groqInput = it }
                    KeyField("OpenRouter API key (free models)", openRouterInput) { openRouterInput = it }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { saveTokens() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (keysSavedFlash) "Saved" else LookbookCopy.ACTION_SAVE_TOKENS)
                    }
                    if (!hfToken.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "HF token saved · for Code use curated Qwen2.5-Coder / Groq (not random auto-listed models).",
                            style = MaterialTheme.typography.labelSmall,
                            color = VestraColors.Accent,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            }

            if (showCloud || showAppearance) {
            item(key = "durable") {
                GlassCard {
                    GlassSectionLabel("SURVIVES REINSTALL")
                    Text(
                        if (durableReady) {
                            "Packs & tokens use Documents/TheLookbook. After uninstall/reinstall they are detected automatically — no re-download or re-paste needed."
                        } else {
                            "Allow all-files access once so model packs and tokens.json live in Documents/TheLookbook and survive uninstall."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Path: Documents/${DurableStorage.ROOT_FOLDER}/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!durableReady) {
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(DurableStorage.manageAllFilesIntent(context))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Enable durable storage")
                        }
                    } else {
                        Text(
                            "Durable storage on · packs root remounted",
                            style = MaterialTheme.typography.labelMedium,
                            color = VestraColors.Accent,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val ok = TokenSidecar.persist(context, appSettings)
                                Toast.makeText(
                                    context,
                                    if (ok) "Wrote ${DurableStorage.TOKENS_FILE}" else "Could not write sidecar",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Export tokens file now")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            }

            if (showAppearance) {
            item(key = "appearance") {
                GlassCard {
                    GlassSectionLabel("APPEARANCE")
                    Text(
                        "Pearl day / graphite night. System follows your phone setting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    AppearanceDropdown(
                        selected = appearance,
                        onSelect = appSettings::setAppearanceMode,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            }

            if (showEngines) {
            item(key = "engine") {
                GlassCard {
                    GlassSectionLabel("LOCAL TRY-ON ENGINE")
                    Text(
                        "On-device engines. Cloud is never chosen by Auto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    EngineDropdown(
                        selected = selectedTier,
                        availability = { tier ->
                            if (tier == EngineTier.AUTO) Availability.Ready else engineRouter.availability(tier)
                        },
                        onSelect = appSettings::setEngineTier,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "local-pack") {
                GlassCard {
                    GlassSectionLabel("LOCAL MODEL PACK")
                    Text(
                        "Select a pack, then download. Transfers resume if interrupted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    PackDropdown(
                        choices = localPackChoices.mapNotNull { entry ->
                            entry.packId?.let { id ->
                                id to "${entry.displayName} · ${entry.approxSizeLabel}"
                            }
                        },
                        selectedId = selectedPackId,
                        onSelect = { id ->
                            selectedPackId = id
                            LocalModelCatalog.entries
                                .firstOrNull { it.packId == id }
                                ?.engineTier
                                ?.let { appSettings.setEngineTier(it) }
                        },
                    )
                    val status = packStates[selectedPackId]?.status
                    val progress = packStates[selectedPackId]?.progress ?: 0f
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (status) {
                            PackStatus.INSTALLED -> packStates[selectedPackId]?.verifyLabel()
                                ?: "Installed — verification pending"
                            PackStatus.DOWNLOADING -> "Downloading ${(progress * 100).toInt()}%…"
                            PackStatus.INCOMPATIBLE -> "This device doesn’t meet pack requirements"
                            PackStatus.UPDATE_AVAILABLE -> "Update available"
                            PackStatus.NOT_INSTALLED -> if (progress > 0f) "Partial download — can resume" else "Not installed"
                            null -> when {
                                packStates.isEmpty() && !packCatalogError.isNullOrBlank() ->
                                    "Catalog unavailable — open All packs to retry"
                                packStates.isEmpty() -> "Loading pack catalog…"
                                else -> "Not in catalog yet — open All packs or tap Download"
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (selectedPackId.isNotBlank()) startDownload(selectedPackId) },
                            enabled = status != PackStatus.INCOMPATIBLE && selectedPackId.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                when (status) {
                                    PackStatus.INSTALLED -> "Re-download"
                                    PackStatus.DOWNLOADING -> "Downloading…"
                                    else -> if (progress > 0f) "Resume" else "Download"
                                },
                            )
                        }
                        OutlinedButton(onClick = onOpenPacks, modifier = Modifier.weight(1f)) {
                            Text("All packs")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "usage") {
                GlassCard(onClick = onOpenUsage) {
                    GlassSectionLabel("USAGE")
                    Text(LookbookCopy.STUDIO_USAGE, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Local ledger of free-tier cloud requests, tokens, and failure notes. Local packs are \$0.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }

            if (showCloud) {
            item(key = "cap-tryon") {
                CloudCapabilityDropdown(
                    title = "CLOUD TRY-ON",
                    capability = AiCapability.TRY_ON,
                    selectedId = tryOnId,
                    appSettings = appSettings,
                    discovery = freeCloudDiscovery,
                    onSelect = appSettings::setCloudProvider,
                )
            }
            item(key = "cap-gen") {
                CloudCapabilityDropdown(
                    title = "IMAGE GENERATION",
                    capability = AiCapability.IMAGE_GEN,
                    selectedId = imageGenId,
                    appSettings = appSettings,
                    discovery = freeCloudDiscovery,
                    onSelect = appSettings::setImageGenProvider,
                )
            }
            item(key = "cap-edit") {
                CloudCapabilityDropdown(
                    title = "IMAGE EDIT / RECREATE",
                    capability = AiCapability.IMAGE_EDIT,
                    selectedId = imageEditId,
                    appSettings = appSettings,
                    discovery = freeCloudDiscovery,
                    onSelect = appSettings::setImageEditProvider,
                )
            }
            item(key = "cap-code") {
                CloudCapabilityDropdown(
                    title = "CODING MODELS",
                    capability = AiCapability.CODE,
                    selectedId = codeId,
                    appSettings = appSettings,
                    discovery = freeCloudDiscovery,
                    onSelect = appSettings::setCodeProvider,
                )
            }
            item(key = "cap-video") {
                CloudCapabilityDropdown(
                    title = "VIDEO MODELS",
                    capability = AiCapability.VIDEO,
                    selectedId = videoId,
                    appSettings = appSettings,
                    discovery = freeCloudDiscovery,
                    onSelect = appSettings::setVideoProvider,
                )
            }
            }

            if (showAppearance) {
            item(key = "storage") {
                Spacer(Modifier.height(14.dp))
                GlassCard {
                    GlassSectionLabel("STORAGE & PRIVACY")
                    Text(
                        "Clears regenerable caches only. Installed model packs and wardrobe index stay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (clearingCache) return@OutlinedButton
                            clearingCache = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    CacheCleanup.clearAppCaches(context)
                                }
                                clearingCache = false
                                Toast.makeText(
                                    context,
                                    "Cleared ${result.deletedFiles} files · ${CacheCleanup.formatBytes(result.freedBytes)}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        enabled = !clearingCache,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (clearingCache) "Clearing…" else "Clear cache")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            usageLedger.clear()
                            Toast.makeText(context, "Usage ledger cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Clear usage ledger")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { confirmClearTokens = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Clear API keys")
                    }
                    Spacer(Modifier.height(8.dp))
                    val reportStore = remember { com.zakir.vestra.data.LocalReportStore(context) }
                    Text(
                        "Content reports on device: ${reportStore.count()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (reportStore.count() == 0) {
                                Toast.makeText(context, "No reports yet", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "The Lookbook content reports")
                                putExtra(Intent.EXTRA_TEXT, reportStore.exportJson())
                            }
                            context.startActivity(Intent.createChooser(send, LookbookCopy.ACTION_EXPORT_REPORTS))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(LookbookCopy.ACTION_EXPORT_REPORTS)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "permissions-$permissionEpoch") {
                GlassCard {
                    GlassSectionLabel("PERMISSIONS")
                    Text(
                        LookbookCopy.PERM_NOTIFICATIONS_TITLE + ": " +
                            if (context.hasPostNotificationsPermission()) "Allowed" else "Not allowed",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        LookbookCopy.PERM_CAMERA_TITLE + ": " +
                            if (context.hasCameraPermission()) "Allowed" else "Not allowed",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        LookbookCopy.PERM_STORAGE_TITLE + ": " +
                            if (DurableStorage.hasAllFilesAccess()) "Allowed" else "Not allowed",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Permissions are requested in context — when you use the camera, download packs, or enable durable storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceDropdown(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Theme") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = GlassFormDefaults.outlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = GlassFormDefaults.menuContainerColor(),
            shadowElevation = GlassFormDefaults.MenuShadow,
        ) {
            AppearanceMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label(), color = VestraColors.Ink) },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                    colors = GlassFormDefaults.menuItemColors(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDropdown(
    selected: EngineTier,
    availability: (EngineTier) -> Availability,
    onSelect: (EngineTier) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { EngineTier.entries }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Engine") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = {
                Text(selected.description(availability(selected)), color = VestraColors.InkMuted)
            },
            colors = GlassFormDefaults.outlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = GlassFormDefaults.menuContainerColor(),
            shadowElevation = GlassFormDefaults.MenuShadow,
        ) {
            options.forEach { tier ->
                val avail = availability(tier)
                val enabled = true // Always selectable; pack download is the recovery path for Lite/Pro.
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(tier.label(), color = VestraColors.Ink)
                            Text(
                                tier.description(avail),
                                style = MaterialTheme.typography.labelSmall,
                                color = VestraColors.InkMuted,
                            )
                        }
                    },
                    onClick = {
                        if (enabled) {
                            onSelect(tier)
                            expanded = false
                        }
                    },
                    enabled = enabled,
                    colors = GlassFormDefaults.menuItemColors(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackDropdown(
    choices: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = choices.firstOrNull { it.first == selectedId }?.second ?: "Select a pack"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Model pack") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = GlassFormDefaults.outlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = GlassFormDefaults.menuContainerColor(),
            shadowElevation = GlassFormDefaults.MenuShadow,
        ) {
            choices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = VestraColors.Ink) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                    colors = GlassFormDefaults.menuItemColors(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudCapabilityDropdown(
    title: String,
    capability: AiCapability,
    selectedId: String,
    appSettings: AppSettings,
    discovery: FreeCloudDiscovery,
    onSelect: (String) -> Unit,
) {
    val hfToken by appSettings.hfToken.collectAsState()
    val groqKey by appSettings.groqApiKey.collectAsState()
    val openRouterKey by appSettings.openRouterApiKey.collectAsState()
    val discovered by appSettings.discoveredProviders.collectAsState()
    val tokenEpoch = "${hfToken.orEmpty()}|${groqKey.orEmpty()}|${openRouterKey.orEmpty()}|${discovered.size}"

    val usable = remember(tokenEpoch, capability) { discovery.selectable(appSettings, capability) }
    val locked = remember(tokenEpoch, capability) { discovery.curatedLocked(appSettings, capability) }
    val unsupported = remember(capability) { discovery.curatedUnsupported(capability) }
    val options = usable

    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
        ?: locked.firstOrNull { it.id == selectedId }
        ?: options.firstOrNull()
    val selectedNeedsToken = selected != null && locked.any { it.id == selected.id }
    val lockedOthers = locked.filter { it.id != selected?.id }
    val selectedContractNote = selected?.let { CloudModelContracts.settingsSupportingText(it) }
    val selectedSupport = selected?.let { CloudModelContracts.forProvider(it).support }

    // Edge case: prefs still point at a locked model while usable options exist (e.g. HF
    // discovery unlocked models but Groq selection lacks a Groq key). Auto-switch.
    LaunchedEffect(tokenEpoch, selectedId, options.map { it.id }) {
        if (options.isEmpty()) return@LaunchedEffect
        val selectedUsable = options.any { it.id == selectedId }
        if (!selectedUsable) {
            onSelect(options.first().id)
        }
    }

    Spacer(Modifier.height(14.dp))
    GlassCard {
        GlassSectionLabel(title)
        Text(
            when {
                options.isNotEmpty() -> "${options.size} free models ready with your tokens"
                locked.isNotEmpty() -> "Add the required token above to unlock ${locked.size} models"
                else -> "No free models for this capability yet"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.displayName ?: "Select model",
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                supportingText = {
                    Text(
                        when {
                            selectedNeedsToken ->
                                "Save a ${selected?.platform.toTokenLabel()} key above to use this model"
                            selectedSupport == ModelSupportLevel.DEGRADED ->
                                selectedContractNote ?: (selected?.usageNote ?: "")
                            else -> selectedContractNote
                                ?: selected?.usageNote?.ifBlank { selected.description }
                                ?: ""
                        },
                        color = when {
                            selectedNeedsToken -> MaterialTheme.colorScheme.error
                            selectedSupport == ModelSupportLevel.DEGRADED -> MaterialTheme.colorScheme.tertiary
                            else -> VestraColors.InkMuted
                        },
                    )
                },
                colors = GlassFormDefaults.outlinedFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                enabled = options.isNotEmpty(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = GlassFormDefaults.menuContainerColor(),
                shadowElevation = GlassFormDefaults.MenuShadow,
            ) {
                options.forEach { provider ->
                    val support = CloudModelContracts.forProvider(provider).support
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    "${provider.displayName} · ${CloudModelContracts.liveStatusLabel(provider, appSettings.modelHealth)}",
                                    color = VestraColors.Ink,
                                )
                                Text(
                                    "${provider.platform.name} · ${CloudModelContracts.forProvider(provider).schemaNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (support == ModelSupportLevel.DEGRADED) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        VestraColors.InkMuted
                                    },
                                )
                            }
                        },
                        onClick = {
                            onSelect(provider.id)
                            expanded = false
                        },
                        colors = GlassFormDefaults.menuItemColors(),
                    )
                }
            }
        }
        if (lockedOthers.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Also needs a token: " + lockedOthers.take(3).joinToString { it.displayName },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (unsupported.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            // Nothing is wrong with the user's setup here — these are models the app cannot
            // drive yet — so this reads as a footnote rather than an error.
            Text(
                "Not selectable yet: " + unsupported.joinToString { it.displayName },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        // Warm HF Inference auto-listing is disabled: image/video need Gradio Spaces, and
        // CODE uses curated Groq/HF/OpenRouter chat routes only.
        Text(
            "Uses curated free HF Spaces / APIs — live Inference discovery isn’t needed here.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        colors = GlassFormDefaults.outlinedFieldColors(),
    )
}

private fun CloudPlatform?.toTokenLabel(): String = when (this) {
    CloudPlatform.GROQ -> "Groq"
    CloudPlatform.OPENROUTER -> "OpenRouter"
    CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> "Hugging Face"
    null -> "API"
}

private fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> "System"
    AppearanceMode.LIGHT -> "Light"
    AppearanceMode.DARK -> "Dark"
}

private fun EngineTier.label(): String = when (this) {
    EngineTier.AUTO -> "Auto (on-device)"
    EngineTier.LITE -> "Lite — on device"
    EngineTier.PRO -> "Pro — on device"
    EngineTier.CLOUD -> "Cloud — free HF Spaces"
}

private fun EngineTier.description(availability: Availability): String {
    val base = when (this) {
        EngineTier.AUTO -> "Best on-device engine. Never uses cloud automatically."
        EngineTier.LITE -> "Fast compositor. Works offline on every phone."
        EngineTier.PRO -> "SD1.5 diffusion on-device. Needs Pro pack."
        EngineTier.CLOUD -> "Free Hugging Face Spaces only. Select model below."
    }
    return when (availability) {
        Availability.Ready -> base
        is Availability.Unavailable -> when (availability.reason) {
            UnavailableReason.PACK_NOT_INSTALLED -> "$base Model pack not installed."
            UnavailableReason.PACK_VERIFY_FAILED ->
                "$base Model pack failed verification — re-download in Model packs."
            UnavailableReason.PACK_VERIFY_PENDING ->
                "$base Model pack verifying — wait a moment."
            UnavailableReason.COMPANION_PACK_MISSING ->
                "$base Pro also needs the Lite pack — install Lite to enable it."
            UnavailableReason.DEVICE_NOT_CAPABLE -> "$base Device doesn’t meet RAM requirements."
            UnavailableReason.OFFLINE -> "$base No internet connection."
            UnavailableReason.NOT_CONFIGURED -> "$base Add the required free API key above."
        }
    }
}
