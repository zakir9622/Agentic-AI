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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.VestraColors
import kotlin.math.abs

/**
 * Full-bleed atelier hero — brand as the dominant signal, one headline,
 * one support line, one CTA. Scanning edge nods to spatial fashion UIs
 * without neon purple / dashboard clutter.
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
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan",
    )
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(430.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VestraColors.AtelierContainer,
                        VestraColors.AtelierCanvas,
                        Color(0xFF070C11),
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
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VestraColors.Accent.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.62f)
                .height(2.dp)
                .offset(y = (28 + scan * 350).dp)
                .graphicsLayer { alpha = 0.25f + (1f - abs(scan - 0.5f)) * 0.45f }
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
                modifier = Modifier.padding(top = 10.dp, end = 4.dp),
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
