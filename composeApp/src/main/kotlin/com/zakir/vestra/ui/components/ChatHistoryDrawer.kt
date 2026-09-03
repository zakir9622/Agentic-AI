package com.zakir.vestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.chat.ConversationSummary
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors

/**
 * The conversation list, opened from the top bar's menu button.
 *
 * This is the half of "New chat" the app shipped without. The button existed for two releases
 * with no history behind it, so it called `ChatRepository.clear()` — an unconfirmed, permanent
 * delete of the only conversation that existed. New chat *files* a conversation now, and this is
 * where it goes.
 *
 * Search is a plain title/preview filter over an in-memory list capped at 60 conversations: no
 * index, no scoring, no network. It is the honest amount of machinery for the amount of data, and
 * the field hides itself below a threshold where scanning beats typing.
 */
@Composable
fun ChatHistoryDrawer(
    conversations: List<ConversationSummary>,
    activeId: String,
    onOpen: (String) -> Unit,
    onNewChat: () -> Unit,
    onDelete: (String) -> Unit,
    /** Shares the active conversation as plain text. Null while it is empty. */
    onShareActive: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    relativeTime: (Long) -> String,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(conversations, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            conversations
        } else {
            conversations.filter {
                it.title.lowercase().contains(q) || it.preview.lowercase().contains(q)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .testTag(TestTags.CHAT_HISTORY_DRAWER)
            // Scrim: tapping outside the panel closes it, the same as the drag gesture would.
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .fillMaxSize()
                .background(VestraColors.SurfaceRaised)
                // Swallows taps so a tap on the panel itself does not reach the scrim above.
                .clickable(enabled = false) {}
                .safeDrawingPadding()
                .padding(horizontal = SpacingTokens.md),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Chats",
                    style = MaterialTheme.typography.titleMedium,
                    color = VestraColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close chat history",
                        tint = VestraColors.InkMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            DrawerAction(
                icon = Icons.Outlined.EditNote,
                label = "New chat",
                testTag = TestTags.DRAWER_NEW_CHAT,
                onClick = {
                    onNewChat()
                    onDismiss()
                },
            )
            // Whole-conversation share lives here rather than in the top bar: at 360dp that row
            // already carries a menu button, the model selector and three actions, and a sixth
            // control would take the width the model name needs.
            if (onShareActive != null) {
                Spacer(Modifier.height(SpacingTokens.xxs))
                DrawerAction(
                    icon = Icons.Outlined.IosShare,
                    label = "Share this chat",
                    testTag = TestTags.DRAWER_SHARE_CHAT,
                    onClick = {
                        onShareActive()
                        onDismiss()
                    },
                )
            }

            if (conversations.size >= SEARCH_THRESHOLD) {
                Spacer(Modifier.height(SpacingTokens.sm))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.DRAWER_SEARCH_FIELD),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = VestraColors.InkMuted)
                    },
                    placeholder = { Text("Search chats", color = VestraColors.InkMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VestraColors.Accent.copy(alpha = 0.55f),
                        unfocusedBorderColor = VestraColors.GlassBorder,
                        focusedContainerColor = VestraColors.GlassFill,
                        unfocusedContainerColor = VestraColors.GlassFill,
                    ),
                    shape = RoundedCornerShape(RadiusTokens.md),
                )
            }

            Spacer(Modifier.height(SpacingTokens.sm))

            if (filtered.isEmpty()) {
                Text(
                    if (query.isBlank()) {
                        "Conversations you start will be listed here."
                    } else {
                        "No chat matches \"${query.trim()}\"."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VestraColors.InkMuted,
                    modifier = Modifier.padding(vertical = SpacingTokens.md),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).imePadding(),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.xxs),
                ) {
                    items(filtered, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            active = conversation.id == activeId,
                            timeLabel = relativeTime(conversation.updatedAtMs),
                            onOpen = {
                                onOpen(conversation.id)
                                onDismiss()
                            },
                            onDelete = { onDelete(conversation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, testTag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(shape)
            .background(VestraColors.GlassFillStrong)
            .border(1.dp, VestraColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Icon(icon, contentDescription = null, tint = VestraColors.Accent, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = VestraColors.Ink)
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    active: Boolean,
    timeLabel: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(RadiusTokens.lg)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.conversationRow(conversation.id))
            .clip(shape)
            .background(if (active) VestraColors.Accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onOpen)
            .padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.titleSmall,
                color = VestraColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$timeLabel · ${conversation.messageCount} messages",
                style = MaterialTheme.typography.labelSmall,
                color = VestraColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .heightIn(min = 34.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .testTag(TestTags.conversationDelete(conversation.id)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Delete \"${conversation.title}\"",
                tint = VestraColors.InkMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Below this many conversations, scanning the list beats typing into a filter. */
private const val SEARCH_THRESHOLD = 6
