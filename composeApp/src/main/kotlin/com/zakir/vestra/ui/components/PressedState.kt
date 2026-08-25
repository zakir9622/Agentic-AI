package com.zakir.vestra.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Shared "is this element currently pressed" tracker — the one gesture-detection block
 * [tilt3d], [press3d], and [lift3d] all need to drive their press-based "hover" reinterpretation
 * (Android has no `:hover`; see each modifier's own doc comment). Centralized so a fix to the
 * gesture-cancellation handling only needs to happen once.
 *
 * Returns the tracked [State] alongside the (possibly modifier-chained) [Modifier] that must be
 * used downstream — the pointer input wiring lives on that returned modifier.
 */
@Composable
fun Modifier.rememberPressedState(): Pair<Modifier, State<Boolean>> {
    val pressed = remember { mutableStateOf(false) }
    val modifier = this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed.value = true
            do {
                val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            pressed.value = false
        }
    }
    return modifier to pressed
}
