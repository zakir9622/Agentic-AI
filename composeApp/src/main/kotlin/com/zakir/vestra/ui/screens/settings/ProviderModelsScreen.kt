package com.zakir.vestra.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.VestraApp
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.shared.cloud.DirectoryModel
import com.zakir.vestra.shared.cloud.DirectoryResult
import com.zakir.vestra.shared.cloud.ProviderConnectivityChecker
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.storage.ApiKeyDataStore
import com.zakir.vestra.storage.TokenSidecar
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassPrimaryButton
import com.zakir.vestra.ui.components.GlassScreen
import com.zakir.vestra.ui.components.GlassSecondaryButton
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.theme.ControlTokens
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.launch

/**
 * One cloud service: its key, a live connectivity test, and the models the key actually unlocks.
 *
 * The list comes from the provider's own `/models` endpoint via
 * [com.zakir.vestra.shared.cloud.ProviderModelDirectory], not from the curated catalog, so it
 * shows what the key really buys. Models this app has a request format for are marked Ready and
 * are selectable; the rest render greyed with the reason. Filtering them out would make the list
 * look sparse and hide the truth; letting them be selected would defer the failure to generation
 * time, where it reads as a bug.
 */
@Composable
fun ProviderModelsScreen(
    platform: CloudPlatform,
    appSettings: AppSettings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as? VestraApp }
    val directory = app?.providerModelDirectory
    val scope = rememberCoroutineScope()

    val serviceName = platform.serviceName()
    val storedKey by appSettings.keyFlowFor(platform).collectAsState()
    var keyInput by remember(storedKey) { mutableStateOf(storedKey.orEmpty()) }
    var saved by remember { mutableStateOf(false) }

    var result by remember { mutableStateOf<DirectoryResult?>(directory?.cached(platform)) }
    var loading by remember { mutableStateOf(false) }

    fun refresh(force: Boolean) {
        val dir = directory ?: return
        loading = true
        scope.launch {
            result = dir.refresh(platform, appSettings.apiKeyForPlatform(platform), force = force)
            loading = false
        }
    }

    // Fetch on open when a key exists, so the common path is "open the page and the list is
    // there" rather than "open the page, then find and press Refresh".
    LaunchedEffect(platform, storedKey) {
        if (!storedKey.isNullOrBlank() && result == null) refresh(force = false)
    }

    GlassScreen(
        title = serviceName,
        subtitle = "API key · available models",
        onBack = onBack,
    ) {
        GlassCard {
            GlassSectionLabel("API KEY")
            Text(
                platform.keyHint(),
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
            Spacer(Modifier.height(SpacingTokens.xs))
            Box(Modifier.testTag(TestTags.PROVIDER_TOKEN_FIELD)) {
                KeyField(label = "$serviceName key", value = keyInput) {
                    keyInput = it
                    saved = false
                }
            }
            GlassPrimaryButton(
                text = if (saved) "Saved" else "Save key",
                onClick = {
                    val trimmed = keyInput.trim().ifBlank { null }
                    // Same three-store write the main Settings screen does, including the cloud
                    // consent grant — a key saved here is genuine interactive consent, and
                    // omitting that call leaves cloud models gated off despite a valid key.
                    appSettings.setKeyForPlatform(platform, trimmed)
                    if (trimmed != null) appSettings.confirmCloudConsentFromApiKeyEntry()
                    TokenSidecar.persist(context, appSettings)
                    scope.launch {
                        val store = app?.apiKeyDataStore ?: ApiKeyDataStore(context)
                        store.saveAll(
                            appSettings.hfToken.value,
                            appSettings.groqApiKey.value,
                            appSettings.openRouterApiKey.value,
                            appSettings.geminiApiKey.value,
                        )
                    }
                    // The cached listing was fetched with the old key; it is no longer valid.
                    directory?.invalidate(platform)
                    result = null
                    saved = true
                    if (trimmed != null) refresh(force = true)
                },
                modifier = Modifier.testTag(TestTags.PROVIDER_TOKEN_SAVE),
            )
            Spacer(Modifier.height(SpacingTokens.xs))
            ProviderConnectivityRow(platform = platform, appSettings = appSettings)
        }
        Spacer(Modifier.height(SpacingTokens.sm))

        GlassCard(modifier = Modifier.testTag(TestTags.PROVIDER_MODEL_LIST)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GlassSectionLabel("AVAILABLE MODELS")
                    Text(
                        modelsSubtitle(result, loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = VestraColors.InkMuted,
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(SpacingTokens.xs))
            GlassSecondaryButton(
                text = if (loading) "Fetching…" else "Refresh model list",
                onClick = { refresh(force = true) },
                enabled = !loading && !storedKey.isNullOrBlank(),
                modifier = Modifier.testTag(TestTags.PROVIDER_REFRESH_MODELS),
            )
            Spacer(Modifier.height(SpacingTokens.sm))

            when (val current = result) {
                null -> if (!loading) {
                    Text(
                        "Save a key above, then refresh to see every model it unlocks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VestraColors.InkMuted,
                    )
                }
                is DirectoryResult.NoKey -> Text(
                    "Add a key above to list this service's models.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                )
                is DirectoryResult.Unauthorized -> Text(
                    current.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.Danger,
                )
                is DirectoryResult.Failed -> Text(
                    "Couldn't list models: ${current.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.Danger,
                )
                is DirectoryResult.Loaded -> {
                    if (current.models.isEmpty()) {
                        Text(
                            "This key returned no usable models.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VestraColors.InkMuted,
                        )
                    }
                    // Ready models first — the ones the user can act on shouldn't be buried
                    // under a longer list of ones they can't.
                    current.models.sortedByDescending { it.runnable }.forEach { model ->
                        DirectoryModelRow(
                            model = model,
                            onSelect = { appSettings.selectDirectoryModel(model) },
                        )
                        Spacer(Modifier.height(SpacingTokens.xs))
                    }
                }
            }
        }
        Spacer(Modifier.height(SpacingTokens.xxl))
    }
}

private fun modelsSubtitle(result: DirectoryResult?, loading: Boolean): String = when {
    loading -> "Asking the provider…"
    result is DirectoryResult.Loaded -> {
        val ready = result.models.count { it.runnable }
        "${result.models.size} listed · $ready usable here"
    }
    else -> "Fetched live from the provider"
}

@Composable
private fun DirectoryModelRow(model: DirectoryModel, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.md)
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.providerModelRow(model.id))
            .clip(shape)
            .background(VestraColors.GlassFill)
            .then(
                if (model.runnable) {
                    Modifier
                        .border(1.dp, VestraColors.SaffronDeep.copy(alpha = 0.35f), shape)
                        .clickable(onClick = onSelect)
                } else {
                    Modifier
                },
            )
            .padding(SpacingTokens.sm),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(ControlTokens.dot)
                    .clip(CircleShape)
                    .background(if (model.runnable) VestraColors.SaffronDeep else VestraColors.InkMuted),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            Text(
                model.displayName,
                style = MaterialTheme.typography.titleSmall,
                // Greyed rather than hidden: an unusable model is still information about what
                // the key covers.
                color = if (model.runnable) VestraColors.Ink else VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            GlassPill(text = if (model.runnable) "Ready" else "Not usable", active = model.runnable)
        }
        Spacer(Modifier.height(SpacingTokens.xxs))
        Text(
            model.id,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (model.specLine.isNotBlank()) {
            Text(
                model.specLine,
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val note = if (model.runnable) model.description else model.unsupportedReason
        if (note.isNotBlank()) {
            Spacer(Modifier.height(SpacingTokens.xxs))
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProviderConnectivityRow(platform: CloudPlatform, appSettings: AppSettings) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as? VestraApp }
    val checker = remember(app) {
        ProviderConnectivityChecker(com.zakir.vestra.shared.platformHttpClient())
    }
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        GlassSecondaryButton(
            text = if (testing) "Testing…" else "Test key",
            enabled = !testing,
            onClick = {
                testing = true
                scope.launch {
                    val key = appSettings.apiKeyForPlatform(platform)
                    val result = when (platform) {
                        CloudPlatform.GROQ -> checker.checkGroq(key)
                        CloudPlatform.OPENROUTER -> checker.checkOpenRouter(key)
                        CloudPlatform.GEMINI -> checker.checkGemini(key)
                        CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> checker.checkHuggingFace(key)
                    }
                    ok = result is com.zakir.vestra.shared.cloud.ConnectivityResult.Connected
                    status = when (result) {
                        is com.zakir.vestra.shared.cloud.ConnectivityResult.Connected ->
                            "Connected · ${result.latencyMs}ms"
                        is com.zakir.vestra.shared.cloud.ConnectivityResult.Unauthorized -> result.detail
                        is com.zakir.vestra.shared.cloud.ConnectivityResult.RateLimited -> result.detail
                        is com.zakir.vestra.shared.cloud.ConnectivityResult.Unreachable -> result.detail
                        com.zakir.vestra.shared.cloud.ConnectivityResult.NoKey -> "No key saved yet"
                    }
                    testing = false
                }
            },
            modifier = Modifier.weight(1f),
        )
        status?.let { GlassPill(text = it, active = ok) }
    }
}

// ── Platform helpers ────────────────────────────────────────────────────────────────────

private fun CloudPlatform.serviceName(): String = when (this) {
    CloudPlatform.GEMINI -> "Google Gemini"
    CloudPlatform.GROQ -> "Groq"
    CloudPlatform.OPENROUTER -> "OpenRouter"
    CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> "Hugging Face"
}

private fun CloudPlatform.keyHint(): String = when (this) {
    CloudPlatform.GEMINI -> "From Google AI Studio. Starts with AIzaSy."
    CloudPlatform.GROQ -> "From console.groq.com. Starts with gsk_."
    CloudPlatform.OPENROUTER -> "From openrouter.ai/keys. Starts with sk-or-."
    CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE ->
        "A read token from huggingface.co/settings/tokens. Starts with hf_."
}

private fun AppSettings.keyFlowFor(platform: CloudPlatform) = when (platform) {
    CloudPlatform.GEMINI -> geminiApiKey
    CloudPlatform.GROQ -> groqApiKey
    CloudPlatform.OPENROUTER -> openRouterApiKey
    CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> hfToken
}

private fun AppSettings.apiKeyForPlatform(platform: CloudPlatform): String? = when (platform) {
    CloudPlatform.GEMINI -> geminiApiKey.value
    CloudPlatform.GROQ -> groqApiKey.value
    CloudPlatform.OPENROUTER -> openRouterApiKey.value
    CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> hfToken.value
}

private fun AppSettings.setKeyForPlatform(platform: CloudPlatform, key: String?) = when (platform) {
    CloudPlatform.GEMINI -> setGeminiApiKey(key)
    CloudPlatform.GROQ -> setGroqApiKey(key)
    CloudPlatform.OPENROUTER -> setOpenRouterApiKey(key)
    CloudPlatform.HF_INFERENCE, CloudPlatform.HF_SPACE -> setHfToken(key)
}

/**
 * Make [model] the default for whichever capability its catalog entry serves.
 *
 * A [DirectoryModel] only reaches here when `runnable` is true, which means it matched a curated
 * [CloudModelCatalog] provider — and that provider is what carries the capability. Without the
 * match there would be nothing to route the selection to.
 */
private fun AppSettings.selectDirectoryModel(model: DirectoryModel) {
    val provider = model.catalogProviderId?.let(CloudModelCatalog::byId) ?: return
    when (provider.capability) {
        AiCapability.IMAGE_GEN -> setImageGenProvider(provider.id)
        AiCapability.IMAGE_EDIT -> setImageEditProvider(provider.id)
        AiCapability.CODE -> setCodeProvider(provider.id)
        AiCapability.VIDEO -> setVideoProvider(provider.id)
        AiCapability.AUDIO -> setAudioProvider(provider.id)
        AiCapability.TRY_ON -> setCloudProvider(provider.id)
    }
}
