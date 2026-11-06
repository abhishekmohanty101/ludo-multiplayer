package com.ludo.game.ui.components.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludo.game.data.model.ChatMessage
import com.ludo.game.data.model.ChatMessageType
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.data.model.QuickReactions
import com.ludo.game.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * In-game chat panel that can be expanded/collapsed.
 */
@Composable
fun GameChatPanel(
    messages: List<ChatMessage>,
    currentUserId: String,
    onSendMessage: (String) -> Unit,
    onSendReaction: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
            // Header with toggle
            ChatHeader(
                messageCount = messages.size,
                isExpanded = isExpanded,
                onToggle = onToggleExpanded
            )
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // Messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(messages) { message ->
                            ChatMessageItem(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Quick reactions
                    QuickReactionsBar(
                        onReactionClick = onSendReaction
                    )
                    
                    Divider()
                    
                    // Message input
                    ChatInput(
                        onSendMessage = onSendMessage
                    )
                }
            }
        }
    }
}

/**
 * Chat panel header with toggle button.
 */
@Composable
private fun ChatHeader(
    messageCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Chat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        if (messageCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Badge {
                Text(text = messageCount.toString())
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
        )
    }
}

/**
 * Single chat message item.
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    val playerColor = try {
        PlayerColor.valueOf(message.senderColor)
    } catch (e: Exception) {
        PlayerColor.BLUE
    }
    
    val color = when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    when (message.messageType) {
        ChatMessageType.SYSTEM -> {
            SystemMessage(message = message.message)
        }
        ChatMessageType.GAME_EVENT -> {
            GameEventMessage(
                message = message.message,
                playerName = message.senderName,
                playerAvatar = message.senderAvatar,
                color = color
            )
        }
        ChatMessageType.EMOJI -> {
            EmojiMessage(
                emoji = message.message,
                senderName = message.senderName,
                senderAvatar = message.senderAvatar,
                color = color,
                isOwnMessage = isOwnMessage
            )
        }
        ChatMessageType.TEXT -> {
            TextMessage(
                message = message,
                color = color,
                isOwnMessage = isOwnMessage
            )
        }
    }
}

/**
 * Regular text message bubble.
 */
@Composable
private fun TextMessage(
    message: ChatMessage,
    color: Color,
    isOwnMessage: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            Text(
                text = message.senderAvatar,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            if (!isOwnMessage) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Surface(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isOwnMessage) 12.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 12.dp
                ),
                color = if (isOwnMessage) color.copy(alpha = 0.2f) 
                       else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = message.message,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Emoji reaction message.
 */
@Composable
private fun EmojiMessage(
    emoji: String,
    senderName: String,
    senderAvatar: String,
    color: Color,
    isOwnMessage: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isOwnMessage) {
            Text(
                text = senderAvatar,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        Text(
            text = emoji,
            fontSize = 32.sp
        )
        
        if (isOwnMessage) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "You",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * System notification message.
 */
@Composable
private fun SystemMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center
    )
}

/**
 * Game event message (capture, roll 6, etc.)
 */
@Composable
private fun GameEventMessage(
    message: String,
    playerName: String,
    playerAvatar: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = playerAvatar, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$playerName $message",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Quick reaction emoji bar.
 */
@Composable
private fun QuickReactionsBar(
    onReactionClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(QuickReactions.reactions) { emoji ->
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onReactionClick(emoji) },
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}

/**
 * Chat message input field.
 */
@Composable
private fun ChatInput(
    onSendMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (text.isNotBlank()) {
                        onSendMessage(text.trim())
                        text = ""
                    }
                }
            ),
            shape = RoundedCornerShape(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSendMessage(text.trim())
                    text = ""
                }
            },
            enabled = text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary 
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Floating chat bubble for collapsed state.
 */
@Composable
fun FloatingChatBubble(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Open Chat"
            )
        }
        
        if (unreadCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
            }
        }
    }
}
