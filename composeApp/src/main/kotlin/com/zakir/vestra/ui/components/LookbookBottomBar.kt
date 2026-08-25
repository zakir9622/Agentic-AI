package com.zakir.vestra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.VestraColors

enum class BottomBarDestination(val label: String, val testTag: String, val icon: ImageVector) {
    HOME("Home", TestTags.BOTTOM_BAR_HOME, Icons.Outlined.Home),
    LIBRARY("Library", TestTags.BOTTOM_BAR_LIBRARY, Icons.Outlined.Checkroom),
    SETTINGS("Settings", TestTags.BOTTOM_BAR_SETTINGS, Icons.Outlined.Settings),
}

/**
 * Three destinations only: Home (the tool-picker grid), Library, Settings. Image/Video/Audio/Code
 * and Chat are reached from the Home grid, not from this bar or a "+" FAB — each is a fully
 * isolated screen, so there's nothing here to jump directly into a specific studio with.
 */
@Composable
fun LookbookBottomBar(
    selected: BottomBarDestination?,
    onSelect: (BottomBarDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.BOTTOM_BAR)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                // dock-shadow: the heavier shadow-lift tier, not the default card shadow.
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(RadiusTokens.xl4))
                .clip(RoundedCornerShape(RadiusTokens.xl4))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VestraColors.GlassFillStrong.copy(alpha = 0.96f),
                            VestraColors.GlassFill.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(VestraColors.GlassHighlight, VestraColors.GlassBorder.copy(alpha = 0.4f)),
                    ),
                    shape = RoundedCornerShape(RadiusTokens.xl4),
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockItem(
                    destination = BottomBarDestination.HOME,
                    selected = selected == BottomBarDestination.HOME,
                    onClick = { onSelect(BottomBarDestination.HOME) },
                )
                DockItem(
                    destination = BottomBarDestination.LIBRARY,
                    selected = selected == BottomBarDestination.LIBRARY,
                    onClick = { onSelect(BottomBarDestination.LIBRARY) },
                )
                DockItem(
                    destination = BottomBarDestination.SETTINGS,
                    selected = selected == BottomBarDestination.SETTINGS,
                    onClick = { onSelect(BottomBarDestination.SETTINGS) },
                )
            }
        }
    }
}

/**
 * Exact-match of lookbookweb's active dock-item treatment (`AppShell.tsx:154-178`,
 * `DockLink`): the active item swaps to `!text-accent-foreground gradient-pill shadow-none` —
 * i.e. the whole item gets filled with the accent gradient, not just a color/dot change.
 */
@Composable
private fun DockItem(
    destination: BottomBarDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_item_scale",
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "dock_item_fill",
    )
    val contentColor = if (selected) Color.White else VestraColors.InkMuted

    Column(
        modifier = modifier
            .testTag(destination.testTag)
            .scale(scale)
            .clip(RoundedCornerShape(RadiusTokens.xl4))
            .drawBehind {
                if (fillAlpha > 0f) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(VestraColors.AccentSoft, VestraColors.Accent),
                        ),
                        alpha = fillAlpha,
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = destination.label }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = contentColor,
        )
    }
}
