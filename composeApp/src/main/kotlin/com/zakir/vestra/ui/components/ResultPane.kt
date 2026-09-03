package com.zakir.vestra.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import com.zakir.vestra.ui.theme.RadiusTokens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.data.LocalReportStore
import com.zakir.vestra.data.ReportReason
import com.zakir.vestra.media.MediaExport
import com.zakir.vestra.shared.cloud.GenerativeState
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.time.formatDurationSeconds
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.VestraColors
import androidx.compose.ui.graphics.Color
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun ResultPane(
    state: GenerativeState?,
    generationStartedAtMs: Long? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    retryLabel: String = LookbookCopy.ACTION_RETRY,
    accent: Color = VestraColors.Accent,
) {
    val context = LocalContext.current
    val reportStore = remember { LocalReportStore(context) }
    var reportPath by remember { mutableStateOf<String?>(null) }
    var privacyBlurPath by remember { mutableStateOf<String?>(null) }
    var fullscreenPath by remember { mutableStateOf<String?>(null) }

    fullscreenPath?.let { path ->
        FullScreenImageViewer(
            imagePath = path,
            onDismiss = { fullscreenPath = null },
            // Both close the viewer first: PrivacyBlurSheet and the report dialog are their own
            // windows, and stacking one over a full-screen Dialog leaves the viewer's black
            // scrim between the sheet and the image it is about to edit.
            onPrivacyBlur = {
                fullscreenPath = null
                privacyBlurPath = path
            },
            onReport = {
                fullscreenPath = null
                reportPath = path
            },
        )
    }

    privacyBlurPath?.let { path ->
        PrivacyBlurSheet(
            imagePath = path,
            onDismiss = { privacyBlurPath = null },
            onSaved = {
                privacyBlurPath = null
                GlassSnackbar.show("Saved", SnackbarLevel.SUCCESS)
            },
        )
    }

    reportPath?.let { path ->
        AlertDialog(
            onDismissRequest = { reportPath = null },
            title = { Text("Report content") },
            text = {
                Column {
                    Text("Reports are stored on this device only (no paid services). Why are you reporting?")
                    Spacer(Modifier.height(8.dp))
                    ReportReason.entries.forEach { reason ->
                        TextButton(
                            onClick = {
                                reportStore.submit(path, reason)
                                reportPath = null
                                GlassSnackbar.show("Report saved locally", SnackbarLevel.SUCCESS)
                            },
                            modifier = Modifier.testTag(TestTags.reportReason(reason.name)),
                        ) { Text(reason.label) }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { reportPath = null },
                    modifier = Modifier.testTag(TestTags.REPORT_CANCEL_BUTTON),
                ) { Text("Cancel") }
            },
        )
    }

    when (state) {
        null -> Unit
        // Cancel lives on the composer's own send button (spinner while loading, stop icon
        // while generating) — a second Cancel here duplicated that control. The live log is
        // docked next to the composer instead of a scrollable card competing with this pane.
        is GenerativeState.Preparing -> GlassLoadingCard(state.message, accent = accent)
        is GenerativeState.Running -> {
            var tick by remember(state.deadlineEpochMs, state.stage, generationStartedAtMs) {
                mutableIntStateOf(0)
            }
            LaunchedEffect(state.deadlineEpochMs, generationStartedAtMs) {
                while (true) {
                    delay(1_000)
                    tick++
                }
            }
            @Suppress("UNUSED_EXPRESSION")
            tick // recompose each second
            val remSec = state.deadlineEpochMs?.let { deadline ->
                ((deadline - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L)
            }
            val elapsedSec = generationStartedAtMs?.let { start ->
                ((System.currentTimeMillis() - start) / 1_000L).coerceAtLeast(0L)
            }
            val message = buildString {
                append(state.stage)
                if (remSec != null) append(" · ${formatDurationSeconds(remSec)} left")
                if (elapsedSec != null) append(" · ${formatDurationSeconds(elapsedSec)} elapsed")
            }
            GlassLoadingCard(
                message = message,
                progress = state.fraction,
                accent = accent,
            )
        }
        // Creative Studio V2 batch result (1-4 candidates from one prompt). Tapping a candidate
        // opens it fullscreen (Save/Share/Close only — Remix and edit-intents aren't wired since
        // they'd need a verified content-URI conversion for a plain generation path this session
        // has no device to confirm). All candidates are already saved to the Wardrobe (see
        // GenerativeViewModel.ingestImageBatch).
        // Same rule as the single result below: candidates only, actions in the viewer.
        is GenerativeState.ImageBatchReady -> Box(Modifier.testTag(TestTags.RESULT_IMAGE_READY)) {
            ImageCandidateGrid(
                batch = state.batch,
                selectedCandidateId = state.batch.selectedCandidateId,
                onOpenCandidate = { candidate -> fullscreenPath = candidate.path },
            )
        }
        // A result is a picture, so the thread shows a picture. It used to arrive wrapped in a
        // card carrying a "RESULT" label, two provenance pills and four full-width buttons —
        // roughly 300dp of chrome around 320dp of image, on the one surface where the image is
        // the entire point. Save / Share / Privacy blur / Report all moved into the full-screen
        // viewer, which is where a user who wants to act on an image already is.
        //
        // The one thing that does *not* move is the AI-generated marker. It is a provenance
        // disclosure, not a control, and a synthetic image of a person needs to be labelled
        // where it is seen rather than one tap away — so it renders on the image itself.
        is GenerativeState.ImageReady -> GeneratedImageResult(
            path = state.path,
            onOpen = { fullscreenPath = state.path },
        )
        is GenerativeState.VideoReady -> GlassCard(modifier = Modifier.testTag(TestTags.RESULT_VIDEO_READY)) {
            GlassSectionLabel("VIDEO READY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(text = "AI-generated", active = true)
                GlassPill(text = "In looks gallery", active = true, accent = accent)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Saved on device. Save to Movies/The Lookbook or open with a system player.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(state.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSecondaryButton(
                    text = "Save to Gallery",
                    onClick = { MediaExport.saveVideoToGallery(context, File(state.path)) },
                    modifier = Modifier.weight(1f),
                )
                GlassSecondaryButton(
                    text = LookbookCopy.ACTION_OPEN_VIDEO,
                    onClick = { MediaExport.openVideo(context, File(state.path)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSecondaryButton(
                    text = LookbookCopy.ACTION_SHARE,
                    onClick = { MediaExport.share(context, File(state.path), "Share clip") },
                    modifier = Modifier.weight(1f),
                )
                GlassSecondaryButton(
                    text = LookbookCopy.ACTION_REPORT,
                    onClick = { reportPath = state.path },
                    modifier = Modifier.weight(1f).testTag(TestTags.REPORT_BUTTON),
                )
            }
        }
        is GenerativeState.AudioReady -> GlassCard(modifier = Modifier.testTag(TestTags.RESULT_AUDIO_READY)) {
            var playbackFailed by remember(state.path) { mutableStateOf(false) }
            GlassSectionLabel("AUDIO READY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(text = "AI voice", active = true)
                GlassPill(text = "Knobs applied locally", active = true, accent = accent)
            }
            Spacer(Modifier.height(10.dp))
            InlineAudioPlayer(
                path = state.path,
                accent = accent,
                onPlaybackFailed = { playbackFailed = true },
            )
            if (playbackFailed) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Couldn't play this in-app — try opening it externally.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (playbackFailed) {
                    GlassSecondaryButton(
                        text = "Open externally",
                        onClick = { MediaExport.openAudio(context, File(state.path)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                GlassSecondaryButton(
                    text = LookbookCopy.ACTION_SHARE,
                    onClick = { MediaExport.share(context, File(state.path), "Share audio") },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            GlassSecondaryButton(
                text = "Save to Music",
                onClick = { MediaExport.saveAudioToMusic(context, File(state.path)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = LookbookCopy.ACTION_REPORT,
                onClick = { reportPath = state.path },
                modifier = Modifier.testTag(TestTags.REPORT_BUTTON),
            )
        }
        is GenerativeState.CodeStreaming -> GlassCard(modifier = Modifier.testTag(TestTags.RESULT_CODE_STREAMING)) {
            GlassSectionLabel("CODE · generating…")
            Spacer(Modifier.height(8.dp))
            // Live output as the model streams it, not a spinner that resolves into a wall of
            // text only once the whole response is done — the same CodeOutput used for the
            // finished result, just re-rendered on every chunk while it's still growing.
            CodeOutput(
                text = state.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
        is GenerativeState.CodeReady -> GlassCard(modifier = Modifier.testTag(TestTags.RESULT_CODE_READY)) {
            GlassSectionLabel("CODE · ${state.tokensIn + state.tokensOut} free tokens")
            Text(
                "${state.tokensIn} in · ${state.tokensOut} out",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            // Segmented output: prose stays readable, each fenced block gets its own language
            // label and Copy, so pasting code no longer drags the explanation along with it.
            CodeOutput(
                text = state.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = "Copy all",
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("lookbook-code", state.text))
                    GlassSnackbar.show("Copied", SnackbarLevel.SUCCESS)
                },
            )
        }
        is GenerativeState.TranscribeReady -> GlassCard(modifier = Modifier.testTag(TestTags.RESULT_TRANSCRIBE_READY)) {
            GlassSectionLabel("TRANSCRIPTION")
            Text(
                "On-device · ${state.providerId}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                state.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(10.dp))
            GlassSecondaryButton(
                text = "Copy transcript",
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("lookbook-transcript", state.text))
                    GlassSnackbar.show("Copied", SnackbarLevel.SUCCESS)
                },
            )
        }
        is GenerativeState.Failed -> GlassErrorBanner(
            message = state.message,
            onRetry = onRetry,
            retryLabel = retryLabel,
            onDismiss = onDismiss,
        )
    }
}

/**
 * A generated image in the thread: the picture, an AI-generated marker, and nothing else.
 *
 * [ContentScale.FillWidth] with an unconstrained height rather than the old fixed 320dp box —
 * a portrait result in a 320dp-tall Fit box rendered as a narrow strip floating in two grey
 * bands, which reads as a layout bug rather than as a photograph. The cap only exists so a very
 * tall image cannot push the composer off screen.
 *
 * The marker stays on the image deliberately. Every other control moved into the viewer, but a
 * synthetic image of a person has to be labelled where it is seen — a disclosure one tap away is
 * not a disclosure. It is drawn as part of the picture, not as chrome around it.
 */
@Composable
private fun GeneratedImageResult(path: String, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.RESULT_IMAGE_READY)
            .clip(shape)
            .clickable(onClick = onOpen),
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "Generated look. Tap to open full screen.",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            contentScale = ContentScale.FillWidth,
        )
        AiGeneratedMarker(Modifier.align(Alignment.BottomStart).padding(10.dp))
    }
}

/** Provenance badge drawn over the image. Reads on any picture, light or dark. */
@Composable
private fun AiGeneratedMarker(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "AI-generated",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
        )
    }
}
