package com.ludo.game.ui.screens.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludo.game.data.model.GameState
import com.ludo.game.data.model.Player
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.data.repository.GameRepository
import com.ludo.game.data.repository.RoomRepository
import com.ludo.game.domain.LudoRules
import com.ludo.game.ui.components.board.BoardPositionMapper
import com.ludo.game.ui.components.board.TokenPosition
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the game screen.
 */
data class GameUiState(
    val isLoading: Boolean = true,
    val players: Map<String, Player> = emptyMap(),
    val gameState: GameState? = null,
    val currentUserId: String = "",
    val isMyTurn: Boolean = false,
    val canRollDice: Boolean = false,
    val diceValue: Int? = null,
    val isRolling: Boolean = false,
    val tokenPositions: Map<PlayerColor, List<TokenPosition>> = emptyMap(),
    val selectableTokenIds: List<Int> = emptyList(),
    val winner: Player? = null,
    val error: String? = null
)

/**
 * ViewModel for the main game screen.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val roomRepository: RoomRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val roomId: String = savedStateHandle.get<String>("roomId") ?: ""
    
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    
    private var playerColorMap: Map<String, PlayerColor> = emptyMap()
    
    init {
        val userId = auth.currentUser?.uid ?: ""
        _uiState.update { it.copy(currentUserId = userId) }
        
        if (roomId.isNotEmpty()) {
            observeRoom()
            observeGameState()
        }
    }
    
    /**
     * Observe room for player info.
     */
    private fun observeRoom() {
        viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { room ->
                room?.let {
                    // Map player IDs to colors based on join order
                    val colors = PlayerColor.entries
                    playerColorMap = room.players.keys.mapIndexed { index, playerId ->
                        playerId to colors[index % colors.size]
                    }.toMap()
                    
                    _uiState.update { state ->
                        state.copy(players = room.players)
                    }
                }
            }
        }
    }
    
    /**
     * Observe game state changes.
     */
    private fun observeGameState() {
        viewModelScope.launch {
            gameRepository.observeGameState(roomId).collect { gameState ->
                gameState?.let { state ->
                    val userId = _uiState.value.currentUserId
                    val isMyTurn = state.currentTurnPlayerId == userId
                    
                    // Convert token positions to UI format
                    val tokenPositions = convertTokenPositions(state)
                    
                    // Get selectable tokens
                    val selectableTokenIds = if (isMyTurn && state.diceValue != null) {
                        getSelectableTokens(state, userId)
                    } else {
                        emptyList()
                    }
                    
                    // Check for winner
                    val winner = state.winner?.let { winnerId ->
                        _uiState.value.players[winnerId]
                    }
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            gameState = state,
                            isMyTurn = isMyTurn,
                            canRollDice = isMyTurn && state.canRollDice,
                            diceValue = state.diceValue,
                            tokenPositions = tokenPositions,
                            selectableTokenIds = selectableTokenIds,
                            winner = winner
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Convert game state tokens to UI token positions.
     */
    private fun convertTokenPositions(gameState: GameState): Map<PlayerColor, List<TokenPosition>> {
        val result = mutableMapOf<PlayerColor, List<TokenPosition>>()
        
        gameState.tokens.forEach { (playerId, tokens) ->
            val color = playerColorMap[playerId] ?: return@forEach
            
            val positions = tokens.map { (tokenIdStr, tokenState) ->
                val tokenId = tokenIdStr.toIntOrNull() ?: 0
                val (row, col) = BoardPositionMapper.toGridPosition(
                    position = tokenState.position,
                    color = color,
                    tokenId = tokenId
                )
                
                TokenPosition(
                    color = color,
                    tokenId = tokenId,
                    boardPosition = tokenState.position,
                    row = row,
                    col = col
                )
            }
            
            result[color] = positions
        }
        
        return result
    }
    
    /**
     * Get tokens that can be moved with current dice value.
     */
    private fun getSelectableTokens(gameState: GameState, playerId: String): List<Int> {
        val diceValue = gameState.diceValue ?: return emptyList()
        val tokens = gameState.tokens[playerId] ?: return emptyList()
        
        return tokens.filter { (tokenIdStr, tokenState) ->
            val position = tokenState.position
            
            // Check if move is valid
            when {
                position == LudoRules.WINNING_POSITION -> false
                position == -1 -> diceValue == 6
                else -> {
                    val newPosition = position + diceValue
                    newPosition <= LudoRules.WINNING_POSITION
                }
            }
        }.map { it.key.toInt() }
    }
    
    /**
     * Roll the dice.
     */
    fun rollDice() {
        if (!_uiState.value.canRollDice) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true) }
            
            // Add delay for animation
            kotlinx.coroutines.delay(800)
            
            gameRepository.rollDice(roomId)
                .onSuccess { diceValue ->
                    _uiState.update { 
                        it.copy(
                            isRolling = false,
                            diceValue = diceValue
                        )
                    }
                    
                    // Auto-skip if no valid moves
                    val state = _uiState.value.gameState
                    if (state != null) {
                        val selectableTokens = getSelectableTokens(
                            state.copy(diceValue = diceValue),
                            _uiState.value.currentUserId
                        )
                        if (selectableTokens.isEmpty()) {
                            skipTurn()
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isRolling = false,
                            error = error.message
                        )
                    }
                }
        }
    }
    
    /**
     * Move a token.
     */
    fun moveToken(tokenId: Int) {
        if (!_uiState.value.selectableTokenIds.contains(tokenId)) return
        
        viewModelScope.launch {
            gameRepository.moveToken(roomId, tokenId)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }
    
    /**
     * Skip turn when no valid moves.
     */
    private fun skipTurn() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Brief delay to show dice result
            gameRepository.skipTurn(roomId)
        }
    }
    
    /**
     * Handle token click on the board.
     */
    fun onTokenClick(color: PlayerColor, tokenId: Int) {
        val userId = _uiState.value.currentUserId
        val myColor = playerColorMap[userId]
        
        // Only allow clicking own tokens
        if (color == myColor && _uiState.value.selectableTokenIds.contains(tokenId)) {
            moveToken(tokenId)
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Get current player's color.
     */
    fun getMyColor(): PlayerColor? {
        return playerColorMap[_uiState.value.currentUserId]
    }
}
