package com.ludo.game.ui.navigation

import androidx.compose.runtime.Composable
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
    const val GAME = "game/{roomId}"
    const val HISTORY = "history"
    const val REPLAY = "replay/{gameId}"
    
    fun createRoom(playerName: String) = "create_room/$playerName"
    fun joinRoom(playerName: String, roomCode: String) = "join_room/$playerName/$roomCode"
    fun game(roomId: String) = "game/$roomId"
    fun replay(gameId: String) = "replay/$gameId"
}

/**
 * Main navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                onCreateGame = { playerName ->
                    navController.navigate(Routes.createRoom(playerName))
                },
                onJoinGame = { playerName, roomCode ->
                    navController.navigate(Routes.joinRoom(playerName, roomCode))
                },
                onViewHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }
        
        // Create Room - goes to WaitingRoomScreen with playerName (ViewModel auto-creates room)
        composable(
            route = Routes.CREATE_ROOM,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType }
            )
        ) {
            WaitingRoomScreen(
                onBackClick = { navController.popBackStack() },
                onGameStart = { roomId ->
                    navController.navigate(Routes.game(roomId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }
        
        // Join Room - goes to WaitingRoomScreen with playerName and roomCode (ViewModel auto-joins)
        composable(
            route = Routes.JOIN_ROOM,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("roomCode") { type = NavType.StringType }
            )
        ) {
            WaitingRoomScreen(
                onBackClick = { navController.popBackStack() },
                onGameStart = { roomId ->
                    navController.navigate(Routes.game(roomId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
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
                onGameEnd = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
        
        // History Screen
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onReplayClick = { gameId ->
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
