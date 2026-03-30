package com.keviqn.kevinsgames.model

import androidx.compose.ui.graphics.Color

data class Game(
    val id: Int,
    val title: String,
    val description: String,
    val color: Color
)

val placeholderGames = listOf(
    Game(1, "Space Shooter",  "Drag to dodge, shoot to destroy!", Color(0xFF1B5E20)),
    Game(2, "Snake",          "Eat and grow!",                    Color(0xFF2E7D32)),
    Game(3, "Memory Match",   "Flip cards to find pairs",         Color(0xFFB5200E)),
    Game(4, "Pong",           "Paddle and ball action",           Color(0xFF1565C0)),
    Game(5, "2048",           "Merge tiles to win",               Color(0xFFE65100)),
    Game(6, "Minesweeper",    "Avoid the mines",                  Color(0xFF4A148C)),
    Game(7, "Hangman",        "Guess the word",                   Color(0xFF00695C)),
    Game(8, "Breakout",       "Smash bricks with a ball",         Color(0xFF827717)),
)
