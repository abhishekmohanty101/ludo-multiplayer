package com.ludo.game.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ludo.game.ui.screens.game.GameScreen
import com.ludo.game.ui.screens.home.HomeScreen
import com.ludo.game.ui.screens.lobby.WaitingRoomScreen
import com.ludo.game.ui.screens.history.HistoryScreen
import com.ludo.game.ui.screens.history.ReplayScreen

/**
 * Navigation routes.
 */
object Routes {
    const val HOME = "home"
    const val CREATE_ROOM = "create_room/{playerName}"
    const val JOIN_ROOM = "join_room/{playerName}/{roomCode}"
    const val WAITING_ROOM = "waiting_room"
    const val GAME = "game/{roomId}"
    const val HISTORY = "history"
    const val REPLAY = "replay/{gameId}"
    
    fun createRoom(playerName: String) = "create_room/$playerName"
    fun joinRoom(playerName: String, roomCode: String) = "join_room/$playerName/$roomCode"
    fun game(roomId: String) = "game/$roomId"
    fun replay(gameId: String) = "replay/$gameId"
}

/**
 * Main navigation graph.
 */
@Composable
fun LudoNavGraph(
    navController: NavHostController,
    deepLinkRoomCode: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    // Handle deep link navigation
    LaunchedEffect(deepLinkRoomCode) {
        deepLinkRoomCode?.let { code ->
            // Navigate to join room with the code
            // User will still need to enter their name
            navController.navigate("join_room/Player/$code")
            onDeepLinkHandled()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                onCreateGame = { playerName ->
                    navController.navigate(Routes.createRoom(playerName))
                },
                onJoinGame = { nameAndCode ->
                    val parts = nameAndCode.split("|")
                    if (parts.size == 2) {
                        navController.navigate(Routes.joinRoom(parts[0], parts[1]))
                    }
                },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }
        
        // Create Room -> Waiting Room
        composable(
            route = Routes.CREATE_ROOM,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            
            // This would typically trigger room creation in ViewModel
            // For now, navigate to waiting room
            WaitingRoomScreen(
                onBackClick = { navController.popBackStack() },
                onGameStart = { roomId ->
                    navController.navigate(Routes.game(roomId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        
        // Join Room -> Waiting Room
        composable(
            route = Routes.JOIN_ROOM,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("roomCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            
            WaitingRoomScreen(
                onBackClick = { navController.popBackStack() },
                onGameStart = { roomId ->
                    navController.navigate(Routes.game(roomId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        
        // Waiting Room
        composable(Routes.WAITING_ROOM) {
            WaitingRoomScreen(
                onBackClick = { navController.popBackStack() },
                onGameStart = { roomId ->
                    navController.navigate(Routes.game(roomId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        
        // Game Screen
        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) {
            GameScreen(
                onBackClick = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onGameEnd = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        
        // History Screen
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onReplayGame = { gameId ->
                    navController.navigate(Routes.replay(gameId))
                }
            )
        }
        
        // Replay Screen
        composable(
            route = Routes.REPLAY,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType }
            )
        ) {
            ReplayScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
