package com.zakir.vestra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.zakir.vestra.ui.util.rememberReduceMotion

/**
 * Exact-match of lookbookweb's `press-3d` utility (`styles.css:310-322`): hover
 * `translateY(-2px)`, active `translateY(1px) scale(0.98)`. Android has no hover, so — like
 * [tilt3d] — the lift is driven by the press itself: down = lift+shrink toward the active
 * state, up = settles back to rest. Intended for pill buttons/chips/nav rows, not full cards
 * (those use [GlassCard]'s own inline press-scale, or [lift3d] below for list rows).
 */
@Composable
fun Modifier.press3d(): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    var pressed by remember { mutableStateOf(false) }
    val liftPx by animateFloatAsState(if (pressed) 1f else -2f, label = "pressLift")
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "pressScale")

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                do {
                    val event = awaitPointerEvent()
                } while (event.changes.any { it.pressed })
                pressed = false
            }
        }
        .graphicsLayer {
            translationY = liftPx * density
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Exact-match of lookbookweb's `lift-3d` utility (`styles.css:395-404`): hover
 * `translateY(-3px) scale(1.008)`, no rotation — a gentler cousin of [tilt3d] for list rows
 * (job/history/model rows) that shouldn't tilt like a hero card. Same press-driven
 * reinterpretation of "hover" as the rest of this file.
 */
@Composable
fun Modifier.lift3d(): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    var pressed by remember { mutableStateOf(false) }
    val liftPx by animateFloatAsState(if (pressed) -3f else 0f, label = "listLift")
    val scale by animateFloatAsState(if (pressed) 1.008f else 1f, label = "listScale")

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                do {
                    val event = awaitPointerEvent()
                } while (event.changes.any { it.pressed })
                pressed = false
            }
        }
        .graphicsLayer {
            translationY = liftPx * density
            scaleX = scale
            scaleY = scale
        }
}
