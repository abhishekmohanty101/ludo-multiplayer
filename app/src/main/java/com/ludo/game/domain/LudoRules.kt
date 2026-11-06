package com.ludo.game.domain

import com.ludo.game.data.model.PlayerColor
import com.ludo.game.ui.components.board.BoardPositionMapper
import kotlin.random.Random

/**
 * Core game logic for Ludo.
 */
object LudoRules {
    
    const val TOKENS_PER_PLAYER = 4
    const val MAIN_TRACK_SIZE = 52
    const val FINISH_LANE_SIZE = 5
    const val WINNING_POSITION = 57
    
    /**
     * Roll the dice (1-6).
     */
    fun rollDice(): Int = Random.nextInt(1, 7)
    
    /**
     * Check if a token can leave home (needs a 6).
     */
    fun canLeaveHome(diceValue: Int): Boolean = diceValue == 6
    
    /**
     * Check if a move is valid.
     */
    fun isValidMove(
        currentPosition: Int,
        diceValue: Int,
        playerColor: PlayerColor,
        allTokenPositions: Map<PlayerColor, List<Int>>
    ): Boolean {
        // Can't move finished tokens
        if (currentPosition == WINNING_POSITION) return false
        
        // Need 6 to leave home
        if (currentPosition == -1 && diceValue != 6) return false
        
        // Calculate new position
        val newPosition = calculateNewPosition(currentPosition, diceValue)
        
        // Can't overshoot winning position
        if (newPosition > WINNING_POSITION) return false
        
        // Can't land on own token (stacking not allowed)
        val ownTokens = allTokenPositions[playerColor] ?: emptyList()
        if (newPosition != -1 && newPosition < WINNING_POSITION && ownTokens.contains(newPosition)) {
            // Check if it's the same token moving (allowed)
            return ownTokens.count { it == currentPosition } == 1
        }
        
        return true
    }
    
    /**
     * Calculate new position after moving.
     */
    fun calculateNewPosition(currentPosition: Int, diceValue: Int): Int {
        return when {
            currentPosition == -1 -> 0  // Leave home to start
            currentPosition + diceValue > WINNING_POSITION -> currentPosition  // Invalid
            else -> currentPosition + diceValue
        }
    }
    
    /**
     * Check if a position is a safe spot.
     */
    fun isSafeSpot(position: Int): Boolean {
        return BoardPositionMapper.isSafeSpot(position)
    }
    
    /**
     * Check if moving to a position would capture an opponent.
     */
    fun checkCapture(
        newPosition: Int,
        movingPlayerColor: PlayerColor,
        allTokenPositions: Map<PlayerColor, List<Int>>
    ): CaptureResult? {
        // Can't capture on safe spots
        if (isSafeSpot(newPosition)) return null
        
        // Can't capture in finish lane or winning position
        if (newPosition >= MAIN_TRACK_SIZE) return null
        
        // Check each opponent's tokens
        for ((color, positions) in allTokenPositions) {
            if (color == movingPlayerColor) continue
            
            positions.forEachIndexed { tokenId, position ->
                if (position == newPosition) {
                    return CaptureResult(color, tokenId)
                }
            }
        }
        
        return null
    }
    
    /**
     * Check if a player has won (all 4 tokens finished).
     */
    fun checkWin(tokenPositions: List<Int>): Boolean {
        return tokenPositions.all { it == WINNING_POSITION }
    }
    
    /**
     * Get all valid moves for a player's tokens.
     */
    fun getValidMoves(
        playerColor: PlayerColor,
        tokenPositions: List<Int>,
        diceValue: Int,
        allTokenPositions: Map<PlayerColor, List<Int>>
    ): List<Int> {
        return tokenPositions.indices.filter { tokenId ->
            isValidMove(
                currentPosition = tokenPositions[tokenId],
                diceValue = diceValue,
                playerColor = playerColor,
                allTokenPositions = allTokenPositions
            )
        }
    }
    
    /**
     * Check if player gets a bonus roll (rolled 6 or captured).
     */
    fun getsBonusRoll(diceValue: Int, captured: Boolean): Boolean {
        return diceValue == 6 || captured
    }
    
    /**
     * Check for three consecutive sixes (token returns home).
     */
    fun checkThreeSixes(consecutiveSixes: Int): Boolean {
        return consecutiveSixes >= 3
    }
    
    /**
     * Get next player in turn order.
     */
    fun getNextPlayer(
        currentPlayer: PlayerColor,
        activePlayers: List<PlayerColor>
    ): PlayerColor {
        val currentIndex = activePlayers.indexOf(currentPlayer)
        val nextIndex = (currentIndex + 1) % activePlayers.size
        return activePlayers[nextIndex]
    }
    
    /**
     * Get the standard turn order based on player colors.
     */
    fun getTurnOrder(): List<PlayerColor> {
        return listOf(
            PlayerColor.RED,
            PlayerColor.GREEN,
            PlayerColor.YELLOW,
            PlayerColor.BLUE
        )
    }
}

/**
 * Result of a capture.
 */
data class CaptureResult(
    val capturedPlayerColor: PlayerColor,
    val capturedTokenId: Int
)

/**
 * Result of a move.
 */
data class MoveResult(
    val newPosition: Int,
    val capture: CaptureResult?,
    val isWinningMove: Boolean,
    val bonusRoll: Boolean
)
