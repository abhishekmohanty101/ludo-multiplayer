package com.ludo.game.util

import java.security.SecureRandom

/**
 * Generates unique room codes for multiplayer game sessions.
 * 
 * Uses alphanumeric characters excluding confusing ones (0, O, 1, I, L)
 * to make codes easier to read and share verbally.
 */
object RoomCodeGenerator {
    
    // Characters that are easy to distinguish (no 0/O, 1/I/L confusion)
    private const val ALLOWED_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 6
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generates a random 6-character room code.
     * Example: "ABC123", "XY7K9M"
     */
    fun generate(): String {
        return (1..CODE_LENGTH)
            .map { ALLOWED_CHARS[secureRandom.nextInt(ALLOWED_CHARS.length)] }
            .joinToString("")
    }
    
    /**
     * Validates if a given code matches the expected format.
     */
    fun isValid(code: String): Boolean {
        if (code.length != CODE_LENGTH) return false
        return code.all { it in ALLOWED_CHARS }
    }
    
    /**
     * Normalizes user input (uppercase, trim whitespace).
     */
    fun normalize(input: String): String {
        return input.trim().uppercase().filter { it.isLetterOrDigit() }
    }
}
