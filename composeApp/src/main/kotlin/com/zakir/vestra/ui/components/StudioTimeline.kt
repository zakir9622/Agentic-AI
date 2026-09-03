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
 * is still null (the brief moment before the engine has emitted its first progress update),
 * otherwise [ResultPane] itself — reusing its existing per-[com.zakir.vestra.shared.cloud.GenerativeState]-subtype
 * rendering (image/video/audio/code/transcript/failure, and — since [GenerativeViewModel.updateLastTurn]
 * no longer filters it — the live `Running` stage/progress card too) rather than duplicating it.
 *
 * Retry is offered on every turn that carries one: [ResultPane] only surfaces it on a
 * [com.zakir.vestra.shared.cloud.GenerativeState.Failed] result, and a failure is worth re-running
 * whether or not something newer has been sent since. Dismiss stays [isLatest]-only because it
 * pops the most recent turn by definition.
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
                onRetry = onRetry,
                onDismiss = if (isLatest) onDismiss else null,
                retryLabel = retryLabel,
                accent = accent,
            )
        }
    }
}
