package com.keviqn.kevinsgames.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keviqn.kevinsgames.ui.ballbuster.BallBusterGameScreen
import com.keviqn.kevinsgames.ui.gatewalker.GateWalkerGameScreen
import com.keviqn.kevinsgames.ui.shooter.ShooterGameScreen
import com.keviqn.kevinsgames.ui.reflextiles.ReflexTilesMenuScreen
import com.keviqn.kevinsgames.ui.reflextiles.ReflexTilesGameScreen
import com.keviqn.kevinsgames.ui.reflextiles.Difficulty

private const val ROUTE_HOME = "home"
private const val ROUTE_GAME_MENU = "gameMenu/{gameId}"
private const val ROUTE_GAME = "game/{gameId}"
private const val ROUTE_REFLEX_TILES_GAME = "reflexTilesGame/{difficulty}"

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            HomeScreen(
                onGameClick = { game ->
                    navController.navigate("gameMenu/${game.id}")
                }
            )
        }

        composable(
            route = ROUTE_GAME_MENU,
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: return@composable
            val game = com.keviqn.kevinsgames.model.placeholderGames.first { it.id == gameId }
            
            // Special handling for Reflex Tiles (game 4) - show difficulty selection
            if (gameId == 4) {
                ReflexTilesMenuScreen(
                    game = game,
                    onBack = { navController.popBackStack() },
                    onPlayWithDifficulty = { difficulty ->
                        navController.navigate("reflexTilesGame/${difficulty.name}")
                    }
                )
            } else {
                GameMenuScreen(
                    game = game,
                    onBack = { navController.popBackStack() },
                    onPlay = { navController.navigate("game/$gameId") }
                )
            }
        }

        composable(
            route = ROUTE_GAME,
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: return@composable
            when (gameId) {
                1    -> ShooterGameScreen(onBack = { navController.popBackStack() })
                2    -> BallBusterGameScreen(onBack = { navController.popBackStack() })
                3    -> GateWalkerGameScreen(onBack = { navController.popBackStack() })
                else -> GameScreen(gameId = gameId, onBack = { navController.popBackStack() })
            }
        }

        composable(
            route = ROUTE_REFLEX_TILES_GAME,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
        ) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: return@composable
            val difficulty = Difficulty.valueOf(difficultyName)
            ReflexTilesGameScreen(
                difficulty = difficulty,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
