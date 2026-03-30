package com.keviqn.kevinsgames.ui.shooter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShooterGameScreen(onBack: () -> Unit) {

    val state = remember { ShooterGameState() }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
                    // Walls
                    state.walls.forEach { drawWall(it) }

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

private fun DrawScope.drawWall(wall: Wall) {
    // Solid wall body – steel grey
    drawRect(
        color   = Color(0xFF78909C),
        topLeft = Offset(wall.x, wall.y),
        size    = Size(wall.w, WALL_H)
    )
    // Bright edge highlight
    drawRect(
        color   = Color(0xFFB0BEC5),
        topLeft = Offset(wall.x, wall.y),
        size    = Size(wall.w, 3f)
    )
    // HP bar (runs along the bottom of the wall)
    val fill = (wall.hp.toFloat() / wall.maxHp.toFloat()).coerceIn(0f, 1f)
    drawRect(
        color   = Color(0x88000000),
        topLeft = Offset(wall.x, wall.y + WALL_H - 5f),
        size    = Size(wall.w, 5f)
    )
    drawRect(
        color   = Color(0xFFFFCA28),
        topLeft = Offset(wall.x, wall.y + WALL_H - 5f),
        size    = Size(wall.w * fill, 5f)
    )
}

private fun DrawScope.drawEnemy(enemy: Enemy) {
    drawRect(Color(0xFFE53935), topLeft = Offset(enemy.x, enemy.y), size = Size(ENTITY_W, ENTITY_H))
    val barW = ENTITY_W - 8f
    drawRect(Color(0x66000000), topLeft = Offset(enemy.x + 4f, enemy.y + ENTITY_H - 10f), size = Size(barW, 6f))
    val fill = (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
    drawRect(Color(0xFFFF8A80), topLeft = Offset(enemy.x + 4f, enemy.y + ENTITY_H - 10f), size = Size(barW * fill, 6f))
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

        // Gun stats
        val dmgLayout = measurer.measure("DMG ×${state.bulletDamage}", style)
        drawText(dmgLayout, topLeft = Offset(size.width - dmgLayout.size.width - 12f, 12f))

        val rateLayout = measurer.measure("Rate ×${"%.1f".format(BASE_FIRE_INTERVAL / state.fireInterval)}", style)
        drawText(rateLayout, topLeft = Offset(size.width - rateLayout.size.width - 12f, 32f))

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
        // Game Over screen
        val titleLayout = measurer.measure("GAME OVER", TextStyle(Color.Red, 32.sp, FontWeight.Bold))
        drawText(titleLayout, topLeft = Offset(
            (size.width - titleLayout.size.width) / 2f,
            size.height / 2f - 60f
        ))

        val stats = """
            Rounds: ${state.round}
            Enemies Destroyed: ${state.enemiesDestroyed}
            Upgrades Collected: ${state.upgradesCollected}
            
            Tap to Restart
        """.trimIndent()
        val statsLayout = measurer.measure(stats, TextStyle(Color.White, 18.sp, FontWeight.Bold))
        drawText(statsLayout, topLeft = Offset(
            (size.width  - statsLayout.size.width)  / 2f,
            size.height / 2f
        ))
    }
}
