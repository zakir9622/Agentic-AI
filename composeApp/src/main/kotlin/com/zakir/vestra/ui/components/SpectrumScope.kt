package com.zakir.vestra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.VestraColors
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

private const val SCOPE_BAR_COUNT = 24

/**
 * Playback-side spectrum analyzer — renders [magnitudes] (as produced by
 * `com.zakir.vestra.shared.audio.SimpleFft.magnitudeSpectrum`) as bars, log-compressed so quiet
 * detail near the noise floor stays visible instead of being flattened by a few loud low bins.
 * Purely a rendering function: the caller owns where `magnitudes` comes from and at what rate it
 * updates — no live capture wiring here.
 */
@Composable
fun SpectrumScope(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier,
) {
    val accent = VestraColors.ModalityAudio
    val track = VestraColors.GlassBorder
    Canvas(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(RadiusTokens.sm)),
    ) {
        drawRect(track, size = Size(size.width, size.height))
        if (magnitudes.isEmpty()) return@Canvas

        val maxMag = magnitudes.maxOrNull()?.coerceAtLeast(1e-6f) ?: 1e-6f
        val bucketSize = max(1, magnitudes.size / SCOPE_BAR_COUNT)
        val barCount = min(SCOPE_BAR_COUNT, magnitudes.size)
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1f)

        var x = 0f
        for (bar in 0 until barCount) {
            val from = bar * bucketSize
            val to = min(from + bucketSize, magnitudes.size)
            var bucketMax = 0f
            for (i in from until to) bucketMax = max(bucketMax, magnitudes[i])
            // Log compression: ln(1 + x) keeps quiet bins visible without a raw dB conversion.
            val normalized = (ln(1f + bucketMax) / ln(1f + maxMag)).coerceIn(0f, 1f)
            val barHeight = max(2f, size.height * normalized)
            val top = size.height - barHeight
            drawRect(accent, topLeft = Offset(x, top), size = Size(barWidth, barHeight))
            x += barWidth + gap
        }
    }
}
