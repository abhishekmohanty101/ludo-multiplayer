package com.ludo.game.data.model

/**
 * Represents a completed game for history/replay.
 */
data class GameHistory(
    val gameId: String = "",
    val roomCode: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val players: List<GameHistoryPlayer> = emptyList(),
    val winnerId: String = "",
    val winnerName: String = "",
    val totalMoves: Int = 0,
    val moves: List<GameHistoryMove> = emptyList(),
    val highlights: List<GameHighlight> = emptyList()
)

/**
 * Player info in game history.
 */
data class GameHistoryPlayer(
    val playerId: String = "",
    val name: String = "",
    val avatar: String = "😀",
    val color: String = "RED",
    val finalPosition: Int = 0,  // 1st, 2nd, 3rd, 4th
    val tokensFinished: Int = 0,
    val captures: Int = 0,
    val gotCaptured: Int = 0,
    val sixesRolled: Int = 0
)

/**
 * Single move in game history.
 */
data class GameHistoryMove(
    val moveNumber: Int = 0,
    val playerId: String = "",
    val playerName: String = "",
    val playerColor: String = "",
    val tokenId: Int = 0,
    val fromPosition: Int = 0,
    val toPosition: Int = 0,
    val diceValue: Int = 0,
    val wasCapture: Boolean = false,
    val capturedPlayerId: String? = null,
    val wasTokenFinished: Boolean = false,
    val timestamp: Long = 0
)

/**
 * Notable game moments.
 */
data class GameHighlight(
    val type: HighlightType = HighlightType.CAPTURE,
    val moveNumber: Int = 0,
    val description: String = "",
    val playerId: String = "",
    val playerName: String = ""
)

/**
 * Types of game highlights.
 */
enum class HighlightType {
    FIRST_TOKEN_OUT,    // First token left home
    CAPTURE,            // Captured opponent
    TRIPLE_SIX,         // Rolled three 6s
    TOKEN_FINISHED,     // Token reached home
    CLOSE_CALL,         // Almost got captured
    COMEBACK,           // Recovered from behind
    WINNING_MOVE        // The final winning move
}

/**
 * Game statistics for a player.
 */
data class PlayerStats(
    val playerId: String = "",
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalCaptures: Int = 0,
    val totalGotCaptured: Int = 0,
    val totalSixesRolled: Int = 0,
    val averageGameDuration: Long = 0,
    val longestWinStreak: Int = 0,
    val currentWinStreak: Int = 0,
    val favoriteColor: String = "RED"
) {
    val winRate: Float
        get() = if (totalGames > 0) wins.toFloat() / totalGames else 0f
}
