package com.ludo.game.data.model

/**
 * Chat message in a game room.
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "😀",
    val senderColor: String = "RED",
    val message: String = "",
    val messageType: ChatMessageType = ChatMessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of chat messages.
 */
enum class ChatMessageType {
    TEXT,           // Regular text message
    EMOJI,          // Single emoji reaction
    SYSTEM,         // System notification (player joined, etc.)
    GAME_EVENT      // Game event (captured, rolled 6, etc.)
}

/**
 * Quick emoji reactions available in game.
 */
object QuickReactions {
    val reactions = listOf(
        "👍", "👎", "😂", "😮", "😢", "😡",
        "🎉", "👏", "🔥", "💪", "🤔", "😈"
    )
    
    val gameReactions = mapOf(
        "nice_roll" to "🎲🔥",
        "captured" to "💥",
        "almost" to "😅",
        "winner" to "🏆",
        "good_luck" to "🍀",
        "hurry_up" to "⏰"
    )
}
