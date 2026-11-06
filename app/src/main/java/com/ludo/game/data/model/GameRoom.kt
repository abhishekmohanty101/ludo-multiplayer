package com.ludo.game.data.model

/**
 * Represents a game room for multiplayer matchmaking.
 */
data class GameRoom(
    val roomId: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val status: RoomStatus = RoomStatus.WAITING,
    val maxPlayers: Int = 4,
    val players: Map<String, Player> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if room is full.
     */
    fun isFull(): Boolean = players.size >= maxPlayers
    
    /**
     * Checks if game can start (at least 2 players).
     */
    fun canStart(): Boolean = players.size >= 2
    
    /**
     * Gets available player colors.
     */
    fun getAvailableColors(): List<PlayerColor> {
        val takenColors = players.values.map { it.color }.toSet()
        return PlayerColor.entries.filter { it !in takenColors }
    }
}

/**
 * Room status enum.
 */
enum class RoomStatus {
    WAITING,      // Waiting for players to join
    IN_PROGRESS,  // Game has started
    FINISHED,     // Game ended
    ABANDONED     // All players left
}

/**
 * Player in a room.
 */
data class Player(
    val id: String = "",
    val name: String = "",
    val color: PlayerColor = PlayerColor.RED,
    val isOnline: Boolean = true,
    val isReady: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Player colors matching Ludo board.
 */
enum class PlayerColor {
    RED,
    GREEN,
    YELLOW,
    BLUE
}
