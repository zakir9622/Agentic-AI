package com.zakir.vestra.ui.screens.generate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.ui.TryOnViewModel

/**
 * The generation stage. Starts the run on entry and plays a scanline shimmer
 * while states stream in; the full AGSL film-developing shader replaces the
 * Canvas shimmer in M6.
 */
@Composable
fun GenerationScreen(
    viewModel: TryOnViewModel,
    onComplete: () -> Unit,
    onAbort: () -> Unit,
) {
    val state by viewModel.generation.collectAsState()

    LaunchedEffect(Unit) {
        if (state is GenerationState.Idle) viewModel.startGeneration()
    }
    LaunchedEffect(state) {
        if (state is GenerationState.Complete) onComplete()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Text(
                "Act III",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("The fitting", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(48.dp))

            when (val current = state) {
                is GenerationState.Failed -> FailureContent(current.error, onAbort)
                else -> {
                    ScanlineStage()
                    Spacer(Modifier.height(32.dp))
                    val (fraction, label) = when (val s = state) {
                        is GenerationState.Preparing -> 0.05f to s.message
                        is GenerationState.Running -> s.fraction to s.stage
                        else -> 0f to "Preparing"
                    }
                    val animated by animateFloatAsState(fraction, label = "progress")
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanlineStage() {
    val transition = rememberInfiniteTransition(label = "scanline")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val gold = MaterialTheme.colorScheme.primary
    val stage = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(Modifier.size(220.dp, 300.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = stage,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            )
            val y = size.height * sweep
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        gold.copy(alpha = 0f),
                        gold.copy(alpha = 0.55f),
                        gold.copy(alpha = 0f),
                    ),
                    startY = y - 60f,
                    endY = y + 60f,
                ),
                topLeft = Offset(0f, y - 60f),
                size = androidx.compose.ui.geometry.Size(size.width, 120f),
            )
        }
    }
}

@Composable
private fun FailureContent(error: TryOnError, onAbort: () -> Unit) {
    Text(
        text = error.userMessage(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onAbort) { Text("Back to studio") }
}

private fun TryOnError.userMessage(): String = when (this) {
    TryOnError.ModelPackMissing -> "The model pack for this engine isn't installed yet. Download it from Settings → Model packs."
    TryOnError.DeviceNotCapable -> "This device can't run the selected engine. Switch to Lite or Auto in Settings."
    TryOnError.NetworkUnavailable -> "Cloud generation needs a connection. Switch to an on-device engine to stay offline."
    is TryOnError.SafetyBlocked -> "This image can't be used: $reason"
    is TryOnError.Internal -> "Something went wrong during generation. Please try again."
}
