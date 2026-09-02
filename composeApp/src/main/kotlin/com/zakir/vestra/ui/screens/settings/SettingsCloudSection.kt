package com.zakir.vestra.ui.screens.settings

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.ConnectivityResult
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.ProviderConnectivityChecker
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.storage.TokenSidecar
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassSnackbar
import com.zakir.vestra.ui.components.SnackbarLevel
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** API keys card + per-capability cloud model dropdowns. */
internal fun LazyListScope.settingsCloudKeysSection(
    appSettings: AppSettings,
    connectivityChecker: ProviderConnectivityChecker,
    hfTokenSaved: Boolean,
    hfInput: String,
    groqInput: String,
    openRouterInput: String,
    geminiInput: String = "",
    onHfInput: (String) -> Unit,
    onGroqInput: (String) -> Unit,
    onOpenRouterInput: (String) -> Unit,
    onGeminiInput: (String) -> Unit = {},
    keysSavedFlash: Boolean,
    clipboardHint: String?,
    durableReady: Boolean,
    onApplyClipboard: () -> Boolean,
    onOpenPortal: (String) -> Unit,
    onSaveTokens: () -> Unit,
    importTokensLauncher: ManagedActivityResultLauncher<Array<String>, android.net.Uri?>,
    onKeysLoadedFromDocuments: (count: Int) -> Unit,
) {
    item(key = "keys") {
        val scope = rememberCoroutineScope()
        GlassCard {
            GlassSectionLabel("API KEYS & CREDENTIALS")
            Text(
                "API keys are stored securely on-device using Jetpack DataStore. Enter your free API keys for Hugging Face, Groq, OpenRouter, and Google Gemini — or import tokens.json. Local Lite/Pro packs never need a key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.HF) },
                    modifier = Modifier.weight(1f),
                ) { Text("Hugging Face", maxLines = 1) }
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.GROQ) },
                    modifier = Modifier.weight(1f),
                ) { Text("Groq", maxLines = 1) }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.OPENROUTER) },
                    modifier = Modifier.weight(1f),
                ) { Text("OpenRouter", maxLines = 1) }
                OutlinedButton(
                    onClick = { onOpenPortal(TokenSidecar.Portal.GEMINI) },
                    modifier = Modifier.weight(1f),
                ) { Text("Gemini", maxLines = 1) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (!onApplyClipboard()) {
                        GlassSnackbar.show(
                            "No Hugging Face / Groq / OpenRouter / Gemini key found on clipboard",
                            SnackbarLevel.WARNING,
                        )
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
                            onKeysLoadedFromDocuments(count)
                            GlassSnackbar.show(
                                if (count > 0) {
                                    "Loaded $count key(s) from Documents/TheLookbook"
                                } else {
                                    "No tokens.json / tokens.txt found in Documents/TheLookbook"
                                },
                                if (count > 0) SnackbarLevel.SUCCESS else SnackbarLevel.WARNING,
                            )
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
            KeyField("Hugging Face API key", hfInput, onHfInput)
            ConnectivityTestRow(
                label = "Test Hugging Face key",
                testTag = TestTags.connectivityTestButton("huggingface"),
                onTest = { connectivityChecker.checkHuggingFace(hfInput.ifBlank { null }) },
            )
            Spacer(Modifier.height(4.dp))
            KeyField("Groq API key", groqInput, onGroqInput)
            ConnectivityTestRow(
                label = "Test Groq key",
                testTag = TestTags.connectivityTestButton("groq"),
                onTest = { connectivityChecker.checkGroq(groqInput.ifBlank { null }) },
            )
            Spacer(Modifier.height(4.dp))
            KeyField("OpenRouter API key (free models)", openRouterInput, onOpenRouterInput)
            ConnectivityTestRow(
                label = "Test OpenRouter key",
                testTag = TestTags.connectivityTestButton("openrouter"),
                onTest = { connectivityChecker.checkOpenRouter(openRouterInput.ifBlank { null }) },
            )
            Spacer(Modifier.height(4.dp))
            KeyField("Google Gemini API key", geminiInput, onGeminiInput)
            ConnectivityTestRow(
                label = "Test Gemini key",
                testTag = TestTags.connectivityTestButton("gemini"),
                onTest = { connectivityChecker.checkGemini(geminiInput.ifBlank { null }) },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSaveTokens,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (keysSavedFlash) "Saved" else LookbookCopy.ACTION_SAVE_TOKENS)
            }
            if (hfTokenSaved) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "HF token saved · for Code use curated Qwen2.5-Coder / Groq (not random auto-listed models).",
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.Accent,
                )
            }
            val cloudConsentGranted by appSettings.cloudConsentGranted.collectAsState()
            if (cloudConsentGranted) {
                TextButton(
                    onClick = {
                        appSettings.revokeCloudConsent()
                        GlassSnackbar.show(
                            "Switched to on-device only · pick a cloud model or save a key to use cloud again",
                            SnackbarLevel.INFO,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Stop using cloud models (switch to on-device only)")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
/**
 * A real "test this key now" action — a genuine, minimal, read-only network call via
 * [ProviderConnectivityChecker] (a models-list / who-am-i request against the same host this
 * app's actual generation code talks to), not a simulated delay-then-random result. Every label
 * below reflects an actual HTTP outcome: a real status code, a real measured latency, or a real
 * thrown exception — see `docs/DRAWBACKS.md` for why this replaces the fake ping pattern found in
 * the GoogleLookBookUI source this screen's sibling components were ported from.
 */
@androidx.compose.runtime.Composable
private fun ConnectivityTestRow(
    label: String,
    testTag: String,
    onTest: suspend () -> ConnectivityResult,
) {
    var testing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<ConnectivityResult?>(null) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {
                if (!testing) {
                    testing = true
                    scope.launch {
                        lastResult = onTest()
                        testing = false
                    }
                }
            },
            enabled = !testing,
            modifier = Modifier.testTag(testTag),
        ) {
            Text(if (testing) "Testing…" else label)
        }
        lastResult?.let { result ->
            val (text, active) = when (result) {
                is ConnectivityResult.Connected -> "Connected · ${result.latencyMs}ms" to true
                is ConnectivityResult.Unauthorized -> result.detail to false
                is ConnectivityResult.RateLimited -> result.detail to false
                is ConnectivityResult.Unreachable -> result.detail to false
                ConnectivityResult.NoKey -> "No key entered above" to false
            }
            com.zakir.vestra.ui.components.GlassPill(text = text, active = active)
        }
    }
}
