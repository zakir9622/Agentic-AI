package com.zakir.vestra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.util.rememberReduceMotion
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicLong

/**
 * A7 — lookbookweb uses `sonner`, top-center, for every success/error/info/warning notice.
 * Android's native `Toast` (bottom-anchored, unstyled) can't be repositioned or restyled to
 * match, so this is the Compose replacement: a global message bus any call site — Composable or
 * plain Kotlin (e.g. [com.zakir.vestra.media.MediaExport], which has no Composable scope) — can
 * post to, plus a single top-center host mounted once at the app root ([GlassSnackbarHost] in
 * `VestraNavHost`).
 */
enum class SnackbarLevel { SUCCESS, ERROR, WARNING, INFO }

data class SnackbarRequest(val id: Long, val message: String, val level: SnackbarLevel)

object GlassSnackbar {
    private val idSequence = AtomicLong(0)

    // DROP_OLDEST — a burst of calls should never silently lose the newest message; combined
    // with the host's collectLatest, only the latest request in a burst is ever shown anyway.
    private val _messages = MutableSharedFlow<SnackbarRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<SnackbarRequest> = _messages.asSharedFlow()

    fun show(message: String, level: SnackbarLevel = SnackbarLevel.INFO) {
        _messages.tryEmit(SnackbarRequest(idSequence.incrementAndGet(), message, level))
    }
}

private const val VISIBLE_MS = 3200L

/** Mount once at the app root, positioned top-center over everything else. */
@Composable
fun GlassSnackbarHost(modifier: Modifier = Modifier) {
    // `displayed` is separate from `visible` so the exit animation still has content to animate
    // away — clearing them together would blank the card the instant the exit transition starts.
    var displayed by remember { mutableStateOf<SnackbarRequest?>(null) }
    var visible by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(Unit) {
        // collectLatest — a newer message immediately preempts and replaces whatever is still
        // showing (cancelling its delay), instead of queuing behind it for up to VISIBLE_MS.
        GlassSnackbar.messages.collectLatest { request ->
            displayed = request
            visible = true
            delay(VISIBLE_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) EnterTransition.None else slideInVertically { -it } + fadeIn(),
        exit = if (reduceMotion) ExitTransition.None else slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        displayed?.let { request -> GlassSnackbarCard(request) }
    }
}

@Composable
private fun GlassSnackbarCard(request: SnackbarRequest) {
    val (icon, accent) = when (request.level) {
        SnackbarLevel.SUCCESS -> Icons.Outlined.CheckCircle to VestraColors.Accent
        SnackbarLevel.ERROR -> Icons.Outlined.ErrorOutline to VestraColors.Danger
        SnackbarLevel.WARNING -> Icons.Outlined.WarningAmber to WarningAmberColor
        SnackbarLevel.INFO -> Icons.Outlined.Info to VestraColors.InkMuted
    }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            .widthIn(max = 420.dp)
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, accent.copy(alpha = 0.5f), shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(TestTags.GLASS_SNACKBAR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.padding(end = 10.dp))
        Text(request.message, style = MaterialTheme.typography.bodyMedium, color = VestraColors.Ink)
    }
}

// A conventional warning amber — not sourced from lookbookweb (its exact `sonner` warning hex
// wasn't captured in this session's research), chosen only to be visually distinct from the
// error red and this app's accent, matching the universal warning-color convention.
private val WarningAmberColor = Color(0xFFE8A23D)
