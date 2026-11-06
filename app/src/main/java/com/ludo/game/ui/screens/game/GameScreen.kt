package com.ludo.game.ui.screens.game

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ludo.game.data.model.Player
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.ui.components.board.LudoBoard
import com.ludo.game.ui.components.board.TokenPosition
import com.ludo.game.ui.components.dice.DiceWithControls
import com.ludo.game.ui.theme.*

/**
 * Main game screen showing the Ludo board and game controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onGameEnd: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Show error toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    // Show winner dialog
    if (uiState.winner != null) {
        WinnerDialog(
            winner = uiState.winner!!,
            isMe = uiState.winner!!.id == uiState.currentUserId,
            onDismiss = onGameEnd
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ludo Game") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Player info bar
            PlayersInfoBar(
                players = uiState.players.values.toList(),
                currentTurnPlayerId = uiState.gameState?.currentTurnPlayerId ?: "",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Loading state
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Game board
                LudoBoard(
                    tokens = uiState.tokenPositions,
                    currentPlayer = viewModel.getMyColor(),
                    selectableTokens = uiState.selectableTokenIds.mapNotNull { tokenId ->
                        uiState.tokenPositions[viewModel.getMyColor()]?.find { it.tokenId == tokenId }
                    },
                    onTokenClick = { color, tokenId ->
                        viewModel.onTokenClick(color, tokenId)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dice and controls
                DiceWithControls(
                    diceValue = uiState.diceValue,
                    isRolling = uiState.isRolling,
                    canRoll = uiState.canRollDice,
                    isMyTurn = uiState.isMyTurn,
                    onRoll = { viewModel.rollDice() },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Turn indicator
                TurnIndicator(
                    isMyTurn = uiState.isMyTurn,
                    currentPlayerName = uiState.players[uiState.gameState?.currentTurnPlayerId]?.name ?: "Unknown"
                )
            }
        }
    }
}

/**
 * Horizontal bar showing all players with turn indicator.
 */
@Composable
fun PlayersInfoBar(
    players: List<Player>,
    currentTurnPlayerId: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        players.forEach { player ->
            PlayerChip(
                player = player,
                isCurrentTurn = player.id == currentTurnPlayerId
            )
        }
    }
}

/**
 * Individual player chip showing name and color.
 */
@Composable
fun PlayerChip(
    player: Player,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when (player.color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) color.copy(alpha = 0.2f) 
                           else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentTurn) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            
            Column {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
                )
                if (isCurrentTurn) {
                    Text(
                        text = "Playing",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Turn indicator at bottom of screen.
 */
@Composable
fun TurnIndicator(
    isMyTurn: Boolean,
    currentPlayerName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMyTurn) Color(0xFF4CAF50).copy(alpha = 0.1f)
                           else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = if (isMyTurn) "Your Turn!" else "$currentPlayerName's turn",
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = if (isMyTurn) Color(0xFF4CAF50) 
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Winner celebration dialog.
 */
@Composable
fun WinnerDialog(
    winner: Player,
    isMe: Boolean,
    onDismiss: () -> Unit
) {
    val color = when (winner.color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 64.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isMe) "You Win!" else "${winner.name} Wins!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isMe) "Congratulations!" else "Better luck next time!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Home")
                }
            }
        }
    }
}
