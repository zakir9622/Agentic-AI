package com.zakir.vestra.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.zakir.vestra.ui.TestTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zakir.vestra.shared.audio.AndroidLatencyCalibrator
import com.zakir.vestra.shared.audio.AndroidMicRecorder
import com.zakir.vestra.shared.audio.CalibrationResult
import com.zakir.vestra.shared.audio.PitchDetector
import com.zakir.vestra.shared.audio.WavIo
import com.zakir.vestra.audio.AudioClip
import com.zakir.vestra.audio.AudioClipLibrary
import com.zakir.vestra.audio.AudioImportHelper
import com.zakir.vestra.ui.components.AudioClipList
import com.zakir.vestra.ui.components.AudioLevelMeter
import com.zakir.vestra.media.MediaExport
import com.zakir.vestra.shared.audio.VoiceCatalog
import com.zakir.vestra.shared.audio.VoiceKnobs
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.cloud.CloudModelCatalog
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.local.LocalModelCatalog
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.components.AtelierFilterChip
import com.zakir.vestra.ui.components.DockedLiveLog
import com.zakir.vestra.ui.components.GlassPill
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.GlassTile
import com.zakir.vestra.ui.components.ModelPickerSheet
import com.zakir.vestra.ui.components.OnDevicePickerEntry
import com.zakir.vestra.ui.components.PromptComposer
import com.zakir.vestra.ui.components.ResultPane
import com.zakir.vestra.ui.components.StudioTurnBubble
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import java.io.File

/**
 * Audio Studio — TTS with voice personas + local voice-changer knobs + mic record.
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
        viewModel.bindStudio(AiCapability.AUDIO)
    }

    val warmup by viewModel.warmup.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val state by viewModel.state.collectAsState()
    val turns by viewModel.turns.collectAsState()
    val liveLog by viewModel.liveLog.collectAsState()
    val generationStartedAtMs by viewModel.generationStartedAtMs.collectAsState()
    val preflight by viewModel.preflightMessage.collectAsState()
    val personaId by viewModel.voicePersonaId.collectAsState()
    val knobs by viewModel.voiceKnobs.collectAsState()
    val reference by viewModel.referenceUri.collectAsState()
    val audioId by viewModel.appSettings.audioProviderId.collectAsState()
    val cloudModelsEnabled by viewModel.appSettings.cloudModelsEnabled.collectAsState()
    val packStates by packManager?.states?.collectAsState()
        ?: remember { mutableStateOf(emptyMap()) }

    // Picking a model should load it, not defer the cost to the first prompt — matches
    // UnifiedStudioPane's Image/Video/Code warm-up; Audio never had this wired at all. Keyed on
    // audioId (the actual model/provider selection), not personaId (just the voice) — keying on
    // personaId would skip re-warming on a real model switch and re-warm needlessly on a voice
    // change that doesn't touch which engine is loaded.
    LaunchedEffect(audioId) {
        viewModel.warmUpLocal(AiCapability.AUDIO)
    }

    val provider = viewModel.appSettings.selectedProvider(AiCapability.AUDIO)
    val busy = state is GenerativeState.Running || state is GenerativeState.Preparing
    var showModelPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val micRecorder = remember {
        AndroidMicRecorder(File(context.cacheDir, "audio_recordings").also { it.mkdirs() })
    }
    var isRecording by remember { mutableStateOf(false) }
    var recordHint by remember { mutableStateOf<String?>(null) }
    val micAmplitude by micRecorder.amplitude.collectAsState()
    val scope = rememberCoroutineScope()
    var matchHint by remember { mutableStateOf<String?>(null) }
    var calibrating by remember { mutableStateOf(false) }
    var calibrationHint by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            if (micRecorder.isRecording) micRecorder.stop()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (micRecorder.start()) {
                isRecording = true
                recordHint = "Recording… tap Stop when done (max 15s)"
            } else {
                recordHint = micRecorder.lastFailure ?: "Could not start recording"
            }
        } else {
            recordHint = "Microphone permission is required to record voice."
        }
    }

    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = AudioImportHelper.copyUriToCache(context, uri)
                if (path != null) {
                    viewModel.setReference(path)
                    recordHint = "Imported clip ready — adjust knobs, then Apply voice change"
                } else {
                    recordHint = "Could not import that audio file"
                }
            }
        }
    }

    fun toggleMic() {
        if (busy) return
        if (isRecording) {
            val path = micRecorder.stop()
            isRecording = false
            if (path != null) {
                viewModel.setReference(path)
                recordHint = "Clip saved — adjust knobs, then Apply voice change"
            } else {
                recordHint = micRecorder.lastFailure ?: "Recording failed"
            }
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                if (micRecorder.start()) {
                    isRecording = true
                    recordHint = "Recording… tap Stop when done (max 15s)"
                } else {
                    recordHint = micRecorder.lastFailure ?: "Could not start recording"
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun matchVoiceToPersona() {
        val path = reference
        if (path == null) {
            matchHint = "Record a clip first, then Match voice."
            return
        }
        scope.launch {
            matchHint = "Detecting pitch…"
            val result = withContext(Dispatchers.Default) {
                val wav = WavIo.readPcm16MonoWav(File(path))
                    ?: return@withContext null
                val sourceHz = PitchDetector.detectPitchHz(wav.samples, wav.sampleRate)
                    ?: return@withContext null
                val persona = VoiceCatalog.byId(personaId)
                val targetHz = VoiceCatalog.typicalHzFor(persona.variety)
                val semitones = PitchDetector.semitoneDifferenceRounded(sourceHz, targetHz).coerceIn(-12, 12)
                Triple(sourceHz, targetHz, semitones)
            }
            if (result == null) {
                matchHint = "Couldn't detect a clear pitch in the recorded clip — try a longer, clearer take."
            } else {
                val (sourceHz, targetHz, semitones) = result
                viewModel.setVoiceKnobs(knobs.copy(pitchSemitones = semitones.toFloat()))
                matchHint = "Matched ${sourceHz.toInt()}Hz → ~${targetHz.toInt()}Hz for ${VoiceCatalog.byId(personaId).displayName} · pitch set to ${if (semitones >= 0) "+" else ""}$semitones"
            }
        }
    }

    fun calibrateLatency() {
        if (calibrating) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            calibrationHint = "Microphone permission is required to calibrate."
            return
        }
        calibrating = true
        calibrationHint = "Playing a short tone and listening for it…"
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                AndroidLatencyCalibrator().calibrate()
            }
            calibrating = false
            calibrationHint = when (result) {
                is CalibrationResult.Measured ->
                    "Estimated round-trip latency: ${result.latencyMs.toInt()}ms · informational only, not yet applied to live monitoring"
                is CalibrationResult.Unavailable -> "Calibration unavailable: ${result.reason}"
            }
        }
    }

    val pickerModels = remember(freeCloudDiscovery) {
        freeCloudDiscovery?.selectable(viewModel.appSettings, AiCapability.AUDIO)
            ?: CloudModelCatalog.forCapability(AiCapability.AUDIO)
    }
    // Produced-clip list. Rescanned whenever generation state changes so a new recording,
    // conversion or TTS result appears without the user leaving the tab. The scan reads file
    // metadata, so it stays off the UI thread.
    var clips by remember { mutableStateOf<List<AudioClip>>(emptyList()) }
    LaunchedEffect(state) {
        clips = withContext(Dispatchers.IO) {
            AudioClipLibrary.scan(
                listOf(
                    File(context.filesDir, "generations"),
                    File(context.cacheDir, "audio_recordings"),
                ),
            )
        }
    }

    var localAudioReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        localAudioReady = withContext(Dispatchers.Default) {
            viewModel.localAudioOfflineReady()
        }
    }
    val onDeviceEntries = remember(packStates, localAudioReady) {
        LocalModelCatalog.forStudioPicker(AiCapability.AUDIO).map { entry ->
            val packReady = entry.packId?.let { packStates[it]?.isReady() == true } == true
            OnDevicePickerEntry(
                id = entry.id,
                displayName = entry.displayName,
                detail = entry.testingNote,
                ready = LocalModelCatalog.studioEntryReady(entry, packReady),
                statusLabel = LocalModelCatalog.studioStatusLabel(entry, packReady),
            )
        }
    }

    val accent = VestraColors.ModalityAudio
    val listState = rememberLazyListState()
    // Auto-scroll to the newest turn as it's appended or its result comes in — the "controls"
    // header item (index 0) means the last turn always sits at `turns.lastIndex + 1`.
    LaunchedEffect(turns) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex + 1)
    }

    Column(modifier.fillMaxSize()) {
        // Persona/knobs/mic controls are studio configuration, not conversation content, so they
        // stay as a fixed header above the turn timeline rather than becoming timeline turns
        // themselves — only prompt→result exchanges do that (see 3.1.6 CHANGELOG).
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.section, vertical = 8.dp),
        ) {
            item(key = "audio-controls") {
                Column {
                    // The section label + static "Device TTS works offline..." info line used to
                    // sit here — decorative model-status copy, not actionable feedback (see
                    // 3.1.6 CHANGELOG). The preflight/recordHint pills below are real validation/
                    // recording feedback, not status clutter, so they stay.
                    if (preflight != null) {
                        Spacer(Modifier.height(6.dp))
                        GlassPill(text = preflight!!, active = true)
                    }
                    if (recordHint != null) {
                        Spacer(Modifier.height(6.dp))
                        GlassPill(text = recordHint!!, active = isRecording || reference != null)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("VOICE PERSONA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    VoiceCatalog.groupedByVariety().forEach { (section, personas) ->
                        GlassTile(Modifier.padding(bottom = 8.dp)) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    section.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(personas) { persona ->
                                        AtelierFilterChip(
                                            selected = personaId == persona.id,
                                            onClick = { viewModel.setVoicePersona(persona.id) },
                                            label = { Text(persona.displayName) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        VoiceCatalog.byId(personaId).description,
                        style = MaterialTheme.typography.labelSmall,
                        color = VestraColors.InkMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("LIVE VOICE CHANGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Record → apply knobs → play. Continuous streaming DSP is not in this build.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VestraColors.InkMuted,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (isRecording) {
                        AudioLevelMeter(amplitude = micAmplitude, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtelierFilterChip(
                            selected = isRecording,
                            onClick = { toggleMic() },
                            label = { Text(if (isRecording) "Stop mic" else "Record mic") },
                        )
                        AtelierFilterChip(
                            selected = false,
                            onClick = { if (!busy) audioFilePickerLauncher.launch("audio/*") },
                            label = { Text("Import audio") },
                            modifier = Modifier.testTag(TestTags.AUDIO_IMPORT_BUTTON),
                        )
                        AtelierFilterChip(
                            selected = false,
                            onClick = {
                                if (!busy && reference != null) viewModel.applyVoiceChange()
                            },
                            label = { Text("Apply voice change") },
                        )
                        AtelierFilterChip(
                            selected = false,
                            onClick = {
                                if (!busy && reference != null) viewModel.transcribeAudio()
                            },
                            label = { Text("Transcribe (offline)") },
                        )
                        if (reference != null) {
                            AtelierFilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.setReference(null)
                                    micRecorder.clear()
                                    recordHint = null
                                    matchHint = null
                                },
                                label = { Text("Clear clip") },
                            )
                        }
                    }
                    if (reference != null) {
                        Text(
                            "Clip ready · ${reference!!.substringAfterLast('/')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VestraColors.Accent,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtelierFilterChip(
                            selected = false,
                            onClick = { matchVoiceToPersona() },
                            label = { Text("Match voice") },
                        )
                        AtelierFilterChip(
                            selected = calibrating,
                            onClick = { calibrateLatency() },
                            enabled = !calibrating,
                            label = {
                                if (calibrating) {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Calibrating…")
                                    }
                                } else {
                                    Text("Calibrate mic latency")
                                }
                            },
                        )
                    }
                    if (matchHint != null) {
                        Text(
                            matchHint!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (calibrationHint != null) {
                        Text(
                            calibrationHint!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = VestraColors.InkMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("VOICE CHANGER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Local DSP · pitch · speed · formant · warmth · clarity",
                        style = MaterialTheme.typography.labelSmall,
                        color = VestraColors.InkMuted,
                    )
                    KnobSlider("Pitch (semitones)", knobs.pitchSemitones, -12f..12f, "%.0f", accent = accent) {
                        viewModel.setVoiceKnobs(knobs.copy(pitchSemitones = it))
                    }
                    KnobSlider("Speed", knobs.speed, 0.5f..2f, "%.2f×", accent = accent) {
                        viewModel.setVoiceKnobs(knobs.copy(speed = it))
                    }
                    KnobSlider("Formant", knobs.formant, 0.5f..1.5f, "%.2f", accent = accent) {
                        viewModel.setVoiceKnobs(knobs.copy(formant = it))
                    }
                    KnobSlider("Warmth", knobs.warmth, 0f..1f, "%.2f", accent = accent) {
                        viewModel.setVoiceKnobs(knobs.copy(warmth = it))
                    }
                    KnobSlider("Clarity", knobs.clarity, 0f..1f, "%.2f", accent = accent) {
                        viewModel.setVoiceKnobs(knobs.copy(clarity = it))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AtelierFilterChip(
                            selected = false,
                            onClick = { viewModel.setVoiceKnobs(VoiceKnobs.Default) },
                            label = { Text("Reset knobs") },
                        )
                    }
                }
            }
            itemsIndexed(turns, key = { _, turn -> turn.id }) { index, turn ->
                StudioTurnBubble(
                    turn = turn,
                    index = index,
                    isLatest = index == turns.lastIndex,
                    accent = accent,
                    generationStartedAtMs = generationStartedAtMs,
                    onRetry = {
                        if (reference != null && prompt.equals("voice-change", true)) {
                            viewModel.applyVoiceChange()
                        } else {
                            viewModel.generateAudio()
                        }
                    },
                    onDismiss = {
                        viewModel.forceStop(showStopped = false)
                        viewModel.dismissLastTurn()
                    },
                    retryLabel = "Speak again",
                    modifier = Modifier
                        .animateItem()
                        .padding(vertical = 6.dp),
                )
            }
            item(key = "audio-clips") {
                Column {
                    Spacer(Modifier.height(18.dp))
                    GlassSectionLabel("CLIPS")
                    Text(
                        "Recordings, voice-changed results and generated speech — play them here " +
                            "to compare an original against its conversion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VestraColors.InkMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    AudioClipList(
                        clips = clips,
                        onShare = { clip ->
                            MediaExport.share(context, File(clip.path), "Share audio")
                        },
                        onDelete = { clip ->
                            if (AudioClipLibrary.delete(clip)) clips = clips.filterNot { it.path == clip.path }
                        },
                    )
                }
            }
        }

        // Docked composer — outside the scroll region so prompt, model pill, reference picker
        // and send stay reachable no matter how long the conversation gets (matches
        // UnifiedStudioPane's shape).
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.section)
                .padding(bottom = 10.dp, top = 4.dp),
        ) {
            DockedLiveLog(
                lines = liveLog,
                generationStartedAtMs = generationStartedAtMs,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PromptComposer(
                prompt = prompt,
                onPromptChange = viewModel::setPrompt,
                accent = accent,
                modelLabel = if (localAudioReady) "Device TTS (offline)" else provider.displayName,
                onModelClick = { showModelPicker = true },
                busy = busy,
                loading = warmup is GenerativeViewModel.Warmup.Loading,
                enabled = prompt.isNotBlank() || reference != null,
                onSend = {
                    if (reference != null && (prompt.isBlank() || prompt.equals("voice-change", true))) {
                        viewModel.applyVoiceChange()
                    } else {
                        viewModel.generateAudio()
                    }
                },
                onStop = viewModel::cancel,
                placeholder = "Script for ${VoiceCatalog.byId(personaId).displayName}…",
            )
        }
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
            onSelectDevice = { entry ->
                if (!entry.ready) return@ModelPickerSheet
                viewModel.appSettings.setLocalGenerator(AiCapability.AUDIO, entry.id)
            },
            onDismiss = { showModelPicker = false },
            onDeviceEntries = onDeviceEntries,
            health = viewModel.appSettings.modelHealth,
            cloudGenerationEnabled = cloudModelsEnabled,
            hasCredential = { model ->
                !model.requiresApiKey || !viewModel.appSettings.apiKeyFor(model).isNullOrBlank()
            },
            accent = accent,
        )
    }
}

@Composable
private fun KnobSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    accent: Color = VestraColors.Accent,
    onChange: (Float) -> Unit,
) {
    // A NaN/Infinite value reaching Compose's Slider crashes with "current must not be NaN"
    // (M3's internal Animatable throws it). VoiceKnobs.sanitized() should already prevent this
    // upstream, but this is the last line of defense right at the value source — coerce and log
    // rather than let the composable crash if a bad value ever gets here some other way.
    val safeValue = if (value.isNaN() || value.isInfinite()) {
        com.zakir.vestra.shared.diagnostics.EngineLogHook.w(
            "KnobSlider",
            "NaN/Inf value for \"$label\" ($value), falling back to ${range.start}",
        )
        range.start
    } else {
        value
    }
    Column(Modifier.padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = VestraColors.Ink)
            Text(format.format(safeValue), style = MaterialTheme.typography.labelSmall, color = accent)
        }
        Slider(
            value = safeValue,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
