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
    Game(2, "Ball Buster",    "Break boxes with bouncing balls!", Color(0xFF2E7D32)),
    Game(3, "Gate Walker",    "Run forward, choose gates wisely!", Color(0xFF6A1B9A)),
    Game(4, "Reflex Tiles",   "Tap black tiles before time runs out!", Color(0xFFD84315))
)
