package com.zakir.vestra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.zakir.vestra.ui.util.rememberReduceMotion

/**
 * Exact-match of lookbookweb's `tilt-3d` utility (`styles.css:296-308`): hover =
 * `translateY(-6px) rotateX(6deg) rotateY(-6deg) scale(1.015)`. Android has no hover state, so
 * "hover" is reinterpreted as "while pressed", matching this file's existing press-driven
 * design — rotation tracks pointer position within the element, and the press itself drives the
 * lift + scale that CSS's `:hover` provided for free.
 */
@Composable
fun Modifier.tilt3d(maxDegrees: Float = 6f): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    var targetRotX by remember { mutableFloatStateOf(0f) }
    var targetRotY by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }
    val rotX by animateFloatAsState(targetRotX, label = "tiltX")
    val rotY by animateFloatAsState(targetRotY, label = "tiltY")
    val liftPx by animateFloatAsState(if (pressed) -6f else 0f, label = "tiltLift")
    val scale by animateFloatAsState(if (pressed) 1.015f else 1f, label = "tiltScale")

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                do {
                    val event = awaitPointerEvent()
                    val pos = event.changes.firstOrNull()?.position ?: break
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    targetRotY = ((pos.x - cx) / cx).coerceIn(-1f, 1f) * maxDegrees
                    targetRotX = -((pos.y - cy) / cy).coerceIn(-1f, 1f) * maxDegrees
                } while (event.changes.any { it.pressed })
                pressed = false
                targetRotX = 0f
                targetRotY = 0f
            }
        }
        .graphicsLayer {
            rotationX = rotX
            rotationY = rotY
            scaleX = scale
            scaleY = scale
            translationY = liftPx * density
            cameraDistance = 12f * density
        }
}
