package com.keviqn.kevinsgames.ui.reflextiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keviqn.kevinsgames.data.GameStatsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflexTilesGameScreen(
    difficulty: Difficulty,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val statsManager = remember { GameStatsManager(context) }
    val state = remember {
        ReflexTilesGameState(
            difficulty = difficulty,
            onGameOver = { score, round ->
                statsManager.updateStats(gameId = 4, score = score, round = round)
            }
        )
    }

    LaunchedEffect(Unit) {
        var lastTime = -1L
        while (true) {
            withFrameNanos { nanos ->
                if (lastTime < 0L) { lastTime = nanos; return@withFrameNanos }
                val dt = ((nanos - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
                lastTime = nanos
                state.tick(dt)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reflex Tiles - ${difficulty.displayName}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.gameOver) {
                        IconButton(onClick = { state.togglePause() }) {
                            Icon(
                                imageVector = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (state.isPaused) "Resume" else "Pause"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD84315),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1A1A1A))
        ) {
            if (!state.gameOver) {
                // Game UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Score and Timer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Score: ${state.score}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Time: ${"%.2f".format(state.timeRemaining)}s",
                            color = if (state.timeRemaining < 0.2f) Color.Red else Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 5x5 Grid
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until GRID_SIZE) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until GRID_SIZE) {
                                    val index = row * GRID_SIZE + col
                                    val tile = state.tiles[index]
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(
                                                if (tile.isBlack) Color.Black else Color(0xFF424242)
                                            )
                                            .border(2.dp, Color(0xFF616161))
                                            .clickable(enabled = !state.isPaused) {
                                                state.onTileTap(index)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Empty tile
                                    }
                                }
                            }
                        }
                    }
                }

                // Pause overlay
                if (state.isPaused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PAUSED",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Game Over screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { state.reset() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "GAME OVER",
                            color = Color.Red,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2A2A2A)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Difficulty",
                                    color = Color(0xFFBBBBBB),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = difficulty.displayName,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Score",
                                    color = Color(0xFFBBBBBB),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${state.score}",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Tiles Tapped",
                                    color = Color(0xFFBBBBBB),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${state.round - 1}",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Tap to Restart",
                            color = Color(0xFFD84315),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
