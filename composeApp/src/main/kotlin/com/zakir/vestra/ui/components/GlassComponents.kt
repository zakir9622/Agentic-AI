package com.zakir.vestra.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.SpatialElevation
import com.zakir.vestra.ui.theme.VestraColors

/** Full-screen spatial canvas with soft gradient orbs behind content. */
@Composable
fun SpatialBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VestraColors.Canvas),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VestraColors.Accent.copy(alpha = if (VestraColors.Accent.alpha > 0f) 0.18f else 0.18f),
                            Color.Transparent,
                        ),
                        radius = 1100f,
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VestraColors.AccentSoft.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        center = androidx.compose.ui.geometry.Offset(0.92f, 0.08f),
                        radius = 800f,
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            VestraColors.Canvas.copy(alpha = 0.35f),
                        ),
                    ),
                ),
        )
        content()
    }
}

/** Frosted glass card — semi-transparent fill, highlight border, spatial shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val glassFill = VestraColors.GlassFill
    val base = Modifier
        .fillMaxWidth()
        .then(
            if (elevation > 0.dp) {
                Modifier.graphicsLayer {
                    // Soft elevation; avoid ambient/spot shadow colors that blank on some GPU paths.
                    shadowElevation = elevation.toPx().coerceAtMost(12f)
                }
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(glassFill)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    VestraColors.GlassHighlight,
                    VestraColors.GlassBorder,
                ),
            ),
            shape = shape,
        )

    if (onClick != null) {
        Surface(onClick = onClick, modifier = base, color = Color.Transparent, shape = shape) {
            Column(Modifier.padding(18.dp), content = content)
        }
    } else {
        Column(base.padding(18.dp), content = content)
    }
}

@Composable
fun GlassSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation()
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        Row(content = actions)
    }
}

@Composable
fun GlassPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = VestraColors.Accent,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(if (active) VestraColors.GlassFillStrong else VestraColors.GlassFill)
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.5f) else VestraColors.GlassBorder,
                shape,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(end = 8.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) accent else VestraColors.InkMuted.copy(alpha = 0.4f)),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) VestraColors.Ink else VestraColors.InkMuted,
        )
    }
}

/** Standard spatial screen shell: gradient canvas, glass top bar, scrollable body. */
@Composable
fun GlassScreen(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SpatialBackground(modifier) {
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier),
        ) {
            GlassTopBar(
                title = title,
                subtitle = subtitle,
                navigation = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions,
            )
            Spacer(Modifier.padding(top = 16.dp))
            content()
        }
    }
}

/** Elevated glass frame for image previews and model cards. */
@Composable
fun GlassImageFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(VestraColors.GlassHighlight, VestraColors.GlassBorder),
                ),
                shape,
            )
            .graphicsLayer {
                shadowElevation = 0f
            },
        content = content,
    )
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (enabled) VestraColors.Accent else VestraColors.Accent.copy(alpha = 0.4f),
        contentColor = Color.White,
    ) {
        Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VestraColors.GlassBorder, shape),
        shape = shape,
        color = VestraColors.GlassFillStrong,
        contentColor = VestraColors.Ink,
    ) {
        Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun GlassEmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    GlassCard(modifier = modifier) {
        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.padding(top = 12.dp))
                    GlassSecondaryButton(text = actionLabel, onClick = onAction)
                }
            }
        }
    }
}

@Composable
fun GlassErrorBanner(message: String, onRetry: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null) {
    GlassCard {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.padding(top = 8.dp))
        Row {
            if (onRetry != null) {
                GlassSecondaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.weight(1f))
            }
            if (onDismiss != null) {
                if (onRetry != null) Spacer(Modifier.padding(horizontal = 6.dp))
                GlassSecondaryButton(text = "Dismiss", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GlassLoadingCard(message: String, progress: Float? = null) {
    GlassCard {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.padding(top = 10.dp))
        if (progress != null) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = VestraColors.Accent,
                trackColor = VestraColors.GlassBorder,
            )
        } else {
            androidx.compose.material3.CircularProgressIndicator(color = VestraColors.Accent)
        }
    }
}
