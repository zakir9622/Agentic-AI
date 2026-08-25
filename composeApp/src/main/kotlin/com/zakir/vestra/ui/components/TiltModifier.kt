package com.zakir.vestra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.zakir.vestra.ui.util.rememberReduceMotion

@Composable
fun Modifier.tilt3d(maxDegrees: Float = 6f): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    var targetRotX by remember { mutableFloatStateOf(0f) }
    var targetRotY by remember { mutableFloatStateOf(0f) }
    val rotX by animateFloatAsState(targetRotX, label = "tiltX")
    val rotY by animateFloatAsState(targetRotY, label = "tiltY")

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val pos = event.changes.firstOrNull()?.position ?: break
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    targetRotY = ((pos.x - cx) / cx).coerceIn(-1f, 1f) * maxDegrees
                    targetRotX = -((pos.y - cy) / cy).coerceIn(-1f, 1f) * maxDegrees
                } while (event.changes.any { it.pressed })
                targetRotX = 0f
                targetRotY = 0f
            }
        }
        .graphicsLayer {
            rotationX = rotX
            rotationY = rotY
            cameraDistance = 12f * density
        }
}
