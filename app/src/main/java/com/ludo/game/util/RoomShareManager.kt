package com.ludo.game.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Handles generation and sharing of room invite links.
 */
object RoomShareManager {
    
    // App's custom scheme for deep linking
    private const val APP_SCHEME = "ludogame"
    
    /**
     * Generates a deep link URI that opens the app directly.
     * Example: ludogame://join?code=ABC123
     */
    fun generateDeepLink(roomCode: String): Uri {
        return Uri.parse("$APP_SCHEME://join?code=$roomCode")
    }
    
    /**
     * Creates the share message with room code.
     */
    fun createShareMessage(roomCode: String, hostName: String = "a friend"): String {
        return """
            |🎲 Join my Ludo game!
            |
            |Room Code: $roomCode
            |
            |Open the Ludo app and enter this code to join!
        """.trimMargin()
    }
    
    /**
     * Opens Android share sheet to share room invite.
     */
    fun shareRoomInvite(context: Context, roomCode: String, hostName: String = "Player") {
        val shareMessage = createShareMessage(roomCode, hostName)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join my Ludo game!")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share room invite via")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
    
    /**
     * Copies room code to clipboard.
     */
    fun copyToClipboard(context: Context, roomCode: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Room Code", roomCode)
        clipboard.setPrimaryClip(clip)
    }
    
    /**
     * Extracts room code from a deep link URI.
     * Handles: ludogame://join?code=ABC123
     */
    fun extractRoomCode(uri: Uri): String? {
        // Try path format: /join/ABC123
        if (uri.pathSegments.size >= 2 && uri.pathSegments[0] == "join") {
            return uri.pathSegments[1]
        }
        
        // Try query format: ?code=ABC123
        return uri.getQueryParameter("code")
    }
}
