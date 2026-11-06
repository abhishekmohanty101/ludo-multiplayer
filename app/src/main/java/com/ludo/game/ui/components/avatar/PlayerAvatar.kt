package com.ludo.game.ui.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.ui.theme.*

/**
 * Available avatar options using emojis.
 */
object AvatarOptions {
    val avatars = listOf(
        "😀", "😎", "🤠", "🥳", "😈", "👻", "🤖", "👽",
        "🦊", "🐱", "🐶", "🦁", "🐯", "🐻", "🐼", "🐨",
        "🦄", "🐲", "🦅", "🦋", "🐙", "🦈", "🐬", "🦜",
        "⭐", "🌟", "💎", "🔥", "⚡", "❄️", "🌈", "🎮"
    )
    
    val defaultAvatar = "😀"
    
    fun getRandomAvatar(): String = avatars.random()
}

/**
 * Player avatar display component.
 */
@Composable
fun PlayerAvatar(
    avatar: String,
    playerColor: PlayerColor,
    size: AvatarSize = AvatarSize.MEDIUM,
    isOnline: Boolean = true,
    showOnlineIndicator: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val color = when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    val avatarSize = when (size) {
        AvatarSize.SMALL -> 32.dp
        AvatarSize.MEDIUM -> 48.dp
        AvatarSize.LARGE -> 72.dp
        AvatarSize.XLARGE -> 96.dp
    }
    
    val fontSize = when (size) {
        AvatarSize.SMALL -> 16.sp
        AvatarSize.MEDIUM -> 24.sp
        AvatarSize.LARGE -> 36.sp
        AvatarSize.XLARGE -> 48.sp
    }
    
    Box(
        modifier = modifier
            .size(avatarSize)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        // Background circle with player color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(3.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatar,
                fontSize = fontSize
            )
        }
        
        // Online indicator
        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(avatarSize * 0.3f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Avatar size options.
 */
enum class AvatarSize {
    SMALL,
    MEDIUM,
    LARGE,
    XLARGE
}

/**
 * Avatar selection dialog.
 */
@Composable
fun AvatarSelectionDialog(
    currentAvatar: String,
    playerColor: PlayerColor,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val color = when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Your Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current avatar preview
                PlayerAvatar(
                    avatar = currentAvatar,
                    playerColor = playerColor,
                    size = AvatarSize.XLARGE,
                    showOnlineIndicator = false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Avatar grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(AvatarOptions.avatars) { avatar ->
                        AvatarGridItem(
                            avatar = avatar,
                            isSelected = avatar == currentAvatar,
                            color = color,
                            onClick = { onAvatarSelected(avatar) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAvatarSelected(AvatarOptions.getRandomAvatar()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎲 Random")
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

/**
 * Single avatar item in the selection grid.
 */
@Composable
private fun AvatarGridItem(
    avatar: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) color.copy(alpha = 0.3f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = avatar,
            fontSize = 20.sp
        )
    }
}

/**
 * Player info card with avatar.
 */
@Composable
fun PlayerInfoCard(
    name: String,
    avatar: String,
    playerColor: PlayerColor,
    isCurrentTurn: Boolean,
    isOnline: Boolean,
    tokensFinished: Int,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val color = when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) color.copy(alpha = 0.15f) 
                           else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentTurn) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(
                avatar = avatar,
                playerColor = playerColor,
                size = AvatarSize.MEDIUM,
                isOnline = isOnline,
                onClick = onAvatarClick
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
                )
                
                if (isCurrentTurn) {
                    Text(
                        text = "Playing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
            
            // Tokens finished indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$tokensFinished/4",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
