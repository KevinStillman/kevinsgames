package com.keviqn.kevinsgames.ui.gatewalker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keviqn.kevinsgames.data.GameStatsManager
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateWalkerGameScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val statsManager = remember { GameStatsManager(context) }
    val state = remember {
        GateWalkerGameState(
            onGameOver = { score, level ->
                statsManager.updateStats(gameId = 3, score = score, round = level)
            }
        )
    }
    val textMeasurer = rememberTextMeasurer()

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
                title = { Text("Gate Walker", fontWeight = FontWeight.Bold) },
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
                    containerColor = Color(0xFF6A1B9A),
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
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            if (state.gameOver) {
                                state.reset()
                                return@awaitEachGesture
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) break
                                val drag = event.changes.firstOrNull { it.pressed } ?: break
                                state.movePlayer(drag.positionChange().x)
                                drag.consume()
                            }
                        }
                    }
            ) {
                state.initCanvas(size.width, size.height)

                // Background - gradient road effect
                drawRect(color = Color(0xFF1A1A2E), size = size)
                
                // Draw road lines for depth effect
                drawRoadLines(state)

                if (!state.gameOver) {
                    if (state.bossActive && state.boss != null) {
                        // Draw boss
                        drawBoss(state.boss!!, textMeasurer)
                        // Draw player crowd
                        drawCrowd(state)
                    } else {
                        // Draw gates
                        state.gates.forEach { gate ->
                            drawGate(gate, state.distanceTraveled, textMeasurer)
                        }

                        // Draw obstacles
                        state.obstacles.forEach { obstacle ->
                            drawObstacle(obstacle, state.distanceTraveled)
                        }

                        // Draw enemies
                        state.enemies.forEach { enemy ->
                            drawEnemy(enemy, state.distanceTraveled, textMeasurer)
                        }

                        // Draw player crowd
                        drawCrowd(state)
                    }
                }

                // HUD & Game Over
                drawHud(state, textMeasurer)
            }
        }
    }
}

// ── Draw helpers ──────────────────────────────────────────────────────────────

private fun DrawScope.drawRoadLines(state: GateWalkerGameState) {
    val lineSpacing = 100f
    val lineHeight = 40f
    val lineWidth = 8f
    
    // Draw dashed center lines for road effect
    var y = (state.distanceTraveled % lineSpacing)
    while (y < size.height) {
        // Left lane line
        drawRect(
            color = Color(0x44FFFFFF),
            topLeft = Offset(size.width * 0.25f - lineWidth / 2, y),
            size = Size(lineWidth, lineHeight)
        )
        // Right lane line
        drawRect(
            color = Color(0x44FFFFFF),
            topLeft = Offset(size.width * 0.75f - lineWidth / 2, y),
            size = Size(lineWidth, lineHeight)
        )
        y += lineSpacing
    }
}

private fun DrawScope.drawGate(gate: Gate, distanceTraveled: Float, measurer: TextMeasurer) {
    val screenY = distanceTraveled - gate.y
    
    // Gate takes up 50% of screen width
    val gateWidth = size.width * 0.5f
    
    // Gate color based on type
    val (gateColor, textColor) = when (gate.type) {
        GateType.ADD -> Color(0xFF4CAF50) to Color.White  // Green
        GateType.MULTIPLY -> Color(0xFF2196F3) to Color.White  // Blue
        GateType.SUBTRACT -> Color(0xFFE53935) to Color.White  // Red
        GateType.DIVIDE -> Color(0xFFFF6F00) to Color.White  // Orange
    }
    
    // Draw gate posts
    val postWidth = 12f
    val leftX = gate.x - gateWidth / 2
    val rightX = gate.x + gateWidth / 2 - postWidth
    
    drawRect(
        color = gateColor,
        topLeft = Offset(leftX, screenY),
        size = Size(postWidth, GATE_HEIGHT)
    )
    drawRect(
        color = gateColor,
        topLeft = Offset(rightX, screenY),
        size = Size(postWidth, GATE_HEIGHT)
    )
    
    // Draw top bar
    drawRect(
        color = gateColor,
        topLeft = Offset(leftX, screenY),
        size = Size(gateWidth, 12f)
    )
    
    // Draw gate effect text
    val symbol = when (gate.type) {
        GateType.ADD -> "+"
        GateType.MULTIPLY -> "×"
        GateType.SUBTRACT -> "-"
        GateType.DIVIDE -> "÷"
    }
    val text = "$symbol${gate.value}"
    val textStyle = TextStyle(
        color = textColor,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    val textLayout = measurer.measure(text, textStyle)
    drawText(
        textLayout,
        topLeft = Offset(
            gate.x - textLayout.size.width / 2,
            screenY + GATE_HEIGHT / 2 - textLayout.size.height / 2
        )
    )
}

private fun DrawScope.drawObstacle(obstacle: Obstacle, distanceTraveled: Float) {
    val screenY = distanceTraveled - obstacle.y
    
    when (obstacle.type) {
        ObstacleType.SPINNING_BAR -> {
            // Draw rotating bar
            rotate(obstacle.rotation, pivot = Offset(obstacle.x + obstacle.size / 2, screenY + obstacle.size / 2)) {
                drawRect(
                    color = Color(0xFFFF5252),
                    topLeft = Offset(obstacle.x, screenY + obstacle.size / 2 - 8f),
                    size = Size(obstacle.size, 16f)
                )
            }
            // Center pivot
            drawCircle(
                color = Color(0xFF424242),
                radius = 12f,
                center = Offset(obstacle.x + obstacle.size / 2, screenY + obstacle.size / 2)
            )
        }
        ObstacleType.WALL -> {
            // Draw solid wall
            drawRect(
                color = Color(0xFF757575),
                topLeft = Offset(obstacle.x, screenY),
                size = Size(obstacle.size, obstacle.size)
            )
            // Highlight
            drawRect(
                color = Color(0xFF9E9E9E),
                topLeft = Offset(obstacle.x, screenY),
                size = Size(obstacle.size, 8f)
            )
        }
        ObstacleType.MOVING_HAZARD -> {
            // Draw pulsing hazard
            val pulseOffset = (obstacle.rotation % 60f) / 60f * 10f
            drawCircle(
                color = Color(0xFFFF6F00),
                radius = obstacle.size / 2 + pulseOffset,
                center = Offset(obstacle.x + obstacle.size / 2, screenY + obstacle.size / 2)
            )
            drawCircle(
                color = Color(0xFFFFAB40),
                radius = obstacle.size / 3,
                center = Offset(obstacle.x + obstacle.size / 2, screenY + obstacle.size / 2)
            )
        }
    }
}

private fun DrawScope.drawEnemy(enemy: Enemy, distanceTraveled: Float, measurer: TextMeasurer) {
    val screenY = distanceTraveled - enemy.y
    
    // Draw enemy body
    drawRect(
        color = Color(0xFFD32F2F),
        topLeft = Offset(enemy.x, screenY),
        size = Size(ENEMY_SIZE, ENEMY_SIZE)
    )
    
    // Draw border
    drawRect(
        color = Color(0xFFB71C1C),
        topLeft = Offset(enemy.x, screenY),
        size = Size(ENEMY_SIZE, ENEMY_SIZE),
        style = Stroke(width = 3f)
    )
    
    // Draw enemy count
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    val textLayout = measurer.measure("${enemy.count}", textStyle)
    drawText(
        textLayout,
        topLeft = Offset(
            enemy.x + ENEMY_SIZE / 2 - textLayout.size.width / 2,
            screenY + ENEMY_SIZE / 2 - textLayout.size.height / 2
        )
    )
}

private fun DrawScope.drawBoss(boss: Boss, measurer: TextMeasurer) {
    val centerX = size.width / 2
    val centerY = size.height / 3
    
    // Draw rotating boss square
    rotate(boss.rotation, pivot = Offset(centerX, centerY)) {
        drawRect(
            color = Color(0xFFD32F2F),
            topLeft = Offset(centerX - BOSS_SIZE / 2, centerY - BOSS_SIZE / 2),
            size = Size(BOSS_SIZE, BOSS_SIZE)
        )
        
        // Draw border
        drawRect(
            color = Color(0xFFB71C1C),
            topLeft = Offset(centerX - BOSS_SIZE / 2, centerY - BOSS_SIZE / 2),
            size = Size(BOSS_SIZE, BOSS_SIZE),
            style = Stroke(width = 6f)
        )
    }
    
    // Draw HP text (not rotated)
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    )
    val hpText = "${boss.hp}"
    val textLayout = measurer.measure(hpText, textStyle)
    drawText(
        textLayout,
        topLeft = Offset(
            centerX - textLayout.size.width / 2,
            centerY - textLayout.size.height / 2
        )
    )
    
    // Draw HP bar above boss
    val barWidth = BOSS_SIZE
    val barHeight = 12f
    val barY = centerY - BOSS_SIZE / 2 - 30f
    
    // Background
    drawRect(
        color = Color(0x66000000),
        topLeft = Offset(centerX - barWidth / 2, barY),
        size = Size(barWidth, barHeight)
    )
    
    // HP fill
    val fill = (boss.hp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
    drawRect(
        color = Color(0xFFFF5252),
        topLeft = Offset(centerX - barWidth / 2, barY),
        size = Size(barWidth * fill, barHeight)
    )
}

private fun DrawScope.drawCrowd(state: GateWalkerGameState) {
    // Draw each crowd member
    state.crowdMembers.forEach { member ->
        val x = state.playerX + member.offsetX
        val y = state.playerY + member.offsetY
        
        // Draw character as circle
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = PLAYER_SIZE / 2,
            center = Offset(x, y)
        )
        
        // Draw outline
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = PLAYER_SIZE / 2,
            center = Offset(x, y),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawHud(state: GateWalkerGameState, measurer: TextMeasurer) {
    val style = TextStyle(Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

    if (!state.gameOver) {
        // Crowd count - large and prominent
        val countStyle = TextStyle(Color(0xFF4CAF50), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        val countText = "${state.crowdCount}"
        val countLayout = measurer.measure(countText, countStyle)
        drawText(countLayout, topLeft = Offset(
            size.width / 2 - countLayout.size.width / 2,
            20f
        ))
        
        // Level
        val levelText = "Level ${state.level}"
        val levelLayout = measurer.measure(levelText, style)
        drawText(levelLayout, topLeft = Offset(12f, 12f))
        
        // Score
        val scoreText = "Score: ${state.score}"
        val scoreLayout = measurer.measure(scoreText, style)
        drawText(scoreLayout, topLeft = Offset(size.width - scoreLayout.size.width - 12f, 12f))
        
        // Progress bar
        val progress = (state.distanceTraveled / state.levelLength).coerceIn(0f, 1f)
        val barWidth = size.width - 24f
        val barHeight = 8f
        val barY = 70f
        
        // Background
        drawRect(
            color = Color(0x44FFFFFF),
            topLeft = Offset(12f, barY),
            size = Size(barWidth, barHeight)
        )
        
        // Progress
        drawRect(
            color = Color(0xFF4CAF50),
            topLeft = Offset(12f, barY),
            size = Size(barWidth * progress, barHeight)
        )
    } else {
        // Game Over screen
        val titleStyle = TextStyle(Color(0xFFFF5252), 40.sp, FontWeight.Bold)
        val titleLayout = measurer.measure("GAME OVER", titleStyle)
        
        val labelStyle = TextStyle(Color(0xFFBBBBBB), 14.sp, FontWeight.Normal)
        val valueStyle = TextStyle(Color.White, 24.sp, FontWeight.Bold)
        val restartStyle = TextStyle(Color(0xFF4CAF50), 20.sp, FontWeight.Bold)
        
        // Stats
        val levelLabel = measurer.measure("Level Reached", labelStyle)
        val levelValue = measurer.measure("${state.level}", valueStyle)
        val scoreLabel = measurer.measure("Final Score", labelStyle)
        val scoreValue = measurer.measure("${state.score}", valueStyle)
        val crowdLabel = measurer.measure("Final Crowd", labelStyle)
        val crowdValue = measurer.measure("${state.crowdCount}", valueStyle)
        val restartText = measurer.measure("Tap to Restart", restartStyle)
        
        val centerY = size.height / 2f
        val centerX = size.width / 2f
        
        // Title
        drawText(titleLayout, topLeft = Offset(
            centerX - titleLayout.size.width / 2f,
            centerY - 220f
        ))
        
        // Level
        drawText(levelLabel, topLeft = Offset(centerX - levelLabel.size.width / 2f, centerY - 100f))
        drawText(levelValue, topLeft = Offset(centerX - levelValue.size.width / 2f, centerY - 65f))
        
        // Score
        drawText(scoreLabel, topLeft = Offset(centerX - scoreLabel.size.width / 2f, centerY - 5f))
        drawText(scoreValue, topLeft = Offset(centerX - scoreValue.size.width / 2f, centerY + 30f))
        
        // Crowd
        drawText(crowdLabel, topLeft = Offset(centerX - crowdLabel.size.width / 2f, centerY + 90f))
        drawText(crowdValue, topLeft = Offset(centerX - crowdValue.size.width / 2f, centerY + 125f))
        
        // Restart
        drawText(restartText, topLeft = Offset(centerX - restartText.size.width / 2f, centerY + 200f))
    }
}
