package com.ludo.game.ui.screens.lobby

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ludo.game.data.model.Player
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.util.RoomShareManager

/**
 * Waiting room screen shown after creating/joining a room.
 * Displays room code, share options, and player list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingRoomScreen(
    viewModel: LobbyViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onGameStart: (String) -> Unit // Navigate to game with roomId
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Navigate to game when started
    LaunchedEffect(uiState.isGameStarted) {
        if (uiState.isGameStarted) {
            uiState.room?.roomId?.let { onGameStart(it) }
        }
    }
    
    // Show error toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waiting Room") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.leaveRoom()
                        onBackClick()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Leave")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.room == null) {
            // Show loading while creating/joining room
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Setting up room...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Room Code Card
                RoomCodeCard(
                    roomCode = uiState.roomCode,
                    onCopyClick = {
                        RoomShareManager.copyToClipboard(context, uiState.roomCode)
                        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                    },
                    onShareClick = {
                        RoomShareManager.shareRoomInvite(
                            context = context,
                            roomCode = uiState.roomCode,
                            hostName = uiState.room?.hostName ?: "Player"
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Players List
                Text(
                    text = "Players (${uiState.room?.players?.size ?: 0}/4)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                PlayersList(
                    players = uiState.room?.players?.values?.toList() ?: emptyList(),
                    hostId = uiState.room?.hostId ?: "",
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Start Game Button (Host Only)
                uiState.room?.let { room ->
                    if (uiState.isHost) {
                        Button(
                            onClick = { viewModel.startGame() },
                            enabled = room.canStart() && !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (room.canStart()) "Start Game" else "Waiting for players...",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Waiting for host to start the game...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Room code display card with copy and share buttons.
 */
@Composable
fun RoomCodeCard(
    roomCode: String,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Room Code",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Large Room Code Display
            if (roomCode.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    roomCode.forEach { char ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    enabled = roomCode.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }
                
                Button(
                    onClick = onShareClick,
                    enabled = roomCode.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

/**
 * List of players in the room.
 */
@Composable
fun PlayersList(
    players: List<Player>,
    hostId: String,
    modifier: Modifier = Modifier
) {
    val emptySlots = 4 - players.size
    
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(players) { player ->
            PlayerItem(
                player = player,
                isHost = player.id == hostId
            )
        }
        
        // Empty slots
        items(emptySlots) {
            EmptyPlayerSlot()
        }
    }
}

/**
 * Single player item in the list.
 */
@Composable
fun PlayerItem(
    player: Player,
    isHost: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when (player.color) {
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.GREEN -> Color(0xFF43A047)
        PlayerColor.YELLOW -> Color(0xFFFDD835)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isHost) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HOST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = player.color.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Online indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (player.isOnline) Color(0xFF4CAF50) else Color.Gray)
            )
        }
    }
}

/**
 * Empty player slot placeholder.
 */
@Composable
fun EmptyPlayerSlot(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Waiting for player...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
