package com.zakir.vestra.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Chrome shared by every isolated per-modality screen (Image/Video/Audio/Code Studio, News &
 * Chat): a back arrow to Home plus the screen's title, since none of these get a bottom-dock
 * slot of their own any more — Home's tool grid is the only way in, so each needs its own way
 * back out.
 */
@Composable
fun IsolatedStudioScreen(
    title: String,
    onBack: () -> Unit,
    accent: Color = VestraColors.Accent,
    content: @Composable ColumnScope.() -> Unit,
) {
    SpatialBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.section, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = VestraColors.Ink,
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
            content()
        }
    }
}
