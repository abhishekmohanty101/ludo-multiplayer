package com.ludo.game.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.ludo.game.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all game sound effects.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private var soundsLoaded = false
    
    // Sound IDs
    private var diceRollSound: Int = 0
    private var tokenMoveSound: Int = 0
    private var tokenCaptureSound: Int = 0
    private var tokenHomeSound: Int = 0
    private var winSound: Int = 0
    private var turnStartSound: Int = 0
    private var buttonClickSound: Int = 0
    private var errorSound: Int = 0
    private var playerJoinSound: Int = 0
    private var chatMessageSound: Int = 0
    
    // Settings
    private var soundEnabled = true
    private var volume = 1.0f
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) soundsLoaded = true
                }
            }
        
        // Load sounds (these will need actual sound files in res/raw/)
        soundPool?.let { pool ->
            try {
                diceRollSound = pool.load(context, R.raw.dice_roll, 1)
                tokenMoveSound = pool.load(context, R.raw.token_move, 1)
                tokenCaptureSound = pool.load(context, R.raw.token_capture, 1)
                tokenHomeSound = pool.load(context, R.raw.token_home, 1)
                winSound = pool.load(context, R.raw.win, 1)
                turnStartSound = pool.load(context, R.raw.turn_start, 1)
                buttonClickSound = pool.load(context, R.raw.button_click, 1)
                errorSound = pool.load(context, R.raw.error, 1)
                playerJoinSound = pool.load(context, R.raw.player_join, 1)
                chatMessageSound = pool.load(context, R.raw.chat_message, 1)
            } catch (e: Exception) {
                // Sound files not found - sounds will be disabled
                soundsLoaded = false
            }
        }
    }
    
    private fun play(soundId: Int, rate: Float = 1.0f) {
        if (!soundEnabled || !soundsLoaded || soundId == 0) return
        soundPool?.play(soundId, volume, volume, 1, 0, rate)
    }
    
    // Public methods for playing specific sounds
    
    fun playDiceRoll() = play(diceRollSound)
    
    fun playTokenMove() = play(tokenMoveSound)
    
    fun playTokenCapture() = play(tokenCaptureSound, 1.2f)
    
    fun playTokenReachedHome() = play(tokenHomeSound)
    
    fun playWin() = play(winSound)
    
    fun playTurnStart() = play(turnStartSound, 0.8f)
    
    fun playButtonClick() = play(buttonClickSound, 1.5f)
    
    fun playError() = play(errorSound)
    
    fun playPlayerJoin() = play(playerJoinSound)
    
    fun playChatMessage() = play(chatMessageSound)
    
    // Settings
    
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }
    
    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
    }
    
    fun isSoundEnabled(): Boolean = soundEnabled
    
    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

/**
 * Sound effect types for easy reference.
 */
enum class GameSound {
    DICE_ROLL,
    TOKEN_MOVE,
    TOKEN_CAPTURE,
    TOKEN_HOME,
    WIN,
    TURN_START,
    BUTTON_CLICK,
    ERROR,
    PLAYER_JOIN,
    CHAT_MESSAGE
}
