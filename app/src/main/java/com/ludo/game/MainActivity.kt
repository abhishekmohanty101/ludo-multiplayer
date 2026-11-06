package com.ludo.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.ludo.game.ui.navigation.LudoNavGraph
import com.ludo.game.ui.theme.LudoGameTheme
import com.ludo.game.util.RoomShareManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LudoGameTheme {
                val navController = rememberNavController()
                var deepLinkRoomCode by remember { mutableStateOf<String?>(null) }
                
                // Handle deep link on launch
                LaunchedEffect(Unit) {
                    intent?.let { handleDeepLink(it) }?.let { roomCode ->
                        deepLinkRoomCode = roomCode
                    }
                }
                
                LudoNavGraph(
                    navController = navController,
                    deepLinkRoomCode = deepLinkRoomCode,
                    onDeepLinkHandled = { deepLinkRoomCode = null }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        handleDeepLink(intent)?.let { roomCode ->
            // Navigate to join room with this code
            // You can use a shared ViewModel or event bus to handle this
        }
    }
    
    /**
     * Extracts room code from deep link intent.
     */
    private fun handleDeepLink(intent: Intent): String? {
        if (intent.action != Intent.ACTION_VIEW) return null
        
        val uri = intent.data ?: return null
        return RoomShareManager.extractRoomCode(uri)
    }
}
