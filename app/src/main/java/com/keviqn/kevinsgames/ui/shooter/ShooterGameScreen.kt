package com.keviqn.kevinsgames.ui.shooter

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShooterGameScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val statsManager = remember { GameStatsManager(context) }
    val state = remember {
        ShooterGameState(
            onGameOver = { score, round ->
                statsManager.updateStats(gameId = 1, score = score, round = round)
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
                title = { Text("Space Shooter", fontWeight = FontWeight.Bold) },
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
                    containerColor = Color(0xFF1B5E20),
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

                // Background
                drawRect(color = Color(0xFF0A0A1A), size = size)

                if (!state.gameOver) {
                    // Enemies
                    state.enemies.forEach { drawEnemy(it) }

                    // Upgrades
                    state.upgrades.forEach { drawUpgrade(it) }

                    // Bullets
                    state.bullets.forEach {
                        drawRect(
                            Color(0xFFFFFF66),
                            topLeft = Offset(it.x, it.y),
                            size = Size(BULLET_W, BULLET_H)
                        )
                    }

                    // Player
                    val playerTop  = state.canvasH - PLAYER_HEIGHT - 16f
                    val playerLeft = state.playerX - PLAYER_WIDTH / 2f
                    drawRect(
                        Color(0xFF4CAF50),
                        topLeft = Offset(playerLeft, playerTop),
                        size = Size(PLAYER_WIDTH, PLAYER_HEIGHT)
                    )
                }

                // HUD & Game Over
                drawHud(state, textMeasurer)
            }
        }
    }
}

// ── Draw helpers ──────────────────────────────────────────────────────────────

private fun DrawScope.drawEnemy(enemy: Enemy) {
    // Different colors for different enemy types
    val color = when (enemy.type) {
        EnemyType.NORMAL -> Color(0xFFE53935)   // Red
        EnemyType.SPRINTER -> Color(0xFFFF6F00) // Orange
        EnemyType.TANK -> Color(0xFF6A1B9A)     // Purple
    }
    val hpBarColor = when (enemy.type) {
        EnemyType.NORMAL -> Color(0xFFFF8A80)   // Light red
        EnemyType.SPRINTER -> Color(0xFFFFAB40) // Light orange
        EnemyType.TANK -> Color(0xFFBA68C8)     // Light purple
    }
    
    drawRect(color, topLeft = Offset(enemy.x, enemy.y), size = Size(ENTITY_W, ENTITY_H))
    val barW = ENTITY_W - 8f
    drawRect(Color(0x66000000), topLeft = Offset(enemy.x + 4f, enemy.y + ENTITY_H - 10f), size = Size(barW, 6f))
    val fill = (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
    drawRect(hpBarColor, topLeft = Offset(enemy.x + 4f, enemy.y + ENTITY_H - 10f), size = Size(barW * fill, 6f))
}

private fun DrawScope.drawUpgrade(upgrade: Upgrade) {
    val color = when (upgrade.type) {
        UpgradeType.FIRE_RATE -> Color(0xFF1E88E5)
        UpgradeType.DAMAGE    -> Color(0xFFAB47BC)
    }
    drawRect(color, topLeft = Offset(upgrade.x, upgrade.y), size = Size(ENTITY_W, ENTITY_H))
    drawRect(Color(0x33FFFFFF), topLeft = Offset(upgrade.x + 6f, upgrade.y + 6f), size = Size(ENTITY_W - 12f, ENTITY_H - 12f))
}

private fun DrawScope.drawHud(state: ShooterGameState, measurer: TextMeasurer) {
    val style = TextStyle(Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    if (!state.gameOver) {
        // Round
        val roundLayout = measurer.measure("Round ${state.round}", style)
        drawText(roundLayout, topLeft = Offset(12f, 12f))

        // Stats box
        val statsTitle = "Stats"
        val dmgText = "DMG ×${state.bulletDamage}"
        val speedText = "Speed ×${"%.1f".format(BASE_FIRE_INTERVAL / state.fireInterval)}"
        
        val titleLayout = measurer.measure(statsTitle, style)
        val dmgLayout = measurer.measure(dmgText, style)
        val speedLayout = measurer.measure(speedText, style)
        
        val boxPadding = 8f
        val lineSpacing = 6f
        val boxWidth = maxOf(titleLayout.size.width, dmgLayout.size.width, speedLayout.size.width) + boxPadding * 2
        val boxHeight = titleLayout.size.height + dmgLayout.size.height + speedLayout.size.height + lineSpacing * 2 + boxPadding * 2
        val boxLeft = size.width - boxWidth - 12f
        val boxTop = 12f
        
        // Draw box border
        drawRect(
            color = Color.White,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        
        // Draw stats text
        drawText(titleLayout, topLeft = Offset(boxLeft + boxPadding, boxTop + boxPadding))
        drawText(dmgLayout, topLeft = Offset(boxLeft + boxPadding, boxTop + boxPadding + titleLayout.size.height + lineSpacing))
        drawText(speedLayout, topLeft = Offset(boxLeft + boxPadding, boxTop + boxPadding + titleLayout.size.height + dmgLayout.size.height + lineSpacing * 2))

        // Between-wave countdown
        if (state.waveStarting) {
            val msg = "Wave ${state.round + 1} in ${state.waveTimer.toInt() + 1}…"
            val layout = measurer.measure(msg, TextStyle(Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            drawText(layout, topLeft = Offset(
                (size.width  - layout.size.width)  / 2f,
                (size.height - layout.size.height) / 2f
            ))
        }
    } else {
        // Game Over screen with better layout
        val titleStyle = TextStyle(Color.Red, 40.sp, FontWeight.Bold)
        val titleLayout = measurer.measure("GAME OVER", titleStyle)
        
        val labelStyle = TextStyle(Color(0xFFBBBBBB), 14.sp, FontWeight.Normal)
        val valueStyle = TextStyle(Color.White, 24.sp, FontWeight.Bold)
        val restartStyle = TextStyle(Color(0xFF4CAF50), 20.sp, FontWeight.Bold)
        
        // Stats
        val roundLabel = measurer.measure("Round", labelStyle)
        val roundValue = measurer.measure("${state.round}", valueStyle)
        val enemiesLabel = measurer.measure("Enemies Destroyed", labelStyle)
        val enemiesValue = measurer.measure("${state.enemiesDestroyed}", valueStyle)
        val upgradesLabel = measurer.measure("Upgrades Collected", labelStyle)
        val upgradesValue = measurer.measure("${state.upgradesCollected}", valueStyle)
        val restartText = measurer.measure("Tap to Restart", restartStyle)
        
        val centerY = size.height / 2f
        val centerX = size.width / 2f
        
        // Title
        drawText(titleLayout, topLeft = Offset(
            centerX - titleLayout.size.width / 2f,
            centerY - 220f
        ))
        
        // Round
        drawText(roundLabel, topLeft = Offset(centerX - roundLabel.size.width / 2f, centerY - 100f))
        drawText(roundValue, topLeft = Offset(centerX - roundValue.size.width / 2f, centerY - 65f))
        
        // Enemies
        drawText(enemiesLabel, topLeft = Offset(centerX - enemiesLabel.size.width / 2f, centerY - 5f))
        drawText(enemiesValue, topLeft = Offset(centerX - enemiesValue.size.width / 2f, centerY + 30f))
        
        // Upgrades
        drawText(upgradesLabel, topLeft = Offset(centerX - upgradesLabel.size.width / 2f, centerY + 90f))
        drawText(upgradesValue, topLeft = Offset(centerX - upgradesValue.size.width / 2f, centerY + 125f))
        
        // Restart
        drawText(restartText, topLeft = Offset(centerX - restartText.size.width / 2f, centerY + 200f))
    }
}
