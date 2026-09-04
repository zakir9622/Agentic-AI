package com.zakir.vestra.ui.screens.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.WarningAmber
import com.zakir.vestra.shared.chat.ChatMessage
import com.zakir.vestra.shared.chat.ContextBudget
import com.zakir.vestra.shared.news.NewsItem
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.components.CodeBlock
import com.zakir.vestra.ui.components.GlassSnackbar
import com.zakir.vestra.ui.components.SnackbarLevel
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.VestraColors
import com.zakir.vestra.ui.components.MarkdownText
import com.zakir.vestra.ui.util.rememberSpeaker
import com.zakir.vestra.ui.util.shareText
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One turn in the thread.
 *
 * The two sides are deliberately asymmetric, matching the reference app: a **user** turn is a
 * short right-aligned pill, because it is usually one line and the user already knows what they
 * typed; an **assistant** turn has no bubble at all. It is the page.
 *
 * Dropping the assistant bubble is not cosmetic. A reply is the longest content the app renders
 * and the bubble was costing it 28dp of horizontal padding plus a 32dp avatar gutter, on a
 * surface that is already glass over an aurora background. Bare text on the canvas reads at the
 * width the prose was written for, and the model name plus the action row give the turn its
 * identity instead of a border does.
 *
 * The body renders through [MarkdownText]. Before that, a completely ordinary reply arrived on
 * screen as `- **Fashion try-on** features and tips` — markers intact — which was the app's most
 * visible defect because it was on the first reply of every conversation. Fenced code still
 * splits out to [CodeBlock] first, via [MessageSegment].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun ChatMessageBubble(
    message: ChatMessage,
    index: Int,
    modifier: Modifier = Modifier,
    modelDisplayName: String? = null,
    /** Re-runs the last exchange. Only supplied for the newest assistant turn. */
    onRegenerate: (() -> Unit)? = null,
    /** Puts this user turn back in the composer for editing. Newest user turn only. */
    onEdit: ((String) -> Unit)? = null,
    /** Removes this turn from the thread. Offered in the long-press menu. */
    onDelete: (() -> Unit)? = null,
) {
    val isUser = message.role.equals("user", ignoreCase = true)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val speaker = rememberSpeaker()
    val haptics = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }

    val formattedTime = remember(message.timestampMs) {
        if (message.timestampMs > 0) {
            runCatching { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestampMs)) }
                .getOrDefault("")
        } else {
            ""
        }
    }

    val segments = remember(message.text) { MessageSegment.split(message.text.ifBlank { "…" }) }

    if (isUser) {
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                // Uniform, no tail. A tail is a speech-bubble convention and this thread has
                // only one bubble in it — the assistant side is bare text — so the tail pointed
                // at nothing and turned a two-character "hi" into a lopsided blob.
                shape = RoundedCornerShape(RadiusTokens.lg),
                color = VestraColors.Accent,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .testTag(TestTags.chatMessageBubble(index, message.role)),
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                )
            }
            // Edit sits outside the bubble rather than inside it: a pill sized to two characters
            // has no room for a control, and putting one there would set the bubble's minimum
            // width for every short message in the thread.
            if (onEdit != null) {
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = { onEdit(message.text) },
                    modifier = Modifier.size(30.dp).testTag(TestTags.messageEdit(index)),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit and re-send this message",
                        modifier = Modifier.size(15.dp),
                        tint = VestraColors.InkMuted,
                    )
                }
            }
        }
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.chatMessageBubble(index, message.role))
            // Long-press is the platform gesture for "what else can I do with this", and it is
            // where select-text and delete belong: neither earns a permanent icon in the action
            // row, but both are unreachable without it.
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                },
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        if (showMenu) {
            MessageActionSheet(
                canDelete = onDelete != null,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(message.text))
                    GlassSnackbar.show("Copied to clipboard", SnackbarLevel.SUCCESS)
                },
                onShare = { shareText(context, message.text) },
                onDelete = { onDelete?.invoke() },
                onDismiss = { showMenu = false },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(VestraColors.Accent, VestraColors.SaffronDeep),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Assistant",
                    modifier = Modifier.size(11.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = modelDisplayName ?: (message.providerId ?: "Lookbook AI"),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (formattedTime.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = VestraColors.InkMuted.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        segments.forEachIndexed { segmentIndex, segment ->
            if (segmentIndex > 0) Spacer(Modifier.height(10.dp))
            when (segment) {
                is MessageSegment.Prose -> MarkdownText(
                    text = segment.text,
                    color = VestraColors.Ink,
                    modifier = Modifier.fillMaxWidth(),
                )
                is MessageSegment.Code -> CodeBlock(
                    code = segment.code,
                    language = segment.language,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReplyAction(
                icon = if (copied) Icons.Outlined.Done else Icons.Outlined.ContentCopy,
                label = "Copy reply",
                tint = if (copied) VestraColors.Accent else VestraColors.InkMuted,
                testTag = TestTags.messageAction("copy", index),
            ) {
                clipboardManager.setText(AnnotatedString(message.text))
                copied = true
                GlassSnackbar.show("Copied to clipboard", SnackbarLevel.SUCCESS)
            }
            if (onRegenerate != null) {
                ReplyAction(
                    icon = Icons.Outlined.Refresh,
                    label = "Regenerate reply",
                    testTag = TestTags.messageAction("regenerate", index),
                    onClick = onRegenerate,
                )
            }
            ReplyAction(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                label = "Read reply aloud",
                testTag = TestTags.messageAction("speak", index),
            ) { speaker.toggle(message.text) }
            ReplyAction(
                icon = Icons.Outlined.Share,
                label = "Share reply",
                testTag = TestTags.messageAction("share", index),
            ) { shareText(context, message.text) }
        }
    }
}

/** One icon in an assistant turn's action row. */
@Composable
private fun ReplyAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    tint: Color = VestraColors.InkMuted,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp).testTag(testTag)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = tint)
    }
}

/**
 * One run of a message: prose, or a fenced code block.
 *
 * Splitting is deliberately literal — it looks for ``` fences and nothing else. A message with no
 * fences yields exactly one [Prose] segment, so the common case costs one allocation and behaves
 * exactly as before. An unterminated fence takes the rest of the message as code rather than
 * silently dropping it.
 */
sealed interface MessageSegment {
    data class Prose(val text: String) : MessageSegment
    data class Code(val code: String, val language: String?) : MessageSegment

    companion object {
        fun split(text: String): List<MessageSegment> {
            if (!text.contains("```")) return listOf(Prose(text))
            val out = mutableListOf<MessageSegment>()
            var i = 0
            while (i < text.length) {
                val open = text.indexOf("```", i)
                if (open == -1) {
                    text.substring(i).takeIf { it.isNotBlank() }?.let { out += Prose(it.trim()) }
                    break
                }
                text.substring(i, open).takeIf { it.isNotBlank() }?.let { out += Prose(it.trim()) }
                val langEnd = text.indexOf('\n', open + 3).let { if (it == -1) text.length else it }
                val language = text.substring(open + 3, langEnd).trim().takeIf { it.isNotBlank() }
                val close = text.indexOf("```", langEnd)
                val codeEnd = if (close == -1) text.length else close
                val code = text.substring(langEnd, codeEnd).trim('\n')
                if (code.isNotBlank()) out += Code(code, language)
                i = if (close == -1) text.length else close + 3
            }
            return out.ifEmpty { listOf(Prose(text)) }
        }
    }
}

/** Animated pulsating typing indicator shown while a chat reply is generating. */
@Composable
fun ChatTypingIndicator(
    modifier: Modifier = Modifier,
    modelLabel: String = "Lookbook Assistant",
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot1",
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot2",
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot3",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(TestTags.CHAT_TYPING_INDICATOR),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp, bottom = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(VestraColors.Accent.copy(alpha = 0.25f), VestraColors.SaffronDeep.copy(alpha = 0.4f)),
                    ),
                )
                .border(1.dp, VestraColors.Accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = VestraColors.Accent,
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = RadiusTokens.lg,
                topEnd = RadiusTokens.lg,
                bottomStart = 4.dp,
                bottomEnd = RadiusTokens.lg,
            ),
            color = VestraColors.GlassFill,
            modifier = Modifier.border(
                1.dp,
                VestraColors.GlassBorder.copy(alpha = 0.5f),
                RoundedCornerShape(
                    topStart = RadiusTokens.lg,
                    topEnd = RadiusTokens.lg,
                    bottomStart = 4.dp,
                    bottomEnd = RadiusTokens.lg,
                ),
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "$modelLabel is thinking…",
                    style = MaterialTheme.typography.labelSmall,
                    color = VestraColors.Accent,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).scale(dot1Scale).clip(CircleShape).background(VestraColors.Accent))
                    Box(Modifier.size(8.dp).scale(dot2Scale).clip(CircleShape).background(VestraColors.AccentSoft))
                    Box(Modifier.size(8.dp).scale(dot3Scale).clip(CircleShape).background(VestraColors.AccentGlow))
                }
            }
        }
    }
}

/** Empty-state shown before any chat message exists, with tap-to-start conversation starters. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatEmptyState(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val starterPrompts = listOf(
        "Discuss modest winter layering & fabric textures" to "🧥 Styling",
        "Compare on-device local models vs cloud" to "⚡ Performance",
        "Suggest tailored silhouettes for contemporary fashion" to "📐 Silhouettes",
        "Synthesize runway trends for modest apparel" to "✨ Editorial",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag(TestTags.CHAT_EMPTY_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(VestraColors.Accent.copy(alpha = 0.25f), Color.Transparent)))
                .border(1.dp, VestraColors.Accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = VestraColors.Accent,
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Atelier Intelligence",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = VestraColors.Ink,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Ask questions about modest fashion, runway aesthetics, on-device reasoning, or tap a headline above.",
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "CONVERSATION STARTERS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
            color = VestraColors.InkMuted,
        )

        Spacer(Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            starterPrompts.forEachIndexed { index, (prompt, tag) ->
                Surface(
                    shape = RoundedCornerShape(RadiusTokens.md),
                    color = VestraColors.GlassFill,
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusTokens.md))
                        .border(1.dp, VestraColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(RadiusTokens.md))
                        .clickable { onPromptSelected(prompt) }
                        .testTag(TestTags.chatStarterPrompt(index)),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = VestraColors.Accent,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = VestraColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Live "used / window" token estimate for the composer, shown above [PromptComposer][
 * com.zakir.vestra.ui.components.PromptComposer] — Part B.2, ported from lookbookweb's
 * `src/lib/tokens.ts` live-budget line. Renders nothing when [budget] is comfortably under
 * budget and the draft is empty, so it doesn't clutter the empty-composer state; switches to a
 * warning row once a send would actually truncate.
 */
@Composable
fun ContextBudgetBar(
    budget: ContextBudget.Budget,
    hasDraft: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = TestTags.CONTEXT_BUDGET_BAR,
) {
    if (!hasDraft && !budget.willTruncate) return
    val warnColor = VestraColors.Danger
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = if (budget.willTruncate) 6.dp else 2.dp)
            .testTag(testTag),
        horizontalArrangement = if (budget.willTruncate) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (budget.willTruncate) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = warnColor,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "This message won't fit the model's context window and will be truncated.",
                style = MaterialTheme.typography.labelSmall,
                color = warnColor,
            )
        } else {
            val label = if (budget.isKnownWindow) {
                "${budget.usedTokens} / ${budget.windowTokens} tokens"
            } else {
                "${budget.usedTokens} tokens (window unknown for this model)"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = VestraColors.InkMuted,
            )
        }
    }
}

/**
 * "Remembering N things" header pill — the local analog of lookbookweb's chat-header memory
 * indicator (`Brain` icon there; `Psychology` is the closest Material Symbols intent match).
 * Hidden entirely at zero facts rather than showing a "Remembering 0 things" pill with no
 * informational value — never fabricates a nonzero count.
 */
@Composable
fun MemoryPill(factCount: Int, modifier: Modifier = Modifier) {
    if (factCount <= 0) return
    com.zakir.vestra.ui.components.GlassPill(
        text = if (factCount == 1) "Remembering 1 thing" else "Remembering $factCount things",
        active = true,
        modifier = modifier.testTag(TestTags.CHAT_MEMORY_PILL),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = VestraColors.Accent,
            )
        },
    )
}

/** Collapsible top strip of live headlines, tap any card to seed a chat discussion. */
@Composable
fun NewsHeadlinesBar(
    newsItems: List<NewsItem>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onHeadlineClick: (NewsItem, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Newspaper,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = VestraColors.Accent,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "LIVE HEADLINES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = VestraColors.Ink,
                )
                if (newsItems.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VestraColors.Accent.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${newsItems.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = VestraColors.Accent,
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse headlines" else "Expand headlines",
                    modifier = Modifier.size(18.dp),
                    tint = VestraColors.InkMuted,
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !refreshing,
                modifier = Modifier.size(28.dp).testTag(TestTags.CHAT_REFRESH_BUTTON),
            ) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VestraColors.Accent)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh headlines",
                        modifier = Modifier.size(16.dp),
                        tint = VestraColors.Accent,
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            if (newsItems.isEmpty()) {
                if (refreshing) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                        Text(
                            "Loading latest fashion & AI dispatches…",
                            style = MaterialTheme.typography.bodySmall,
                            color = VestraColors.InkMuted,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    newsItems.take(8).forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(RadiusTokens.md),
                            color = VestraColors.GlassFill,
                            modifier = Modifier
                                .widthIn(min = 180.dp, max = 260.dp)
                                .clip(RoundedCornerShape(RadiusTokens.md))
                                .border(1.dp, VestraColors.GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(RadiusTokens.md))
                                .clickable { onHeadlineClick(item, index) }
                                .testTag(TestTags.chatHeadlineCard(index)),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.source.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = VestraColors.Accent,
                                    )
                                    Text(
                                        text = "Tap to discuss",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = VestraColors.InkMuted,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, lineHeight = 16.sp),
                                    color = VestraColors.Ink,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The long-press menu for one turn.
 *
 * Deliberately short. Copy and Share duplicate the action row on purpose — a long-press is a
 * discovery path, and a menu that omits the obvious options reads as broken. Delete is the one
 * thing that exists *only* here, because a permanent delete icon beside every reply is an invitation
 * to lose work by mis-tap.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionSheet(
    canDelete: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VestraColors.SurfaceRaised,
        // Its own window, its own composition root — MainActivity's opt-in does not reach here.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(TestTags.MESSAGE_ACTION_SHEET)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            MessageActionRow(Icons.Outlined.ContentCopy, "Copy text", TestTags.MESSAGE_MENU_COPY) {
                onCopy(); onDismiss()
            }
            MessageActionRow(Icons.Outlined.Share, "Share", TestTags.MESSAGE_MENU_SHARE) {
                onShare(); onDismiss()
            }
            if (canDelete) {
                MessageActionRow(
                    Icons.Outlined.DeleteOutline,
                    "Delete this message",
                    TestTags.MESSAGE_MENU_DELETE,
                    tint = VestraColors.Danger,
                ) { onDelete(); onDismiss() }
            }
        }
    }
}

@Composable
private fun MessageActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    tint: Color = VestraColors.Ink,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(RoundedCornerShape(RadiusTokens.lg))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}
