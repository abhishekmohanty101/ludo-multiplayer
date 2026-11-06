package com.ludo.game.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ludo.game.data.model.*
import com.ludo.game.ui.components.board.LudoBoard
import com.ludo.game.ui.components.board.TokenPosition
import com.ludo.game.ui.components.board.BoardPositionMapper
import com.ludo.game.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Game replay screen with playback controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val game = uiState.selectedGame
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll move list to current move
    LaunchedEffect(uiState.replayMoveIndex) {
        coroutineScope.launch {
            listState.animateScrollToItem(uiState.replayMoveIndex)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Game Replay")
                        game?.let {
                            Text(
                                text = "Room: ${it.roomCode}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.closeReplay()
                        onBackClick()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (game == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Game board showing state at current move
                ReplayBoard(
                    game = game,
                    movesUpToCurrent = viewModel.getMovesUpToCurrent(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(8.dp)
                )
                
                // Current move info
                CurrentMoveInfo(
                    move = viewModel.getCurrentMove(),
                    moveIndex = uiState.replayMoveIndex,
                    totalMoves = game.moves.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                
                // Playback controls
                PlaybackControls(
                    isPlaying = uiState.isReplaying,
                    currentMove = uiState.replayMoveIndex,
                    totalMoves = game.moves.size,
                    speed = uiState.replaySpeed,
                    onPlayPause = {
                        if (uiState.isReplaying) viewModel.pauseReplay()
                        else viewModel.startReplay()
                    },
                    onStepBack = { viewModel.stepBackward() },
                    onStepForward = { viewModel.stepForward() },
                    onSeek = { viewModel.jumpToMove(it) },
                    onSpeedChange = { viewModel.setReplaySpeed(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                
                // Move list
                MoveList(
                    moves = game.moves,
                    currentMoveIndex = uiState.replayMoveIndex,
                    onMoveClick = { viewModel.jumpToMove(it) },
                    listState = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                )
            }
        }
    }
}

/**
 * Board displaying state at current replay position.
 */
@Composable
private fun ReplayBoard(
    game: GameHistory,
    movesUpToCurrent: List<GameHistoryMove>,
    modifier: Modifier = Modifier
) {
    // Calculate board state from moves
    val tokenPositions = remember(movesUpToCurrent) {
        calculateBoardState(game.players, movesUpToCurrent)
    }
    
    LudoBoard(
        tokens = tokenPositions,
        currentPlayer = null,
        selectableTokens = emptyList(),
        onTokenClick = { _, _ -> },
        modifier = modifier
    )
}

/**
 * Calculate board state from list of moves.
 */
private fun calculateBoardState(
    players: List<GameHistoryPlayer>,
    moves: List<GameHistoryMove>
): Map<PlayerColor, List<TokenPosition>> {
    // Initialize all tokens at home (-1)
    val tokenStates = players.associate { player ->
        val color = try {
            PlayerColor.valueOf(player.color)
        } catch (e: Exception) {
            PlayerColor.BLUE
        }
        color to mutableListOf(-1, -1, -1, -1)
    }.toMutableMap()
    
    // Apply each move
    moves.forEach { move ->
        val color = try {
            PlayerColor.valueOf(move.playerColor)
        } catch (e: Exception) {
            return@forEach
        }
        
        tokenStates[color]?.let { tokens ->
            if (move.tokenId in tokens.indices) {
                tokens[move.tokenId] = move.toPosition
            }
        }
        
        // Handle captures - send captured token back home
        if (move.wasCapture && move.capturedPlayerId != null) {
            val capturedPlayer = players.find { it.playerId == move.capturedPlayerId }
            capturedPlayer?.let {
                val capturedColor = try {
                    PlayerColor.valueOf(it.color)
                } catch (e: Exception) {
                    null
                }
                capturedColor?.let { cc ->
                    // Find and reset the token at captured position
                    tokenStates[cc]?.let { tokens ->
                        val capturedIndex = tokens.indexOf(move.toPosition)
                        if (capturedIndex >= 0) {
                            tokens[capturedIndex] = -1
                        }
                    }
                }
            }
        }
    }
    
    // Convert to TokenPosition objects
    return tokenStates.map { (color, positions) ->
        color to positions.mapIndexed { tokenId, position ->
            val (row, col) = BoardPositionMapper.toGridPosition(position, color, tokenId)
            TokenPosition(
                color = color,
                tokenId = tokenId,
                boardPosition = position,
                row = row,
                col = col
            )
        }
    }.toMap()
}

/**
 * Current move information display.
 */
@Composable
private fun CurrentMoveInfo(
    move: GameHistoryMove?,
    moveIndex: Int,
    totalMoves: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Move ${moveIndex + 1}/$totalMoves",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            move?.let {
                val color = when (it.playerColor) {
                    "RED" -> LudoRed
                    "GREEN" -> LudoGreen
                    "YELLOW" -> LudoYellow
                    "BLUE" -> LudoBlue
                    else -> LudoBlue
                }
                
                Text(
                    text = it.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "🎲 ${it.diceValue}",
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (it.wasCapture) {
                    Text(text = "⚔️", fontSize = 18.sp)
                }
                
                if (it.wasTokenFinished) {
                    Text(text = "🏠", fontSize = 18.sp)
                }
            }
        }
    }
}

/**
 * Playback control bar.
 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    currentMove: Int,
    totalMoves: Int,
    speed: ReplaySpeed,
    onPlayPause: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (ReplaySpeed) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Progress slider
        Slider(
            value = currentMove.toFloat(),
            onValueChange = { onSeek(it.toInt()) },
            valueRange = 0f..(totalMoves - 1).coerceAtLeast(0).toFloat(),
            steps = (totalMoves - 2).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed selector
            TextButton(onClick = {
                val speeds = ReplaySpeed.entries
                val currentIndex = speeds.indexOf(speed)
                val nextIndex = (currentIndex + 1) % speeds.size
                onSpeedChange(speeds[nextIndex])
            }) {
                Text(
                    text = when (speed) {
                        ReplaySpeed.SLOW -> "0.5x"
                        ReplaySpeed.NORMAL -> "1x"
                        ReplaySpeed.FAST -> "2x"
                        ReplaySpeed.INSTANT -> "4x"
                    }
                )
            }
            
            // Step back
            IconButton(
                onClick = onStepBack,
                enabled = currentMove > 0
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            
            // Play/Pause
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Step forward
            IconButton(
                onClick = onStepForward,
                enabled = currentMove < totalMoves - 1
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
            
            // Restart
            IconButton(onClick = { onSeek(0) }) {
                Icon(Icons.Default.Replay, contentDescription = "Restart")
            }
        }
    }
}

/**
 * List of all moves.
 */
@Composable
private fun MoveList(
    moves: List<GameHistoryMove>,
    currentMoveIndex: Int,
    onMoveClick: (Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(8.dp)
    ) {
        items(moves.size) { index ->
            val move = moves[index]
            val isCurrentMove = index == currentMoveIndex
            
            MoveListItem(
                move = move,
                moveNumber = index + 1,
                isCurrentMove = isCurrentMove,
                onClick = { onMoveClick(index) }
            )
        }
    }
}

/**
 * Single move in the list.
 */
@Composable
private fun MoveListItem(
    move: GameHistoryMove,
    moveNumber: Int,
    isCurrentMove: Boolean,
    onClick: () -> Unit
) {
    val color = when (move.playerColor) {
        "RED" -> LudoRed
        "GREEN" -> LudoGreen
        "YELLOW" -> LudoYellow
        "BLUE" -> LudoBlue
        else -> LudoBlue
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrentMove) color.copy(alpha = 0.2f) 
               else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$moveNumber",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            
            Text(
                text = move.playerName,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp)
            )
            
            Text(
                text = "🎲${move.diceValue}",
                modifier = Modifier.width(40.dp)
            )
            
            Text(
                text = "${move.fromPosition} → ${move.toPosition}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (move.wasCapture) {
                Text(text = "⚔️", fontSize = 14.sp)
            }
            if (move.wasTokenFinished) {
                Text(text = "🏠", fontSize = 14.sp)
            }
        }
    }
}
