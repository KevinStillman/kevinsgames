package com.keviqn.kevinsgames.data

import android.content.Context
import android.content.SharedPreferences

data class GameStats(
    val highScore: Int = 0,
    val gamesPlayed: Int = 0,
    val bestRound: Int = 0
)

class GameStatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)

    fun getStats(gameId: Int): GameStats {
        return GameStats(
            highScore = prefs.getInt("game_${gameId}_high_score", 0),
            gamesPlayed = prefs.getInt("game_${gameId}_games_played", 0),
            bestRound = prefs.getInt("game_${gameId}_best_round", 0)
        )
    }

    fun updateStats(gameId: Int, score: Int, round: Int) {
        val currentStats = getStats(gameId)
        
        prefs.edit().apply {
            // Update high score if current score is higher
            if (score > currentStats.highScore) {
                putInt("game_${gameId}_high_score", score)
            }
            
            // Increment games played
            putInt("game_${gameId}_games_played", currentStats.gamesPlayed + 1)
            
            // Update best round if current round is higher
            if (round > currentStats.bestRound) {
                putInt("game_${gameId}_best_round", round)
            }
            
            apply()
        }
    }

    fun resetStats(gameId: Int) {
        prefs.edit().apply {
            remove("game_${gameId}_high_score")
            remove("game_${gameId}_games_played")
            remove("game_${gameId}_best_round")
            apply()
        }
    }
}
