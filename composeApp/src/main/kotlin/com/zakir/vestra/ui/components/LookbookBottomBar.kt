package com.zakir.vestra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.draw.scale
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
    CREATE("Create", TestTags.BOTTOM_BAR_CREATE, Icons.Outlined.Add),
    CHAT("Chat", TestTags.BOTTOM_BAR_CHAT, Icons.AutoMirrored.Outlined.Chat),
    SETTINGS("Settings", TestTags.BOTTOM_BAR_SETTINGS, Icons.Outlined.Settings),
}

/**
 * Lookbookweb-parity bottom dock: Home / Library / a raised center Create FAB / Chat / Settings,
 * styled as a floating glass pill (not a full-width bar) with a gradient center FAB — matching
 * the reference web app's dock language. The in-studio pager (Image/Video/Audio/Code) is a
 * separate, lower level of navigation nested inside the Home destination — this bar only moves
 * between top-level destinations.
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
                .clip(RoundedCornerShape(RadiusTokens.xl))
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
                    shape = RoundedCornerShape(RadiusTokens.xl),
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                Spacer(modifier = Modifier.width(56.dp))
                DockItem(
                    destination = BottomBarDestination.CHAT,
                    selected = selected == BottomBarDestination.CHAT,
                    onClick = { onSelect(BottomBarDestination.CHAT) },
                )
                DockItem(
                    destination = BottomBarDestination.SETTINGS,
                    selected = selected == BottomBarDestination.SETTINGS,
                    onClick = { onSelect(BottomBarDestination.SETTINGS) },
                )
            }
        }

        CreateFab(
            onClick = { onSelect(BottomBarDestination.CREATE) },
            modifier = Modifier.offset(y = (-14).dp),
        )
    }
}

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
        targetValue = if (isPressed) 0.90f else if (selected) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_item_scale",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) VestraColors.Accent else VestraColors.InkMuted,
        label = "dock_icon_color",
    )

    Column(
        modifier = modifier
            .testTag(destination.testTag)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = destination.label }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = iconColor,
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(VestraColors.Accent),
            )
        }
    }
}

@Composable
private fun CreateFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "center_fab_scale",
    )

    Box(
        modifier = modifier
            .width(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .testTag(BottomBarDestination.CREATE.testTag)
                .size(52.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(VestraColors.AccentSoft, VestraColors.Accent, VestraColors.SaffronDeep),
                    ),
                )
                .border(2.dp, VestraColors.Ivory.copy(alpha = 0.5f), CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { contentDescription = BottomBarDestination.CREATE.label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
