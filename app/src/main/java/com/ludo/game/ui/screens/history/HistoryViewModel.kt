package com.ludo.game.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludo.game.data.model.GameHistory
import com.ludo.game.data.model.GameHistoryMove
import com.ludo.game.data.model.PlayerStats
import com.ludo.game.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for history and replay screens.
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val gameHistory: List<GameHistory> = emptyList(),
    val playerStats: PlayerStats? = null,
    val selectedGame: GameHistory? = null,
    val isReplaying: Boolean = false,
    val replayMoveIndex: Int = 0,
    val replaySpeed: ReplaySpeed = ReplaySpeed.NORMAL,
    val error: String? = null
)

enum class ReplaySpeed(val delayMs: Long) {
    SLOW(2000),
    NORMAL(1000),
    FAST(500),
    INSTANT(100)
}

/**
 * ViewModel for game history and replay functionality.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
        loadStats()
    }
    
    /**
     * Load player's game history.
     */
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            historyRepository.getPlayerHistory()
                .onSuccess { history ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            gameHistory = history
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }
    
    /**
     * Load player statistics.
     */
    fun loadStats() {
        viewModelScope.launch {
            historyRepository.getPlayerStats()
                .onSuccess { stats ->
                    _uiState.update { it.copy(playerStats = stats) }
                }
        }
    }
    
    /**
     * Select a game for replay.
     */
    fun selectGameForReplay(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            historyRepository.getGameForReplay(gameId)
                .onSuccess { game ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            selectedGame = game,
                            replayMoveIndex = 0
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }
    
    /**
     * Start replay playback.
     */
    fun startReplay() {
        val game = _uiState.value.selectedGame ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isReplaying = true, replayMoveIndex = 0) }
            
            game.moves.forEachIndexed { index, _ ->
                if (!_uiState.value.isReplaying) return@launch
                
                _uiState.update { it.copy(replayMoveIndex = index) }
                delay(_uiState.value.replaySpeed.delayMs)
            }
            
            _uiState.update { it.copy(isReplaying = false) }
        }
    }
    
    /**
     * Pause replay.
     */
    fun pauseReplay() {
        _uiState.update { it.copy(isReplaying = false) }
    }
    
    /**
     * Step forward one move.
     */
    fun stepForward() {
        val game = _uiState.value.selectedGame ?: return
        val currentIndex = _uiState.value.replayMoveIndex
        
        if (currentIndex < game.moves.size - 1) {
            _uiState.update { it.copy(replayMoveIndex = currentIndex + 1) }
        }
    }
    
    /**
     * Step backward one move.
     */
    fun stepBackward() {
        val currentIndex = _uiState.value.replayMoveIndex
        
        if (currentIndex > 0) {
            _uiState.update { it.copy(replayMoveIndex = currentIndex - 1) }
        }
    }
    
    /**
     * Jump to specific move.
     */
    fun jumpToMove(index: Int) {
        val game = _uiState.value.selectedGame ?: return
        
        if (index in 0 until game.moves.size) {
            _uiState.update { it.copy(replayMoveIndex = index) }
        }
    }
    
    /**
     * Set replay speed.
     */
    fun setReplaySpeed(speed: ReplaySpeed) {
        _uiState.update { it.copy(replaySpeed = speed) }
    }
    
    /**
     * Get current move for replay.
     */
    fun getCurrentMove(): GameHistoryMove? {
        val game = _uiState.value.selectedGame ?: return null
        val index = _uiState.value.replayMoveIndex
        
        return game.moves.getOrNull(index)
    }
    
    /**
     * Get moves up to current index (for board state).
     */
    fun getMovesUpToCurrent(): List<GameHistoryMove> {
        val game = _uiState.value.selectedGame ?: return emptyList()
        val index = _uiState.value.replayMoveIndex
        
        return game.moves.take(index + 1)
    }
    
    /**
     * Close replay view.
     */
    fun closeReplay() {
        _uiState.update { 
            it.copy(
                selectedGame = null,
                isReplaying = false,
                replayMoveIndex = 0
            )
        }
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
