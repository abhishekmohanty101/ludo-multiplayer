package com.ludo.game.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ludo.game.data.model.GameRoom
import com.ludo.game.data.model.Player
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.data.model.RoomStatus
import com.ludo.game.util.RoomCodeGenerator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing game rooms in Firebase.
 */
@Singleton
class RoomRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val roomsRef = database.getReference("rooms")
    private val roomCodesRef = database.getReference("roomCodes")
    
    /**
     * Creates a new game room and returns the room code.
     */
    suspend fun createRoom(hostName: String): Result<GameRoom> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")
            
            // Generate unique room code
            var roomCode: String
            do {
                roomCode = RoomCodeGenerator.generate()
            } while (isRoomCodeTaken(roomCode))
            
            // Create room ID
            val roomId = roomsRef.push().key 
                ?: throw IllegalStateException("Failed to generate room ID")
            
            // Create host player
            val hostPlayer = Player(
                id = userId,
                name = hostName,
                color = PlayerColor.RED,
                isOnline = true,
                isReady = true
            )
            
            // Create room
            val room = GameRoom(
                roomId = roomId,
                roomCode = roomCode,
                hostId = userId,
                hostName = hostName,
                status = RoomStatus.WAITING,
                players = mapOf(userId to hostPlayer)
            )
            
            // Save room and room code mapping
            roomsRef.child(roomId).setValue(room).await()
            roomCodesRef.child(roomCode).setValue(roomId).await()
            
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Joins an existing room using room code.
     */
    suspend fun joinRoom(roomCode: String, playerName: String): Result<GameRoom> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")
            
            val normalizedCode = RoomCodeGenerator.normalize(roomCode)
            
            if (!RoomCodeGenerator.isValid(normalizedCode)) {
                throw IllegalArgumentException("Invalid room code format")
            }
            
            // Get room ID from code
            val roomId = roomCodesRef.child(normalizedCode).get().await()
                .getValue(String::class.java)
                ?: throw IllegalArgumentException("Room not found")
            
            // Get current room data
            val roomSnapshot = roomsRef.child(roomId).get().await()
            val room = roomSnapshot.getValue(GameRoom::class.java)
                ?: throw IllegalArgumentException("Room not found")
            
            // Validate room state
            when {
                room.status != RoomStatus.WAITING -> 
                    throw IllegalStateException("Game already started")
                room.isFull() -> 
                    throw IllegalStateException("Room is full")
                room.players.containsKey(userId) -> 
                    throw IllegalStateException("Already in this room")
            }
            
            // Assign available color
            val availableColor = room.getAvailableColors().firstOrNull()
                ?: throw IllegalStateException("No colors available")
            
            // Create player
            val player = Player(
                id = userId,
                name = playerName,
                color = availableColor,
                isOnline = true,
                isReady = false
            )
            
            // Add player to room
            roomsRef.child(roomId).child("players").child(userId)
                .setValue(player).await()
            
            // Return updated room
            val updatedSnapshot = roomsRef.child(roomId).get().await()
            val updatedRoom = updatedSnapshot.getValue(GameRoom::class.java)!!
            
            Result.success(updatedRoom)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observes room changes in real-time.
     */
    fun observeRoom(roomId: String): Flow<GameRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(GameRoom::class.java)
                trySend(room)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        roomsRef.child(roomId).addValueEventListener(listener)
        
        awaitClose {
            roomsRef.child(roomId).removeEventListener(listener)
        }
    }
    
    /**
     * Gets room by code (one-time fetch).
     */
    suspend fun getRoomByCode(roomCode: String): Result<GameRoom> {
        return try {
            val normalizedCode = RoomCodeGenerator.normalize(roomCode)
            
            val roomId = roomCodesRef.child(normalizedCode).get().await()
                .getValue(String::class.java)
                ?: throw IllegalArgumentException("Room not found")
            
            val room = roomsRef.child(roomId).get().await()
                .getValue(GameRoom::class.java)
                ?: throw IllegalArgumentException("Room not found")
            
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leaves a room.
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")
            
            val roomSnapshot = roomsRef.child(roomId).get().await()
            val room = roomSnapshot.getValue(GameRoom::class.java)
                ?: throw IllegalArgumentException("Room not found")
            
            // Remove player
            roomsRef.child(roomId).child("players").child(userId)
                .removeValue().await()
            
            // If host leaves or room empty, handle cleanup
            val remainingPlayers = room.players.size - 1
            if (remainingPlayers == 0) {
                // Delete room
                roomsRef.child(roomId).removeValue().await()
                roomCodesRef.child(room.roomCode).removeValue().await()
            } else if (room.hostId == userId) {
                // Transfer host to next player
                val newHostId = room.players.keys.first { it != userId }
                roomsRef.child(roomId).child("hostId").setValue(newHostId).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sets player ready status.
     */
    suspend fun setPlayerReady(roomId: String, isReady: Boolean): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")
            
            roomsRef.child(roomId).child("players").child(userId)
                .child("isReady").setValue(isReady).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Starts the game (host only).
     */
    suspend fun startGame(roomId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")
            
            val room = roomsRef.child(roomId).get().await()
                .getValue(GameRoom::class.java)
                ?: throw IllegalArgumentException("Room not found")
            
            if (room.hostId != userId) {
                throw IllegalStateException("Only host can start the game")
            }
            
            if (!room.canStart()) {
                throw IllegalStateException("Need at least 2 players to start")
            }
            
            roomsRef.child(roomId).child("status")
                .setValue(RoomStatus.IN_PROGRESS).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if a room code is already taken.
     */
    private suspend fun isRoomCodeTaken(code: String): Boolean {
        val snapshot = roomCodesRef.child(code).get().await()
        return snapshot.exists()
    }
}
