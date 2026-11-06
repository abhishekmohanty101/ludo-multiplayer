package com.ludo.game.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludo.game.data.model.GameRoom
import com.ludo.game.data.model.RoomStatus
import com.ludo.game.data.repository.RoomRepository
import com.ludo.game.util.RoomShareManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for lobby screens.
 */
data class LobbyUiState(
    val isLoading: Boolean = false,
    val room: GameRoom? = null,
    val roomCode: String = "",
    val shareLink: String = "",
    val shareMessage: String = "",
    val error: String? = null,
    val isGameStarted: Boolean = false
)

/**
 * ViewModel for room creation, joining, and waiting room.
 */
@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()
    
    /**
     * Creates a new room and generates share link.
     */
    fun createRoom(hostName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            roomRepository.createRoom(hostName)
                .onSuccess { room ->
                    val shareLink = RoomShareManager.generateWebLink(room.roomCode)
                    val shareMessage = RoomShareManager.createShareMessage(room.roomCode, hostName)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            room = room,
                            roomCode = room.roomCode,
                            shareLink = shareLink,
                            shareMessage = shareMessage
                        )
                    }
                    
                    // Start observing room changes
                    observeRoom(room.roomId)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to create room"
                        )
                    }
                }
        }
    }
    
    /**
     * Joins an existing room using room code.
     */
    fun joinRoom(roomCode: String, playerName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            roomRepository.joinRoom(roomCode, playerName)
                .onSuccess { room ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            room = room,
                            roomCode = room.roomCode
                        )
                    }
                    
                    // Start observing room changes
                    observeRoom(room.roomId)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to join room"
                        )
                    }
                }
        }
    }
    
    /**
     * Observes room for real-time updates.
     */
    private fun observeRoom(roomId: String) {
        viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { room ->
                room?.let {
                    _uiState.update { state ->
                        state.copy(
                            room = room,
                            isGameStarted = room.status == RoomStatus.IN_PROGRESS
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Sets current player's ready status.
     */
    fun setReady(isReady: Boolean) {
        val roomId = _uiState.value.room?.roomId ?: return
        
        viewModelScope.launch {
            roomRepository.setPlayerReady(roomId, isReady)
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to update status")
                    }
                }
        }
    }
    
    /**
     * Starts the game (host only).
     */
    fun startGame() {
        val roomId = _uiState.value.room?.roomId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            roomRepository.startGame(roomId)
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to start game"
                        )
                    }
                }
        }
    }
    
    /**
     * Leaves the current room.
     */
    fun leaveRoom() {
        val roomId = _uiState.value.room?.roomId ?: return
        
        viewModelScope.launch {
            roomRepository.leaveRoom(roomId)
            _uiState.update { LobbyUiState() } // Reset state
        }
    }
    
    /**
     * Clears error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
