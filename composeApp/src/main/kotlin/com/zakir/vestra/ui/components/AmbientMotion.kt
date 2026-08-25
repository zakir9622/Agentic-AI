package com.zakir.vestra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.rememberReduceMotion

/**
 * Exact-match of lookbookweb's `float-slow` utility (`styles.css:324-326`, keyframe
 * `float-y` `:336-344`): 9s ease-in-out infinite vertical bob, `±14px`. Used on the home
 * hero's single ambient orb.
 */
@Composable
fun Modifier.floatSlow(): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    val infinite = rememberInfiniteTransition(label = "floatSlow")
    val offset by infinite.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatSlowOffset",
    )
    return this.graphicsLayer { translationY = offset * density }
}

/**
 * Exact-match of lookbookweb's `drift-slow` utility (`styles.css:358-360`, keyframe `drift`
 * `:346-356`): 22s ease-in-out infinite, translate `±3%/-4%` + scale to 1.08. Used on the
 * app shell's 3 ambient background orbs (see [SpatialBackground]).
 */
@Composable
fun Modifier.driftSlow(): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    val infinite = rememberInfiniteTransition(label = "driftSlow")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "driftPhase",
    )
    return this.graphicsLayer {
        translationX = phase * 0.03f * size.width
        translationY = phase * -0.04f * size.height
        val scale = 1f + phase * 0.08f
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Exact-match of lookbookweb's `gradient-text` utility (`styles.css:276-280`):
 * `background-image: var(--gradient-accent); background-clip: text; color: transparent`.
 * Compose's brush-based `TextStyle` is the direct equivalent — used for the home hero's
 * "next creation?" span.
 */
@Composable
fun gradientTextStyle(base: TextStyle): TextStyle {
    val brush = Brush.linearGradient(
        colors = listOf(VestraColors.AccentSoft, VestraColors.Accent),
    )
    return base.copy(brush = brush)
}
