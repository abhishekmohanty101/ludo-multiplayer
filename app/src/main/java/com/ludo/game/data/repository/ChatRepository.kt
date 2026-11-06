package com.ludo.game.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ludo.game.data.model.ChatMessage
import com.ludo.game.data.model.ChatMessageType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing chat messages in Firebase.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val chatsRef = database.getReference("chats")
    
    /**
     * Send a chat message.
     */
    suspend fun sendMessage(
        roomId: String,
        message: String,
        senderName: String,
        senderAvatar: String,
        senderColor: String,
        messageType: ChatMessageType = ChatMessageType.TEXT
    ): Result<ChatMessage> {
        return try {
            val userId = auth.currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            
            val messageId = chatsRef.child(roomId).push().key
                ?: throw IllegalStateException("Failed to generate message ID")
            
            val chatMessage = ChatMessage(
                id = messageId,
                senderId = userId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                senderColor = senderColor,
                message = message,
                messageType = messageType,
                timestamp = System.currentTimeMillis()
            )
            
            chatsRef.child(roomId).child(messageId).setValue(chatMessage).await()
            
            Result.success(chatMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a quick emoji reaction.
     */
    suspend fun sendReaction(
        roomId: String,
        emoji: String,
        senderName: String,
        senderAvatar: String,
        senderColor: String
    ): Result<ChatMessage> {
        return sendMessage(
            roomId = roomId,
            message = emoji,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderColor = senderColor,
            messageType = ChatMessageType.EMOJI
        )
    }
    
    /**
     * Send a system message (player joined, etc.)
     */
    suspend fun sendSystemMessage(
        roomId: String,
        message: String
    ): Result<ChatMessage> {
        return try {
            val messageId = chatsRef.child(roomId).push().key
                ?: throw IllegalStateException("Failed to generate message ID")
            
            val chatMessage = ChatMessage(
                id = messageId,
                senderId = "system",
                senderName = "System",
                senderAvatar = "🎮",
                senderColor = "BLUE",
                message = message,
                messageType = ChatMessageType.SYSTEM,
                timestamp = System.currentTimeMillis()
            )
            
            chatsRef.child(roomId).child(messageId).setValue(chatMessage).await()
            
            Result.success(chatMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a game event message (captured token, etc.)
     */
    suspend fun sendGameEvent(
        roomId: String,
        event: String,
        playerName: String,
        playerAvatar: String,
        playerColor: String
    ): Result<ChatMessage> {
        return sendMessage(
            roomId = roomId,
            message = event,
            senderName = playerName,
            senderAvatar = playerAvatar,
            senderColor = playerColor,
            messageType = ChatMessageType.GAME_EVENT
        )
    }
    
    /**
     * Observe chat messages in real-time.
     */
    fun observeMessages(roomId: String, limit: Int = 50): Flow<List<ChatMessage>> = callbackFlow {
        val query = chatsRef.child(roomId)
            .orderByChild("timestamp")
            .limitToLast(limit)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    child.getValue(ChatMessage::class.java)
                }.sortedBy { it.timestamp }
                
                trySend(messages)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        
        awaitClose {
            query.removeEventListener(listener)
        }
    }
    
    /**
     * Delete old messages (cleanup).
     */
    suspend fun cleanupOldMessages(roomId: String, keepLast: Int = 100): Result<Unit> {
        return try {
            val snapshot = chatsRef.child(roomId)
                .orderByChild("timestamp")
                .get()
                .await()
            
            val allMessages = snapshot.children.toList()
            
            if (allMessages.size > keepLast) {
                val toDelete = allMessages.take(allMessages.size - keepLast)
                toDelete.forEach { it.ref.removeValue() }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
