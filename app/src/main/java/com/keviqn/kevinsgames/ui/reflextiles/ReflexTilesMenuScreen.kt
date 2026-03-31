package com.keviqn.kevinsgames.ui.reflextiles

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keviqn.kevinsgames.data.GameStatsManager
import com.keviqn.kevinsgames.model.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflexTilesMenuScreen(
    game: Game,
    onBack: () -> Unit,
    onPlayWithDifficulty: (Difficulty) -> Unit
) {
    val context = LocalContext.current
    val statsManager = remember { GameStatsManager(context) }
    val stats = remember { statsManager.getStats(game.id) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = game.color,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Stats Section
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 72.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Statistics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // High Score
                        StatRow(label = "High Score", value = stats.highScore.toString())
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Games Played
                        StatRow(label = "Games Played", value = stats.gamesPlayed.toString())
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Best Round
                        StatRow(label = "Best Round", value = stats.bestRound.toString())
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Difficulty Selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Easy Button
                Button(
                    onClick = { onPlayWithDifficulty(Difficulty.EASY) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EASY",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1.0 second per tile",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Medium Button
                Button(
                    onClick = { onPlayWithDifficulty(Difficulty.MEDIUM) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MEDIUM",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0.75 seconds per tile",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Hard Button
                Button(
                    onClick = { onPlayWithDifficulty(Difficulty.HARD) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFD32F2F)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HARD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0.5 seconds per tile",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
