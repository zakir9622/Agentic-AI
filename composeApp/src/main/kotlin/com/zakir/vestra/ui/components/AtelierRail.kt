package com.zakir.vestra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.VestraColors

enum class AtelierTab {
    HOME,
    CREATE,
    TRY_ON,
    VIDEO,
    SETTINGS,
}

/**
 * Floating bottom atelier rail — one active saffron well, muted neighbors.
 * Inspired by premium AI home shells; branded for The Lookbook.
 */
@Composable
fun AtelierRail(
    selected: AtelierTab,
    onSelect: (AtelierTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .background(VestraColors.AtelierContainer.copy(alpha = 0.94f))
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            VestraColors.Accent.copy(alpha = 0.45f),
                            VestraColors.GlassBorder.copy(alpha = 0.25f),
                            VestraColors.AccentSoft.copy(alpha = 0.35f),
                        ),
                    ),
                    shape,
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RailItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                selected = selected == AtelierTab.HOME,
                onClick = { onSelect(AtelierTab.HOME) },
            )
            RailItem(
                icon = Icons.Outlined.Image,
                label = "Create",
                selected = selected == AtelierTab.CREATE,
                onClick = { onSelect(AtelierTab.CREATE) },
            )
            RailItem(
                icon = Icons.Outlined.Checkroom,
                label = "Try-on",
                selected = selected == AtelierTab.TRY_ON,
                onClick = { onSelect(AtelierTab.TRY_ON) },
                prominent = true,
            )
            RailItem(
                icon = Icons.Outlined.Videocam,
                label = "Video",
                selected = selected == AtelierTab.VIDEO,
                onClick = { onSelect(AtelierTab.VIDEO) },
            )
            RailItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                selected = selected == AtelierTab.SETTINGS,
                onClick = { onSelect(AtelierTab.SETTINGS) },
            )
        }
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    prominent: Boolean = false,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "railScale",
    )
    Box(
        Modifier
            .size(if (prominent) 52.dp else 48.dp)
            .scale(scale)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.radialGradient(listOf(VestraColors.Accent, VestraColors.SaffronDeep)),
                    )
                } else {
                    Modifier.background(Color.Transparent)
                },
            )
            .semantics {
                contentDescription = label
                role = Role.Tab
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) VestraColors.Ivory else VestraColors.IvoryMuted,
            modifier = Modifier.size(if (prominent) 24.dp else 22.dp),
        )
    }
}
