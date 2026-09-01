package com.zakir.vestra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.zakir.vestra.ui.TestTags

/** A user-drawn rectangle in the overlay's own local coordinate space (pixels). */
data class BlurRegion(val rect: ComposeRect)

/**
 * Drag-to-draw manual blur regions on top of a generated image — the B7 fallback for anything
 * ML Kit's face detector misses (a hand, a tattoo, a license plate, a face at an angle it didn't
 * catch). Regions are reported in the overlay's own local coordinate space; the caller is
 * responsible for mapping that to the underlying bitmap's pixel space (they know the displayed
 * scale/crop, this composable doesn't).
 */
@Composable
fun RegionBlurOverlay(
    regions: List<BlurRegion>,
    onRegionsChange: (List<BlurRegion>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier
            .fillMaxSize()
            .testTag(TestTags.PRIVACY_BLUR_CANVAS)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragCurrent = offset
                    },
                    onDrag = { change, _ ->
                        dragCurrent = change.position
                    },
                    onDragEnd = {
                        val start = dragStart
                        val end = dragCurrent
                        if (start != null && end != null) {
                            val rect = ComposeRect(
                                left = minOf(start.x, end.x),
                                top = minOf(start.y, end.y),
                                right = maxOf(start.x, end.x),
                                bottom = maxOf(start.y, end.y),
                            )
                            if (rect.width > 4f && rect.height > 4f) {
                                onRegionsChange(regions + BlurRegion(rect))
                            }
                        }
                        dragStart = null
                        dragCurrent = null
                    },
                    onDragCancel = {
                        dragStart = null
                        dragCurrent = null
                    },
                )
            },
    ) {
        regions.forEach { region ->
            drawRect(
                color = Color(0x552B7A78),
                topLeft = Offset(region.rect.left, region.rect.top),
                size = androidx.compose.ui.geometry.Size(region.rect.width, region.rect.height),
            )
        }
        val start = dragStart
        val current = dragCurrent
        if (start != null && current != null) {
            drawRect(
                color = Color(0x332B7A78),
                topLeft = Offset(minOf(start.x, current.x), minOf(start.y, current.y)),
                size = androidx.compose.ui.geometry.Size(
                    kotlin.math.abs(current.x - start.x),
                    kotlin.math.abs(current.y - start.y),
                ),
            )
        }
    }
}
