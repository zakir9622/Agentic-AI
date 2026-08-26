package com.zakir.vestra.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.chat.ChatMessage
import com.zakir.vestra.ui.GenerativeViewModel
import com.zakir.vestra.ui.screens.news.ChatMessageBubble
import com.zakir.vestra.ui.screens.news.ChatTypingIndicator

/**
 * One prompt→result exchange in a studio's conversation timeline (3.1.6) — the user's prompt
 * rendered with [ChatMessageBubble] (the exact same bubble News/Chat uses, reused rather than
 * reinvented), followed by the result: a [ChatTypingIndicator] while [GenerativeViewModel.StudioTurn.result]
 * is still null (the generation hasn't produced any content yet), otherwise [ResultPane] itself —
 * reusing its existing per-[com.zakir.vestra.shared.cloud.GenerativeState]-subtype rendering
 * (image/video/audio/code/transcript/failure) rather than duplicating it.
 *
 * Retry/dismiss only make sense for the turn actively in flight — passed through as-is for
 * [isLatest], forced to `null` for every older turn so history stays view-only.
 */
@Composable
fun StudioTurnBubble(
    turn: GenerativeViewModel.StudioTurn,
    index: Int,
    isLatest: Boolean,
    accent: Color,
    generationStartedAtMs: Long?,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    retryLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ChatMessageBubble(
            message = ChatMessage(
                id = turn.id,
                role = "user",
                text = turn.prompt.ifBlank { "…" },
                timestampMs = turn.timestampMs,
            ),
            index = index,
        )
        val result = turn.result
        if (result == null) {
            ChatTypingIndicator(modelLabel = "")
        } else {
            Spacer(Modifier.height(2.dp))
            ResultPane(
                state = result,
                generationStartedAtMs = if (isLatest) generationStartedAtMs else null,
                onRetry = if (isLatest) onRetry else null,
                onDismiss = if (isLatest) onDismiss else null,
                retryLabel = retryLabel,
                accent = accent,
            )
        }
    }
}
