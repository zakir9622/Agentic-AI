package com.zakir.vestra.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.audio.AndroidPlaybackVisualizer
import com.zakir.vestra.ui.theme.VestraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A single-clip in-app audio player — play/pause, a scrubber-style progress bar, and a live
 * [SpectrumScope] while playing. Modeled on [AudioClipList]'s per-row player (same MediaPlayer/
 * visualizer/progress-polling pattern), for [ResultPane]'s [com.zakir.vestra.shared.cloud.GenerativeState.AudioReady]
 * card — a single result never needs [AudioClipList]'s "only one clip plays across the whole
 * list" cross-row coordination, so this stays a separate, simpler implementation rather than a
 * shared one; a fix here (e.g. the leaked-MediaPlayer-on-failed-prepare fix below) is not
 * automatically mirrored in [AudioClipList]'s own copy.
 *
 * [onPlaybackFailed] fires once if the file can't even start playing (e.g. an unsupported codec)
 * so the caller can offer a fallback (opening it in an external player) rather than leaving a
 * player that silently does nothing.
 */
@Composable
fun InlineAudioPlayer(
    path: String,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = VestraColors.Accent,
    onPlaybackFailed: (() -> Unit)? = null,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    var magnitudes by remember { mutableStateOf(FloatArray(0)) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val visualizer = remember { mutableStateOf<AndroidPlaybackVisualizer?>(null) }

    fun stop() {
        runCatching { player.value?.stop() }
        runCatching { player.value?.release() }
        player.value = null
        visualizer.value?.stop()
        visualizer.value = null
        isPlaying = false
        positionMs = 0
        magnitudes = FloatArray(0)
    }

    // Release the player when this leaves composition, otherwise audio keeps playing after the
    // user navigates away from the result.
    DisposableEffect(path) { onDispose { stop() } }

    fun toggle() {
        if (isPlaying) {
            stop()
            return
        }
        // Held outside the runCatching so a failure between construction and `player.value =
        // mp` (e.g. setDataSource/prepare throwing on an unsupported file) can still release
        // this specific instance — stop() alone only releases whatever's already in player.value,
        // which would still be null at that point, leaking the just-constructed MediaPlayer.
        val mp = MediaPlayer()
        runCatching {
            mp.setDataSource(path)
            mp.setOnCompletionListener { stop() }
            mp.prepare()
            mp.start()
            player.value = mp
            isPlaying = true
            durationMs = mp.duration.coerceAtLeast(0)
            visualizer.value = AndroidPlaybackVisualizer(mp.audioSessionId).also { it.start() }
        }.onFailure {
            runCatching { mp.release() }
            stop()
            onPlaybackFailed?.invoke()
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        launch {
            visualizer.value?.magnitudes?.collectLatest { magnitudes = it }
        }
        while (isPlaying) {
            positionMs = runCatching { player.value?.currentPosition ?: 0 }.getOrDefault(0)
            delay(200)
        }
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = { toggle() }) {
                Icon(
                    if (isPlaying) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = accent,
                )
            }
            Text(
                if (isPlaying) "Playing…" else "Tap to play",
                style = MaterialTheme.typography.bodySmall,
                color = VestraColors.InkMuted,
            )
        }
        if (isPlaying && durationMs > 0) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
            )
        }
        if (isPlaying) {
            Spacer(Modifier.height(8.dp))
            SpectrumScope(magnitudes = magnitudes)
        }
    }
}
