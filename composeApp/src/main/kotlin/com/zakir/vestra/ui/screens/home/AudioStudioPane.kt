package com.zakir.vestra.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.audio.VoiceCatalog
import com.zakir.vestra.shared.audio.VoiceKnobs
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.AtelierFilterChip
import com.zakir.vestra.ui.components.ExamplePromptRow
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.ResultPane
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Audio Studio — TTS with voice personas + local voice-changer knobs.
 * Cloud TTS by default; local TTS pack scaffolded; DSP knobs always on-device.
 */
@Composable
fun AudioStudioPane(
    viewModel: GenerativeViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
    freeCloudDiscovery: FreeCloudDiscovery? = null,
    packManager: ModelPackManager? = null,
) {
    LaunchedEffect(Unit) {
        if (!viewModel.isBusy) viewModel.prepareStudio(resetIfIdle = true)
    }

    val prompt by viewModel.prompt.collectAsState()
    val state by viewModel.state.collectAsState()
    val liveLog by viewModel.liveLog.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val personaId by viewModel.voicePersonaId.collectAsState()
    val knobs by viewModel.voiceKnobs.collectAsState()
    val audioId by viewModel.appSettings.audioProviderId.collectAsState()
    val packStates by packManager?.states?.collectAsState()
        ?: remember { mutableStateOf(emptyMap()) }

    val provider = viewModel.appSettings.selectedProvider(AiCapability.AUDIO)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing
    var showModelPicker by remember { mutableStateOf(false) }

    val pickerModels = remember(freeCloudDiscovery) {
        freeCloudDiscovery?.selectable(viewModel.appSettings, AiCapability.AUDIO)
            ?: CloudModelCatalog.forCapability(AiCapability.AUDIO)
    }
    val onDeviceEntries = remember(packStates) {
        LocalModelCatalog.forCapability(AiCapability.AUDIO).map { entry ->
            val packReady = entry.packId?.let { packStates[it]?.isReady() == true } == true
            val ready = entry.runnable && (entry.packId == null || packReady)
            OnDevicePickerEntry(
                id = entry.id,
                displayName = entry.displayName,
                detail = entry.testingNote,
                ready = ready,
                statusLabel = when {
                    ready -> "Ready offline"
                    entry.packId != null -> "Download in Settings"
                    else -> "Coming soon"
                },
            )
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        GlassSectionLabel(LookbookCopy.STUDIO_AUDIO.uppercase())
        Text(
            "Cloud TTS with named voices. Local voice-changer knobs always run on-device. " +
                "On-device TTS unlocks when local-tts-v1 weights ship.",
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.InkMuted,
        )
        if (preflight != null) {
            Spacer(Modifier.height(6.dp))
            GlassPill(text = preflight!!, active = true)
        }

        Spacer(Modifier.height(12.dp))
        Text("VOICE PERSONA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VoiceCatalog.personas) { persona ->
                AtelierFilterChip(
                    selected = personaId == persona.id,
                    onClick = { viewModel.setVoicePersona(persona.id) },
                    label = { Text(persona.displayName) },
                )
            }
        }
        Text(
            VoiceCatalog.byId(personaId).description,
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(14.dp))
        Text("VOICE CHANGER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "Local DSP · pitch · speed · formant · warmth · clarity",
            style = MaterialTheme.typography.labelSmall,
            color = VestraColors.InkMuted,
        )
        KnobSlider("Pitch (semitones)", knobs.pitchSemitones, -12f..12f, "%.0f") {
            viewModel.setVoiceKnobs(knobs.copy(pitchSemitones = it))
        }
        KnobSlider("Speed", knobs.speed, 0.5f..2f, "%.2f×") {
            viewModel.setVoiceKnobs(knobs.copy(speed = it))
        }
        KnobSlider("Formant", knobs.formant, 0.5f..1.5f, "%.2f") {
            viewModel.setVoiceKnobs(knobs.copy(formant = it))
        }
        KnobSlider("Warmth", knobs.warmth, 0f..1f, "%.2f") {
            viewModel.setVoiceKnobs(knobs.copy(warmth = it))
        }
        KnobSlider("Clarity", knobs.clarity, 0f..1f, "%.2f") {
            viewModel.setVoiceKnobs(knobs.copy(clarity = it))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AtelierFilterChip(
                selected = false,
                onClick = { viewModel.setVoiceKnobs(VoiceKnobs.Default) },
                label = { Text("Reset knobs") },
            )
        }

        Spacer(Modifier.height(12.dp))
        ExamplePromptRow(
            examples = listOf(
                "Welcome to The Lookbook atelier.",
                "This abaya drapes in soft black silk.",
                "Shop the new hijab collection today.",
            ),
            enabled = !busy,
            onPick = viewModel::setPrompt,
        )
        Spacer(Modifier.height(8.dp))
        PromptComposer(
            prompt = prompt,
            onPromptChange = viewModel::setPrompt,
            modelLabel = provider.displayName,
            onModelClick = { showModelPicker = true },
            busy = busy,
            enabled = prompt.isNotBlank(),
            onSend = viewModel::generateAudio,
            onStop = viewModel::cancel,
            placeholder = "Script for ${VoiceCatalog.byId(personaId).displayName}…",
        )
        Spacer(Modifier.height(8.dp))
        ResultPane(
            state = state,
            liveLog = liveLog,
            onRetry = viewModel::generateAudio,
            onDismiss = { viewModel.forceStop(showStopped = false) },
            retryLabel = "Speak again",
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showModelPicker) {
        ModelPickerSheet(
            title = "Audio models",
            models = pickerModels,
            selectedId = audioId.ifBlank { provider.id },
            onSelect = {
                viewModel.appSettings.setAudioProvider(it.id)
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false },
            onDeviceEntries = onDeviceEntries,
            health = viewModel.appSettings.modelHealth,
        )
    }
}

@Composable
private fun KnobSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = VestraColors.Ink)
            Text(format.format(value), style = MaterialTheme.typography.labelSmall, color = VestraColors.Accent)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
