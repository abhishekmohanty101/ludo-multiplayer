package com.ludo.game.data.model

/**
 * Represents the complete state of a game in progress.
 */
data class GameState(
    val roomId: String = "",
    val currentTurnPlayerId: String = "",
    val turnStartTime: Long = 0,
    val diceValue: Int? = null,
    val canRollDice: Boolean = true,
    val consecutiveSixes: Int = 0,
    val tokens: Map<String, Map<String, TokenState>> = emptyMap(),
    val winner: String? = null,
    val lastMoveTimestamp: Long = 0
) {
    /**
     * Get token positions for a specific player color.
     */
    fun getTokenPositions(playerId: String): List<Int> {
        return tokens[playerId]?.values?.map { it.position } ?: listOf(-1, -1, -1, -1)
    }
    
    /**
     * Check if game is over.
     */
    fun isGameOver(): Boolean = winner != null
}

/**
 * State of a single token.
 */
data class TokenState(
    val position: Int = -1,  // -1 = home, 0-51 = track, 52-56 = finish lane, 57 = won
    val state: TokenStateEnum = TokenStateEnum.IN_HOME
)

/**
 * Token state enum.
 */
enum class TokenStateEnum {
    IN_HOME,     // Starting position in home base
    ON_BOARD,    // Moving on the main track
    IN_FINISH,   // In the finish lane
    FINISHED     // Reached center (won)
}

/**
 * Represents a move made by a player.
 */
data class GameMove(
    val playerId: String = "",
    val tokenId: Int = 0,
    val fromPosition: Int = 0,
    val toPosition: Int = 0,
    val diceValue: Int = 0,
    val capturedPlayerId: String? = null,
    val capturedTokenId: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
