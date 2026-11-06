package com.ludo.game.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ludo.game.data.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing game history and player statistics.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val historyRef = database.getReference("gameHistory")
    private val statsRef = database.getReference("playerStats")
    private val movesRef = database.getReference("gameMoves")
    
    /**
     * Save completed game to history.
     */
    suspend fun saveGameHistory(
        roomId: String,
        roomCode: String,
        players: List<GameHistoryPlayer>,
        winnerId: String,
        winnerName: String,
        moves: List<GameHistoryMove>,
        startTime: Long,
        endTime: Long
    ): Result<GameHistory> {
        return try {
            val gameId = historyRef.push().key
                ?: throw IllegalStateException("Failed to generate history ID")
            
            // Generate highlights
            val highlights = generateHighlights(moves, players)
            
            val history = GameHistory(
                gameId = gameId,
                roomCode = roomCode,
                startTime = startTime,
                endTime = endTime,
                players = players,
                winnerId = winnerId,
                winnerName = winnerName,
                totalMoves = moves.size,
                moves = moves,
                highlights = highlights
            )
            
            // Save main history (without full moves list for quick loading)
            val historySummary = history.copy(moves = emptyList())
            historyRef.child(gameId).setValue(historySummary).await()
            
            // Save moves separately for replay functionality
            movesRef.child(gameId).setValue(moves).await()
            
            // Update player stats
            players.forEach { player ->
                updatePlayerStats(player, winnerId)
            }
            
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate game highlights from moves.
     */
    private fun generateHighlights(
        moves: List<GameHistoryMove>,
        players: List<GameHistoryPlayer>
    ): List<GameHighlight> {
        val highlights = mutableListOf<GameHighlight>()
        
        // Track first token out for each player
        val firstTokenOut = mutableSetOf<String>()
        
        moves.forEachIndexed { index, move ->
            // First token out
            if (move.fromPosition == -1 && move.toPosition == 0 && !firstTokenOut.contains(move.playerId)) {
                firstTokenOut.add(move.playerId)
                highlights.add(
                    GameHighlight(
                        type = HighlightType.FIRST_TOKEN_OUT,
                        moveNumber = index + 1,
                        description = "${move.playerName} got their first token out!",
                        playerId = move.playerId,
                        playerName = move.playerName
                    )
                )
            }
            
            // Captures
            if (move.wasCapture) {
                val capturedPlayer = players.find { it.playerId == move.capturedPlayerId }
                highlights.add(
                    GameHighlight(
                        type = HighlightType.CAPTURE,
                        moveNumber = index + 1,
                        description = "${move.playerName} captured ${capturedPlayer?.name ?: "opponent"}!",
                        playerId = move.playerId,
                        playerName = move.playerName
                    )
                )
            }
            
            // Token finished
            if (move.wasTokenFinished) {
                highlights.add(
                    GameHighlight(
                        type = HighlightType.TOKEN_FINISHED,
                        moveNumber = index + 1,
                        description = "${move.playerName} got a token home!",
                        playerId = move.playerId,
                        playerName = move.playerName
                    )
                )
            }
        }
        
        // Winning move
        if (moves.isNotEmpty()) {
            val lastMove = moves.last()
            highlights.add(
                GameHighlight(
                    type = HighlightType.WINNING_MOVE,
                    moveNumber = moves.size,
                    description = "${lastMove.playerName} wins the game!",
                    playerId = lastMove.playerId,
                    playerName = lastMove.playerName
                )
            )
        }
        
        return highlights.sortedBy { it.moveNumber }
    }
    
    /**
     * Update player statistics after a game.
     */
    private suspend fun updatePlayerStats(
        player: GameHistoryPlayer,
        winnerId: String
    ) {
        try {
            val existingStats = statsRef.child(player.playerId).get().await()
                .getValue(PlayerStats::class.java) ?: PlayerStats(playerId = player.playerId)
            
            val isWinner = player.playerId == winnerId
            
            val updatedStats = existingStats.copy(
                totalGames = existingStats.totalGames + 1,
                wins = existingStats.wins + if (isWinner) 1 else 0,
                losses = existingStats.losses + if (!isWinner) 1 else 0,
                totalCaptures = existingStats.totalCaptures + player.captures,
                totalGotCaptured = existingStats.totalGotCaptured + player.gotCaptured,
                totalSixesRolled = existingStats.totalSixesRolled + player.sixesRolled,
                currentWinStreak = if (isWinner) existingStats.currentWinStreak + 1 else 0,
                longestWinStreak = if (isWinner) {
                    maxOf(existingStats.longestWinStreak, existingStats.currentWinStreak + 1)
                } else existingStats.longestWinStreak
            )
            
            statsRef.child(player.playerId).setValue(updatedStats).await()
        } catch (e: Exception) {
            // Log but don't fail the main operation
            e.printStackTrace()
        }
    }
    
    /**
     * Get player's game history.
     */
    suspend fun getPlayerHistory(
        playerId: String = auth.currentUser?.uid ?: "",
        limit: Int = 20
    ): Result<List<GameHistory>> {
        return try {
            val snapshot = historyRef
                .orderByChild("endTime")
                .limitToLast(limit)
                .get()
                .await()
            
            val histories = snapshot.children.mapNotNull { child ->
                child.getValue(GameHistory::class.java)
            }.filter { history ->
                history.players.any { it.playerId == playerId }
            }.sortedByDescending { it.endTime }
            
            Result.success(histories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get full game details including moves for replay.
     */
    suspend fun getGameForReplay(gameId: String): Result<GameHistory> {
        return try {
            val historySnapshot = historyRef.child(gameId).get().await()
            val history = historySnapshot.getValue(GameHistory::class.java)
                ?: throw IllegalArgumentException("Game not found")
            
            val movesSnapshot = movesRef.child(gameId).get().await()
            val moves = movesSnapshot.children.mapNotNull { child ->
                child.getValue(GameHistoryMove::class.java)
            }
            
            Result.success(history.copy(moves = moves))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get player statistics.
     */
    suspend fun getPlayerStats(
        playerId: String = auth.currentUser?.uid ?: ""
    ): Result<PlayerStats> {
        return try {
            val snapshot = statsRef.child(playerId).get().await()
            val stats = snapshot.getValue(PlayerStats::class.java)
                ?: PlayerStats(playerId = playerId)
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get leaderboard.
     */
    suspend fun getLeaderboard(limit: Int = 10): Result<List<PlayerStats>> {
        return try {
            val snapshot = statsRef
                .orderByChild("wins")
                .limitToLast(limit)
                .get()
                .await()
            
            val leaderboard = snapshot.children.mapNotNull { child ->
                child.getValue(PlayerStats::class.java)
            }.sortedByDescending { it.wins }
            
            Result.success(leaderboard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
