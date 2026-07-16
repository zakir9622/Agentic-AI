package com.zakir.vestra.ui.screens.generate

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.ui.TryOnViewModel
import com.zakir.vestra.ui.effects.DevelopStage

/**
 * The fitting act. Starts the run on entry; the develop-front of the AGSL
 * stage tracks real engine progress, and completion lands a haptic beat
 * before the reveal.
 */
@Composable
fun GenerationScreen(
    viewModel: TryOnViewModel,
    onComplete: () -> Unit,
    onAbort: () -> Unit,
) {
    val state by viewModel.generation.collectAsState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (state is GenerationState.Idle) viewModel.startGeneration()
    }
    LaunchedEffect(state) {
        if (state is GenerationState.Complete) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onComplete()
        }
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
                    val (fraction, label) = when (val s = state) {
                        is GenerationState.Preparing -> 0.05f to s.message
                        is GenerationState.Running -> s.fraction to s.stage
                        else -> 0f to "Preparing"
                    }
                    val animated by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(450),
                        label = "progress",
                    )
                    DevelopStage(progress = animated, modifier = Modifier.size(240.dp, 320.dp))
                    Spacer(Modifier.height(32.dp))
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
    is TryOnError.Internal -> message.ifBlank { "Something went wrong during generation. Please try again." }
}
