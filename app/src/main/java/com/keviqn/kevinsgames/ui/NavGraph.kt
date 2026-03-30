package com.keviqn.kevinsgames.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keviqn.kevinsgames.ui.shooter.ShooterGameScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_GAME = "game/{gameId}"

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            HomeScreen(
                onGameClick = { game ->
                    navController.navigate("game/${game.id}")
                }
            )
        }

        composable(
            route = ROUTE_GAME,
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: return@composable
            when (gameId) {
                1    -> ShooterGameScreen(onBack = { navController.popBackStack() })
                else -> GameScreen(gameId = gameId, onBack = { navController.popBackStack() })
            }
        }
    }
}
