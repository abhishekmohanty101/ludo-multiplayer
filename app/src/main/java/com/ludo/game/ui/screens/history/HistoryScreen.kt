package com.ludo.game.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.ludo.game.data.model.*
import com.ludo.game.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Game history and statistics screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onReplayGame: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game History") },
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
        ) {
            // Player Stats Card
            uiState.playerStats?.let { stats ->
                PlayerStatsCard(
                    stats = stats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            
            // Section Header
            Text(
                text = "Recent Games",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.gameHistory.isEmpty()) {
                EmptyHistoryMessage(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.gameHistory) { game ->
                        GameHistoryCard(
                            game = game,
                            onClick = { onReplayGame(game.gameId) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Player statistics card.
 */
@Composable
fun PlayerStatsCard(
    stats: PlayerStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalGames.toString(),
                    label = "Games",
                    icon = "🎮"
                )
                StatItem(
                    value = stats.wins.toString(),
                    label = "Wins",
                    icon = "🏆"
                )
                StatItem(
                    value = "${(stats.winRate * 100).toInt()}%",
                    label = "Win Rate",
                    icon = "📊"
                )
                StatItem(
                    value = stats.currentWinStreak.toString(),
                    label = "Streak",
                    icon = "🔥"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalCaptures.toString(),
                    label = "Captures",
                    icon = "⚔️"
                )
                StatItem(
                    value = stats.totalSixesRolled.toString(),
                    label = "Sixes",
                    icon = "🎲"
                )
                StatItem(
                    value = stats.longestWinStreak.toString(),
                    label = "Best Streak",
                    icon = "⭐"
                )
            }
        }
    }
}

/**
 * Individual stat item.
 */
@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 24.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Game history card.
 */
@Composable
fun GameHistoryCard(
    game: GameHistory,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val duration = (game.endTime - game.startTime).milliseconds
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Room: ${game.roomCode}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(game.endTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = game.winnerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${game.totalMoves} moves • ${duration.inWholeMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Players row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                game.players.forEach { player ->
                    PlayerChip(
                        player = player,
                        isWinner = player.playerId == game.winnerId
                    )
                }
            }
            
            // Highlights preview
            if (game.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    game.highlights.take(3).forEach { highlight ->
                        HighlightChip(highlight = highlight)
                    }
                    
                    if (game.highlights.size > 3) {
                        Text(
                            text = "+${game.highlights.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Replay button
            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Watch Replay")
            }
        }
    }
}

/**
 * Player chip in history card.
 */
@Composable
private fun PlayerChip(
    player: GameHistoryPlayer,
    isWinner: Boolean
) {
    val color = when (player.color) {
        "RED" -> LudoRed
        "GREEN" -> LudoGreen
        "YELLOW" -> LudoYellow
        "BLUE" -> LudoBlue
        else -> LudoBlue
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = player.avatar, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
            )
            if (isWinner) {
                Text(text = " 👑", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Highlight chip.
 */
@Composable
private fun HighlightChip(highlight: GameHighlight) {
    val icon = when (highlight.type) {
        HighlightType.CAPTURE -> "⚔️"
        HighlightType.TOKEN_FINISHED -> "🏠"
        HighlightType.FIRST_TOKEN_OUT -> "🚀"
        HighlightType.TRIPLE_SIX -> "🎲"
        HighlightType.WINNING_MOVE -> "🏆"
        else -> "⭐"
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = icon,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 14.sp
        )
    }
}

/**
 * Empty history message.
 */
@Composable
private fun EmptyHistoryMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎮", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No games yet",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Play some games to see your history here!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
