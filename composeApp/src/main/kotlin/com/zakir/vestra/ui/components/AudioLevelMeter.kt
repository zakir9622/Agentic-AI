package com.zakir.vestra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.rememberReduceMotion
import kotlin.math.max

private const val BAR_COUNT = 12

/**
 * Real-time mic-input level meter — [amplitude] bars scroll left as new samples arrive, one bar
 * per recent reading (rolling history), so the shape of recent speech is visible, not just the
 * instantaneous level. Gated by [rememberReduceMotion]: a single static bar at the current level
 * when the user has reduced motion enabled, matching every other animation in this app.
 */
@Composable
fun AudioLevelMeter(
    amplitude: Float,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    var history by remember { mutableStateOf(List(BAR_COUNT) { 0f }) }

    LaunchedEffect(amplitude) {
        if (!reduceMotion) {
            history = (history.drop(1) + amplitude.coerceIn(0f, 1f))
        }
    }

    val accent = VestraColors.Accent
    val track = VestraColors.GlassBorder

    Canvas(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(RadiusTokens.sm)),
    ) {
        if (reduceMotion) {
            drawStaticLevel(amplitude.coerceIn(0f, 1f), accent, track)
        } else {
            drawBars(history, accent, track)
        }
    }
}

private fun DrawScope.drawStaticLevel(level: Float, accent: androidx.compose.ui.graphics.Color, track: androidx.compose.ui.graphics.Color) {
    drawRect(track, size = Size(size.width, size.height))
    drawRect(accent, size = Size(size.width * level, size.height))
}

private fun DrawScope.drawBars(history: List<Float>, accent: androidx.compose.ui.graphics.Color, track: androidx.compose.ui.graphics.Color) {
    if (history.isEmpty()) return
    val gap = 3.dp.toPx()
    val barWidth = ((size.width - gap * (history.size - 1)) / history.size).coerceAtLeast(1f)
    var x = 0f
    for (level in history) {
        val barHeight = max(2f, size.height * level.coerceIn(0f, 1f))
        val top = size.height - barHeight
        drawRect(track, topLeft = androidx.compose.ui.geometry.Offset(x, 0f), size = Size(barWidth, size.height))
        drawRect(accent, topLeft = androidx.compose.ui.geometry.Offset(x, top), size = Size(barWidth, barHeight))
        x += barWidth + gap
    }
}
