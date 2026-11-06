package com.ludo.game.ui.components.board

import com.ludo.game.data.model.PlayerColor

/**
 * Maps board positions (0-56) to grid coordinates (row, col) for each player.
 * Each player has their own track that overlaps with others on the main path.
 */
object BoardPositionMapper {
    
    /**
     * Convert a logical board position to grid coordinates.
     * 
     * @param position Board position (-1 = home, 0-51 = main track, 52-56 = finish lane, 57 = won)
     * @param color Player color (determines starting position and finish lane)
     * @param tokenId Token ID (0-3) for positioning in home base
     * @return Pair of (row, col) grid coordinates
     */
    fun toGridPosition(position: Int, color: PlayerColor, tokenId: Int): Pair<Int, Int> {
        return when {
            position == -1 -> getHomePosition(color, tokenId)
            position == 57 -> Pair(7, 7) // Center (won)
            position in 52..56 -> getFinishLanePosition(color, position - 52)
            else -> getMainTrackPosition(color, position)
        }
    }
    
    /**
     * Get home base position for a token.
     */
    private fun getHomePosition(color: PlayerColor, tokenId: Int): Pair<Int, Int> {
        val homeOffsets = listOf(
            Pair(1, 1), Pair(1, 3), Pair(3, 1), Pair(3, 3)
        )
        val (rowOffset, colOffset) = homeOffsets[tokenId]
        
        return when (color) {
            PlayerColor.RED -> Pair(rowOffset, colOffset)
            PlayerColor.GREEN -> Pair(rowOffset, colOffset + 9)
            PlayerColor.BLUE -> Pair(rowOffset + 9, colOffset)
            PlayerColor.YELLOW -> Pair(rowOffset + 9, colOffset + 9)
        }
    }
    
    /**
     * Get finish lane position (last 5 cells before winning).
     */
    private fun getFinishLanePosition(color: PlayerColor, lanePosition: Int): Pair<Int, Int> {
        return when (color) {
            PlayerColor.RED -> Pair(7, 1 + lanePosition)      // Left to center
            PlayerColor.GREEN -> Pair(1 + lanePosition, 7)    // Top to center
            PlayerColor.YELLOW -> Pair(7, 13 - lanePosition)  // Right to center
            PlayerColor.BLUE -> Pair(13 - lanePosition, 7)    // Bottom to center
        }
    }
    
    /**
     * Get main track position.
     * The main track has 52 cells shared by all players, but each starts at different offset.
     */
    private fun getMainTrackPosition(color: PlayerColor, position: Int): Pair<Int, Int> {
        // Starting offsets for each color on the 52-cell track
        val startOffset = when (color) {
            PlayerColor.RED -> 0
            PlayerColor.GREEN -> 13
            PlayerColor.YELLOW -> 26
            PlayerColor.BLUE -> 39
        }
        
        // Calculate absolute position on the shared track
        val absolutePosition = (position + startOffset) % 52
        
        // Map absolute position to grid coordinates
        return TRACK_POSITIONS[absolutePosition]
    }
    
    /**
     * The 52 positions on the main track, mapped to (row, col).
     * Starting from Red's start position and going clockwise.
     */
    private val TRACK_POSITIONS = listOf(
        // Red start and top-left vertical path (going up)
        Pair(6, 1),   // 0 - Red start
        Pair(5, 1),   // 1
        Pair(4, 1),   // 2
        Pair(3, 1),   // 3
        Pair(2, 1),   // 4
        Pair(1, 1),   // 5
        Pair(0, 2),   // 6 - Corner turn
        
        // Top horizontal path (going right)
        Pair(0, 3),   // 7
        Pair(0, 4),   // 8
        Pair(0, 5),   // 9
        Pair(0, 6),   // 10
        Pair(1, 6),   // 11
        Pair(2, 6),   // 12
        
        // Green start and top-right vertical path (going down)
        Pair(1, 8),   // 13 - Green start
        Pair(2, 8),   // 14
        Pair(3, 8),   // 15
        Pair(4, 8),   // 16
        Pair(5, 8),   // 17
        Pair(6, 8),   // 18
        Pair(6, 9),   // 19 - Corner turn
        
        // Right horizontal path (going down then left)
        Pair(6, 10),  // 20
        Pair(6, 11),  // 21
        Pair(6, 12),  // 22
        Pair(6, 13),  // 23
        Pair(6, 14),  // 24
        Pair(7, 14),  // 25 - Corner
        Pair(8, 14),  // 26 - Yellow start
        
        // Bottom-right vertical path (going down)
        Pair(8, 13),  // 27
        Pair(8, 12),  // 28
        Pair(8, 11),  // 29
        Pair(8, 10),  // 30
        Pair(8, 9),   // 31
        Pair(9, 8),   // 32 - Corner turn
        
        // Bottom horizontal path (going left)
        Pair(10, 8),  // 33
        Pair(11, 8),  // 34
        Pair(12, 8),  // 35
        Pair(13, 8),  // 36
        Pair(14, 8),  // 37
        Pair(14, 7),  // 38 - Corner
        
        // Blue start and bottom-left vertical path (going up)
        Pair(13, 6),  // 39 - Blue start
        Pair(12, 6),  // 40
        Pair(11, 6),  // 41
        Pair(10, 6),  // 42
        Pair(9, 6),   // 43
        Pair(8, 6),   // 44
        Pair(8, 5),   // 45 - Corner turn
        
        // Left horizontal path (going left then up)
        Pair(8, 4),   // 46
        Pair(8, 3),   // 47
        Pair(8, 2),   // 48
        Pair(8, 1),   // 49
        Pair(8, 0),   // 50
        Pair(7, 0),   // 51 - Back to start area
    )
    
    /**
     * Check if a position is a safe spot.
     */
    fun isSafeSpot(position: Int): Boolean {
        // Safe spots: start positions and star positions
        return position in listOf(0, 8, 13, 21, 26, 34, 39, 47)
    }
    
    /**
     * Get all valid positions a token can move to from current position.
     */
    fun getValidMoves(
        currentPosition: Int,
        diceValue: Int,
        color: PlayerColor
    ): Int? {
        if (currentPosition == -1) {
            // Can only leave home with a 6
            return if (diceValue == 6) 0 else null
        }
        
        if (currentPosition == 57) {
            // Already won
            return null
        }
        
        val newPosition = currentPosition + diceValue
        
        // Check if entering finish lane
        if (currentPosition < 52 && newPosition >= 52) {
            // Must land exactly in finish or center
            return if (newPosition <= 57) newPosition else null
        }
        
        // Normal move on main track
        if (currentPosition < 52) {
            return newPosition % 52
        }
        
        // Moving in finish lane
        return if (newPosition <= 57) newPosition else null
    }
}
