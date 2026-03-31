package com.keviqn.kevinsgames.ui.reflextiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

// ── Constants ────────────────────────────────────────────────────────────────

const val GRID_SIZE = 5
const val TILES_COUNT = GRID_SIZE * GRID_SIZE

// Difficulty settings (time in seconds to tap a black tile)
enum class Difficulty(val timeLimit: Float, val displayName: String) {
    EASY(1.0f, "Easy"),
    MEDIUM(0.75f, "Medium"),
    HARD(0.5f, "Hard")
}

// ── Data classes ─────────────────────────────────────────────────────────────

data class TileState(
    val index: Int,
    val isBlack: Boolean = false
)

// ── Game state ───────────────────────────────────────────────────────────────

class ReflexTilesGameState(
    private val difficulty: Difficulty,
    private val onGameOver: ((score: Int, round: Int) -> Unit)? = null
) {

    // Tiles
    var tiles by mutableStateOf(List(TILES_COUNT) { TileState(it) })
    
    // Game state
    var score by mutableStateOf(0)
    var round by mutableStateOf(1)
    var gameOver by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var gameStarted by mutableStateOf(false)
    
    // Timer for current black tile
    var timeRemaining by mutableStateOf(difficulty.timeLimit)
    private var currentBlackTileIndex: Int? = null
    
    // ── Initialise ────────────────────────────────────────────────────────────
    
    init {
        spawnBlackTile()
    }
    
    // ── Spawn black tile ──────────────────────────────────────────────────────
    
    private fun spawnBlackTile() {
        // Clear all tiles first
        tiles = List(TILES_COUNT) { TileState(it, false) }
        
        // Pick a random tile to be black
        val randomIndex = Random.nextInt(TILES_COUNT)
        currentBlackTileIndex = randomIndex
        
        tiles = tiles.mapIndexed { index, tile ->
            tile.copy(isBlack = index == randomIndex)
        }
        
        // Reset timer
        timeRemaining = difficulty.timeLimit
    }
    
    // ── Tick ──────────────────────────────────────────────────────────────────
    
    fun tick(dt: Float) {
        if (gameOver || isPaused || !gameStarted) return
        
        // Countdown timer
        timeRemaining -= dt
        
        // If time runs out, game over
        if (timeRemaining <= 0f) {
            gameOver = true
            onGameOver?.invoke(score, round)
        }
    }
    
    // ── Tile tap ──────────────────────────────────────────────────────────────
    
    fun onTileTap(index: Int) {
        if (gameOver || isPaused) return
        
        val tile = tiles.getOrNull(index) ?: return
        
        if (tile.isBlack) {
            // Start the game on first tap
            if (!gameStarted) {
                gameStarted = true
            }
            
            // Correct tap!
            score++
            round++
            spawnBlackTile()
        } else {
            // Wrong tap - game over
            gameOver = true
            onGameOver?.invoke(score, round)
        }
    }
    
    // ── Helpers ───────────────────────────────────────────────────────────────
    
    fun togglePause() {
        isPaused = !isPaused
    }
    
    fun reset() {
        score = 0
        round = 1
        gameOver = false
        isPaused = false
        gameStarted = false
        spawnBlackTile()
    }
}
