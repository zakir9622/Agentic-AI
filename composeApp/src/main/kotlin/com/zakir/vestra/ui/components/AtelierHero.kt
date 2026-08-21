package com.zakir.vestra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.VestraColors
import kotlin.math.abs

/**
 * Full-bleed atelier hero — brand dominates; silk-panel collage is the visual plane.
 * One headline, one support, one CTA. No dashboard clutter in the first viewport.
 */
@Composable
fun AtelierHero(
    brand: String,
    headline: String,
    support: String,
    cta: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    statusLine: String? = null,
) {
    val infinite = rememberInfiniteTransition(label = "heroScan")
    val scan by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan",
    )
    val drift by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "panelDrift",
    )
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VestraColors.AtelierContainer,
                        VestraColors.AtelierCanvas,
                        Color(0xFF050408),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        VestraColors.AccentSoft.copy(alpha = 0.55f),
                        VestraColors.GlassBorder.copy(alpha = 0.2f),
                        VestraColors.Accent.copy(alpha = 0.4f),
                    ),
                ),
                shape = shape,
            ),
    ) {
        // Silk panel collage — fashion atmosphere without stock photography.
        SilkPanel(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (28 + drift).dp, y = (-12).dp)
                .rotate(12f)
                .size(160.dp, 210.dp),
            colors = listOf(Color(0xFF3D2A18), VestraColors.SaffronDeep.copy(alpha = 0.85f), Color(0xFF1A120C)),
        )
        SilkPanel(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-40 + drift * 0.4f).dp, y = 36.dp)
                .rotate(-8f)
                .size(130.dp, 180.dp),
            colors = listOf(Color(0xFF1E2430), Color(0xFF2A3344), VestraColors.SilkMist.copy(alpha = 0.35f)),
        )
        SilkPanel(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 18.dp, y = (40 - drift).dp)
                .rotate(6f)
                .size(110.dp, 150.dp),
            colors = listOf(Color(0xFF2C1810), Color(0xFF5C3A22), VestraColors.Accent.copy(alpha = 0.55f)),
        )

        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            VestraColors.AtelierCanvas.copy(alpha = 0.15f),
                            VestraColors.AtelierCanvas.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.55f)
                .height(2.dp)
                .offset(y = (36 + scan * 280).dp)
                .graphicsLayer { alpha = 0.2f + (1f - abs(scan - 0.5f)) * 0.4f }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, VestraColors.AccentSoft, Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(22.dp),
        ) {
            Text(
                brand,
                style = MaterialTheme.typography.displayMedium,
                color = VestraColors.Ivory,
            )
            Text(
                headline,
                style = MaterialTheme.typography.titleLarge,
                color = VestraColors.AccentSoft,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                support,
                style = MaterialTheme.typography.bodyMedium,
                color = VestraColors.IvoryMuted,
                modifier = Modifier.padding(top = 10.dp, end = 8.dp),
            )
            if (statusLine != null) {
                Text(
                    statusLine,
                    style = MaterialTheme.typography.labelMedium,
                    color = VestraColors.AccentSoft.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            GlassPrimaryButton(
                text = cta,
                onClick = onCta,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

@Composable
private fun SilkPanel(
    modifier: Modifier,
    colors: List<Color>,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .border(
                1.dp,
                VestraColors.AccentSoft.copy(alpha = 0.25f),
                RoundedCornerShape(22.dp),
            ),
    )
}
