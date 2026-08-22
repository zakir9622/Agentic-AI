package com.zakir.vestra.ui.screens.studio

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.media.MediaExport
import com.zakir.vestra.shared.cloud.AiCapability
import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.domain.PackStatus
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.components.AtelierHero
import com.zakir.vestra.ui.components.AtelierRail
import com.zakir.vestra.ui.components.AtelierTab
import com.zakir.vestra.ui.components.GlassCard
import com.zakir.vestra.ui.components.GlassSectionLabel
import com.zakir.vestra.ui.components.SpatialBackground
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.rememberReduceMotion
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudioScreen(
    appSettings: AppSettings,
    wardrobe: WardrobeRepository,
    packManager: ModelPackManager,
    onNewLook: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenCode: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPacks: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val context = LocalContext.current
    val recent by wardrobe.entries.collectAsState()
    val packStates by packManager.states.collectAsState()
    LaunchedEffect(Unit) {
        packManager.refresh()
    }
    val proReady = listOf("pro-v2-int8", "pro-v1").any { id ->
        packStates[id]?.isReady() == true
    }

    var online by remember { mutableStateOf(appSettings.networkLikelyAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            online = appSettings.networkLikelyAvailable()
            delay(2_500)
        }
    }

    var appeared by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(Unit) { appeared = true }
    val fade by animateFloatAsState(
        targetValue = if (appeared || reduceMotion) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(640),
        label = "studioFade",
    )
    val heroLift by animateFloatAsState(
        targetValue = if (appeared || reduceMotion) 0f else 18f,
        animationSpec = if (reduceMotion) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroLift",
    )

    val tryOnModel = appSettings.selectedProvider(AiCapability.TRY_ON).displayName
    val hfToken by appSettings.hfToken.collectAsState()
    val hfReady = !hfToken.isNullOrBlank()
    val liteReady = packStates["lite-v1"]?.isReady() == true
    val liteState = packStates["lite-v1"]
    val statusLine = buildString {
        append(
            when {
                proReady -> "Pro verified"
                liteReady -> "Lite verified"
                liteState?.status == PackStatus.INSTALLED -> "Lite verifying…"
                else -> "Lite needs download"
            },
        )
        append("  ·  ")
        append(if (hfReady) "Cloud token set" else "Cloud needs HF token")
        append("  ·  ")
        append(if (online) "Online" else "Offline")
        append("  ·  ")
        append(tryOnModel)
    }

    SpatialBackground {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 18.dp)
                    .alpha(fade),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item(key = "chrome") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                LookbookCopy.PRODUCT_NAME,
                                style = MaterialTheme.typography.titleLarge,
                                color = VestraColors.Ink,
                            )
                            Text(
                                LookbookCopy.STUDIO_HOME,
                                style = MaterialTheme.typography.labelMedium,
                                color = VestraColors.Accent,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(VestraColors.SaffronDeep, VestraColors.Accent),
                                        ),
                                    )
                                    .clickable(onClick = onOpenPacks)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (proReady) "Pro" else "Lite",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                            }
                            IconButton(onClick = onOpenHelp) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = LookbookCopy.STUDIO_HELP,
                                    tint = VestraColors.Ink,
                                )
                            }
                            IconButton(onClick = onOpenWardrobe) {
                                Icon(
                                    Icons.Outlined.Checkroom,
                                    contentDescription = LookbookCopy.STUDIO_WARDROBE,
                                    tint = VestraColors.Ink,
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(VestraColors.GlassFillStrong)
                                        .border(1.5.dp, VestraColors.Accent.copy(alpha = 0.7f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = LookbookCopy.STUDIO_SETTINGS,
                                        tint = VestraColors.Accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item(key = "do-label") {
                    GlassSectionLabel("WHAT WOULD YOU LIKE TO DO")
                    Text(
                        "Pick a studio — same chat composer, searchable models inside the prompt bar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VestraColors.InkMuted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                item(key = "action-list") {
                    CapabilityTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Image,
                        title = LookbookCopy.STUDIO_IMAGE,
                        body = "Generate or edit stills · pick models in chat",
                        accent = VestraColors.Accent,
                        onClick = onOpenCreate,
                        compact = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    CapabilityTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Videocam,
                        title = LookbookCopy.STUDIO_VIDEO,
                        body = "Free cloud clips · model search in composer",
                        accent = VestraColors.SaffronDeep,
                        onClick = onOpenVideo,
                        compact = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    CapabilityTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Code,
                        title = LookbookCopy.STUDIO_CODE,
                        body = "Groq · Hugging Face · OpenRouter",
                        accent = VestraColors.AccentSoft,
                        onClick = onOpenCode,
                        compact = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    CapabilityTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Insights,
                        title = LookbookCopy.STUDIO_USAGE,
                        body = "Requests · tokens · failure notes",
                        accent = VestraColors.Accent,
                        onClick = onOpenUsage,
                        compact = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    CapabilityTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = LookbookCopy.STUDIO_HELP,
                        body = "Guides · permissions · recovery",
                        accent = VestraColors.SaffronDeep,
                        onClick = onOpenHelp,
                        compact = true,
                    )
                    Spacer(Modifier.height(22.dp))
                }

                item(key = "hero") {
                    GlassSectionLabel("CORE TRY-ON")
                    Box(Modifier.padding(bottom = heroLift.dp)) {
                        AtelierHero(
                            brand = LookbookCopy.PRODUCT_NAME,
                            headline = "Cast the look",
                            support = "Abaya, hijab, and shalwar on-device with Lite or Pro — center of the atelier.",
                            cta = LookbookCopy.ACTION_START_TRY_ON,
                            onCta = onNewLook,
                            statusLine = statusLine,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                if (!proReady) {
                    item(key = "pro-cta") {
                        Spacer(Modifier.height(14.dp))
                        GlassCard(onClick = onOpenPacks) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconWell(Icons.Outlined.Cloud)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Install Pro pack", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "One download. Fully offline after. Free cloud try-on stays in Settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "recent-header") {
                    Spacer(Modifier.height(22.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                GlassSectionLabel("RECENT LOOKS")
                        if (recent.isNotEmpty()) {
                            Text(
                                "Open gallery",
                                style = MaterialTheme.typography.labelMedium,
                                color = VestraColors.Accent,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clickable(onClick = onOpenWardrobe),
                            )
                        }
                    }
                }

                item(key = "recent") {
                    if (recent.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recent.take(12), key = { it.id }) { entry ->
                                val file = File(entry.imagePath)
                                Box(
                                    Modifier
                                        .width(148.dp)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(
                                                    VestraColors.GlassHighlight,
                                                    VestraColors.Accent.copy(alpha = 0.35f),
                                                ),
                                            ),
                                            RoundedCornerShape(24.dp),
                                        )
                                        .combinedClickable(
                                            onClick = onOpenWardrobe,
                                            onLongClick = {
                                                if (file.exists()) {
                                                    MediaExport.share(context, file, "Share look")
                                                }
                                            },
                                        ),
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Recent look ${entry.personLabel}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        VestraColors.AtelierCanvas.copy(alpha = 0.8f),
                                                    ),
                                                ),
                                            ),
                                    )
                                    Text(
                                        entry.personLabel.ifBlank { "Look" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VestraColors.Ivory,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 12.dp, end = 44.dp, bottom = 12.dp),
                                    )
                                    IconButton(
                                        onClick = {
                                            if (file.exists()) {
                                                MediaExport.share(context, file, LookbookCopy.ACTION_SHARE)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .semantics {
                                                contentDescription = LookbookCopy.ACTION_SHARE
                                            },
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = null,
                                            tint = VestraColors.Ivory,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(VestraColors.GlassFill)
                                .border(1.dp, VestraColors.GlassBorder, RoundedCornerShape(24.dp))
                                .clickable(onClick = onNewLook),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Tap to cast your first look",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            AtelierRail(
                selected = AtelierTab.HOME,
                onSelect = { tab ->
                    when (tab) {
                        AtelierTab.HOME -> Unit
                        AtelierTab.CREATE -> onOpenCreate()
                        AtelierTab.TRY_ON -> onNewLook()
                        AtelierTab.VIDEO -> onOpenVideo()
                        AtelierTab.SETTINGS -> onOpenSettings()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun IconWell(icon: ImageVector) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = VestraColors.Accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CapabilityTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "capScale",
    )
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .scale(scale)
            .clip(shape)
            .background(VestraColors.GlassFill)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(VestraColors.GlassHighlight, accent.copy(alpha = 0.45f))),
                shape,
            )
            .semantics { contentDescription = "$title. $body" }
            .clickable {
                pressed = true
                onClick()
            }
            .padding(if (compact) 16.dp else 18.dp),
    ) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWell(icon)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = VestraColors.InkMuted)
                }
            }
        } else {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodySmall, color = VestraColors.InkMuted)
            Spacer(Modifier.height(8.dp))
        }
    }
}
