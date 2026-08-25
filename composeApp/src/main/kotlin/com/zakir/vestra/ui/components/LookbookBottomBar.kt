package com.zakir.vestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.VestraColors

enum class BottomBarDestination(val label: String, val testTag: String) {
    HOME("Home", TestTags.BOTTOM_BAR_HOME),
    LIBRARY("Library", TestTags.BOTTOM_BAR_LIBRARY),
    CREATE("Create", TestTags.BOTTOM_BAR_CREATE),
    CHAT("Chat", TestTags.BOTTOM_BAR_CHAT),
    SETTINGS("Settings", TestTags.BOTTOM_BAR_SETTINGS),
}

/**
 * Lookbookweb-parity bottom dock: Home / Library / a raised center Create FAB / Chat / Settings.
 * The in-studio pager (Image/Video/Audio/Code) is a separate, lower level of navigation nested
 * inside the Home destination — this bar only moves between top-level destinations.
 */
@Composable
fun LookbookBottomBar(
    selected: BottomBarDestination?,
    onSelect: (BottomBarDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag(TestTags.BOTTOM_BAR)
            .background(VestraColors.GlassFillStrong)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(VestraColors.GlassBorder, Color.Transparent),
                ),
                shape = androidx.compose.ui.graphics.RectangleShape,
            )
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarItem(
                destination = BottomBarDestination.HOME,
                icon = Icons.Outlined.Home,
                selected = selected == BottomBarDestination.HOME,
                onClick = { onSelect(BottomBarDestination.HOME) },
            )
            BottomBarItem(
                destination = BottomBarDestination.LIBRARY,
                icon = Icons.Outlined.Checkroom,
                selected = selected == BottomBarDestination.LIBRARY,
                onClick = { onSelect(BottomBarDestination.LIBRARY) },
            )
            CreateFab(onClick = { onSelect(BottomBarDestination.CREATE) })
            BottomBarItem(
                destination = BottomBarDestination.CHAT,
                icon = Icons.AutoMirrored.Outlined.Chat,
                selected = selected == BottomBarDestination.CHAT,
                onClick = { onSelect(BottomBarDestination.CHAT) },
            )
            BottomBarItem(
                destination = BottomBarDestination.SETTINGS,
                icon = Icons.Outlined.Settings,
                selected = selected == BottomBarDestination.SETTINGS,
                onClick = { onSelect(BottomBarDestination.SETTINGS) },
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: BottomBarDestination,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) VestraColors.Accent else VestraColors.InkMuted
    Column(
        Modifier
            .wrapContentWidth()
            .testTag(destination.testTag)
            .clickable(onClick = onClick)
            .semantics { contentDescription = destination.label }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

@Composable
private fun CreateFab(onClick: () -> Unit) {
    Box(
        Modifier
            .width(56.dp)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .testTag(BottomBarDestination.CREATE.testTag)
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(VestraColors.SaffronDeep, VestraColors.Accent, VestraColors.AccentSoft),
                    ),
                    shape = CircleShape,
                )
                .clickable(onClick = onClick)
                .semantics { contentDescription = "Create" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}
