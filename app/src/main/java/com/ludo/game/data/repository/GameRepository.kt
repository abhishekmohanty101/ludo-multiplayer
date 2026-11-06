package com.ludo.game.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ludo.game.data.model.GameMove
import com.ludo.game.data.model.GameState
import com.ludo.game.data.model.TokenState
import com.ludo.game.data.model.TokenStateEnum
import com.ludo.game.domain.LudoRules
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing game state in Firebase.
 */
@Singleton
class GameRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val gameStatesRef = database.getReference("gameStates")
    
    /**
     * Initialize a new game state when game starts.
     */
    suspend fun initializeGame(
        roomId: String,
        playerIds: List<String>
    ): Result<GameState> {
        return try {
            // Create initial token states for each player
            val tokens = playerIds.associateWith { playerId ->
                (0..3).associate { tokenId ->
                    tokenId.toString() to TokenState(
                        position = -1,
                        state = TokenStateEnum.IN_HOME
                    )
                }
            }
            
            val gameState = GameState(
                roomId = roomId,
                currentTurnPlayerId = playerIds.first(),
                turnStartTime = System.currentTimeMillis(),
                canRollDice = true,
                tokens = tokens
            )
            
            gameStatesRef.child(roomId).setValue(gameState).await()
            
            Result.success(gameState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe game state changes in real-time.
     */
    fun observeGameState(roomId: String): Flow<GameState?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameState = snapshot.getValue(GameState::class.java)
                trySend(gameState)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        gameStatesRef.child(roomId).addValueEventListener(listener)
        
        awaitClose {
            gameStatesRef.child(roomId).removeEventListener(listener)
        }
    }
    
    /**
     * Roll the dice (only current player can do this).
     */
    suspend fun rollDice(roomId: String): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            
            val snapshot = gameStatesRef.child(roomId).get().await()
            val gameState = snapshot.getValue(GameState::class.java)
                ?: throw IllegalArgumentException("Game not found")
            
            // Validate it's this player's turn
            if (gameState.currentTurnPlayerId != userId) {
                throw IllegalStateException("Not your turn")
            }
            
            if (!gameState.canRollDice) {
                throw IllegalStateException("Cannot roll dice now")
            }
            
            // Roll the dice
            val diceValue = LudoRules.rollDice()
            
            // Check for three consecutive sixes
            val newConsecutiveSixes = if (diceValue == 6) {
                gameState.consecutiveSixes + 1
            } else {
                0
            }
            
            // Update game state
            val updates = mapOf(
                "diceValue" to diceValue,
                "canRollDice" to false,
                "consecutiveSixes" to newConsecutiveSixes
            )
            
            gameStatesRef.child(roomId).updateChildren(updates).await()
            
            Result.success(diceValue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Move a token.
     */
    suspend fun moveToken(
        roomId: String,
        tokenId: Int
    ): Result<GameMove> {
        return try {
            val userId = auth.currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            
            val snapshot = gameStatesRef.child(roomId).get().await()
            val gameState = snapshot.getValue(GameState::class.java)
                ?: throw IllegalArgumentException("Game not found")
            
            // Validate turn
            if (gameState.currentTurnPlayerId != userId) {
                throw IllegalStateException("Not your turn")
            }
            
            val diceValue = gameState.diceValue
                ?: throw IllegalStateException("Must roll dice first")
            
            // Get current token position
            val currentTokenState = gameState.tokens[userId]?.get(tokenId.toString())
                ?: throw IllegalArgumentException("Token not found")
            
            val currentPosition = currentTokenState.position
            
            // Calculate new position
            val newPosition = LudoRules.calculateNewPosition(currentPosition, diceValue)
            
            // Check for captures
            val allPositions = gameState.tokens.mapValues { (_, tokens) ->
                tokens.values.map { it.position }
            }
            
            // Convert to PlayerColor for capture check (simplified - you'd map userId to color)
            // For now, we'll skip capture logic in this simplified version
            
            // Determine new token state
            val newTokenState = when {
                newPosition == LudoRules.WINNING_POSITION -> TokenStateEnum.FINISHED
                newPosition >= LudoRules.MAIN_TRACK_SIZE -> TokenStateEnum.IN_FINISH
                newPosition >= 0 -> TokenStateEnum.ON_BOARD
                else -> TokenStateEnum.IN_HOME
            }
            
            // Check for win
            val updatedTokens = gameState.tokens[userId]!!.toMutableMap()
            updatedTokens[tokenId.toString()] = TokenState(newPosition, newTokenState)
            
            val allFinished = updatedTokens.values.all { it.state == TokenStateEnum.FINISHED }
            
            // Determine next turn
            val bonusRoll = diceValue == 6 && gameState.consecutiveSixes < 3
            val nextPlayerId = if (bonusRoll) {
                userId
            } else {
                getNextPlayer(userId, gameState.tokens.keys.toList())
            }
            
            // Create move record
            val move = GameMove(
                playerId = userId,
                tokenId = tokenId,
                fromPosition = currentPosition,
                toPosition = newPosition,
                diceValue = diceValue,
                timestamp = System.currentTimeMillis()
            )
            
            // Update game state
            val updates = mutableMapOf<String, Any?>(
                "tokens/$userId/$tokenId/position" to newPosition,
                "tokens/$userId/$tokenId/state" to newTokenState.name,
                "diceValue" to null,
                "canRollDice" to true,
                "currentTurnPlayerId" to nextPlayerId,
                "turnStartTime" to System.currentTimeMillis(),
                "lastMoveTimestamp" to System.currentTimeMillis()
            )
            
            if (!bonusRoll) {
                updates["consecutiveSixes"] = 0
            }
            
            if (allFinished) {
                updates["winner"] = userId
            }
            
            gameStatesRef.child(roomId).updateChildren(updates).await()
            
            Result.success(move)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Skip turn (when no valid moves available).
     */
    suspend fun skipTurn(roomId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            
            val snapshot = gameStatesRef.child(roomId).get().await()
            val gameState = snapshot.getValue(GameState::class.java)
                ?: throw IllegalArgumentException("Game not found")
            
            if (gameState.currentTurnPlayerId != userId) {
                throw IllegalStateException("Not your turn")
            }
            
            val nextPlayerId = getNextPlayer(userId, gameState.tokens.keys.toList())
            
            val updates = mapOf(
                "diceValue" to null,
                "canRollDice" to true,
                "currentTurnPlayerId" to nextPlayerId,
                "turnStartTime" to System.currentTimeMillis(),
                "consecutiveSixes" to 0
            )
            
            gameStatesRef.child(roomId).updateChildren(updates).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get next player in rotation.
     */
    private fun getNextPlayer(currentPlayerId: String, playerIds: List<String>): String {
        val currentIndex = playerIds.indexOf(currentPlayerId)
        val nextIndex = (currentIndex + 1) % playerIds.size
        return playerIds[nextIndex]
    }
}
