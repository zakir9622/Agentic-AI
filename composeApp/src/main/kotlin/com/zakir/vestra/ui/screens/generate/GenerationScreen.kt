package com.zakir.vestra.ui.screens.generate

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.engine.pipeline.ConditioningStage
import com.zakir.vestra.ui.TryOnViewModel
import com.zakir.vestra.ui.effects.DevelopStage
import com.zakir.vestra.ui.theme.VestraColors

/**
 * The atelier monitor. A true-black canvas with a single frosted, glass-edged
 * container, perfectly centered — a cinematic view onto the multi-conditioning
 * pipeline (structure → texture → synthesis). Starts the shoot on entry; the
 * develop-front tracks real per-shot progress; completion lands a haptic beat.
 */
@Composable
fun GenerationScreen(
    viewModel: TryOnViewModel,
    onComplete: () -> Unit,
    onAbort: () -> Unit,
) {
    val shoot by viewModel.shoot.collectAsState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (shoot == null) viewModel.startShoot()
    }
    LaunchedEffect(shoot) {
        val current = shoot ?: return@LaunchedEffect
        if (current.inner is GenerationState.Complete) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (current.isFinished) onComplete()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(VestraColors.AtelierCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "THE SHOOT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            val current = shoot
            if (current != null && current.totalShots > 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Shot ${current.shotIndex + 1} of ${current.totalShots}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VestraColors.IvoryMuted,
                )
            }
            Spacer(Modifier.height(28.dp))

            // Frosted, glass-edged monitor frame — perfectly centered.
            Box(
                Modifier
                    .size(268.dp, 356.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(VestraColors.AtelierContainer)
                    .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(24.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (val inner = current?.inner) {
                    is GenerationState.Failed -> FailureContent(inner.error, onAbort)
                    else -> {
                        val shotFraction = when (val s = current?.inner) {
                            is GenerationState.Preparing -> 0.05f
                            is GenerationState.Running -> s.fraction
                            else -> 0f
                        }
                        val animatedShot by animateFloatAsState(
                            targetValue = shotFraction,
                            animationSpec = tween(450),
                            label = "shot",
                        )
                        DevelopStage(
                            progress = animatedShot,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                }
            }

            if (current?.inner !is GenerationState.Failed) {
                Spacer(Modifier.height(28.dp))
                val (shotFraction, label) = when (val s = current?.inner) {
                    is GenerationState.Preparing -> 0.05f to s.message
                    is GenerationState.Running -> s.fraction to s.stage
                    else -> 0f to ConditioningStage.STRUCTURE.label
                }
                val overall = if (current == null) {
                    0f
                } else {
                    (current.shotIndex + shotFraction) / current.totalShots
                }
                val animatedOverall by animateFloatAsState(
                    targetValue = overall,
                    animationSpec = tween(450),
                    label = "overall",
                )
                LinearProgressIndicator(
                    progress = { animatedOverall },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = VestraColors.GlassBorder,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VestraColors.Ivory,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FailureContent(error: TryOnError, onAbort: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = error.userMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = VestraColors.IvoryMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAbort) { Text("Back to studio") }
    }
}

private fun TryOnError.userMessage(): String = when (this) {
    TryOnError.ModelPackMissing -> "The model pack for this engine isn't installed yet. Download it from Settings → Model packs."
    TryOnError.DeviceNotCapable -> "This device can't run the selected engine. Switch to Lite or Auto in Settings."
    is TryOnError.SafetyBlocked -> "This image can't be used: $reason"
    is TryOnError.Internal -> message.ifBlank { "Something went wrong during generation. Please try again." }
}
