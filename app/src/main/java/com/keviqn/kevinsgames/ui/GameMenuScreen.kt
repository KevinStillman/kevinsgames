package com.keviqn.kevinsgames.ui

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
fun GameMenuScreen(
    game: Game,
    onBack: () -> Unit,
    onPlay: () -> Unit
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
                    text = "🎮",
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
            
            // Play Button
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = game.color
                )
            ) {
                Text(
                    text = "PLAY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
