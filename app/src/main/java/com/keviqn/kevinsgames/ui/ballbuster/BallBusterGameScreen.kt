package com.keviqn.kevinsgames.ui.ballbuster

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.keviqn.kevinsgames.data.GameStatsManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallBusterGameScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val statsManager = remember { GameStatsManager(context) }
    val state = remember {
        BallBusterGameState(
            onGameOver = { score, round ->
                statsManager.updateStats(gameId = 2, score = score, round = round)
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
                title = { Text("Ball Buster", fontWeight = FontWeight.Bold) },
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
                    containerColor = Color(0xFF2E7D32),
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
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (state.gameOver) {
                                    state.reset()
                                } else {
                                    state.startAim(offset)
                                }
                            },
                            onDrag = { change, _ ->
                                state.updateAim(change.position)
                            },
                            onDragEnd = {
                                state.launchBall()
                            }
                        )
                    }
            ) {
                state.initCanvas(size.width, size.height)

                // Background
                drawRect(color = Color(0xFF1A1A2E), size = size)

                if (!state.gameOver) {
                    // Boxes
                    state.boxes.forEach { box ->
                        drawBox(state, box)
                    }

                    // Balls (render all of them)
                    state.balls.forEach { ball ->
                        drawCircle(
                            color = Color(0xFFFFEB3B),
                            radius = BALL_RADIUS,
                            center = Offset(ball.x, ball.y)
                        )
                    }

                    // Aim line (dotted trajectory)
                    if (state.aimStart != null && state.aimCurrent != null && !state.ballLaunched) {
                        drawAimLine(state)
                    }
                }

                // HUD & Game Over
                drawHud(state, textMeasurer)
            }
            
            // Fast Forward Button
            if (state.showFastForward && !state.gameOver) {
                Button(
                    onClick = { state.toggleFastForward() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.fastForwardActive) Color(0xFFFF5722) else Color(0xFF4CAF50)
                    )
                ) {
                    Text(if (state.fastForwardActive) "⏸" else ">>", fontSize = 20.sp)
                }
            }
        }
    }
}

// ── Draw helpers ──────────────────────────────────────────────────────────────

private fun DrawScope.drawBox(state: BallBusterGameState, box: Box) {
    val screenPos = state.gridToScreen(box.gridX, box.gridY)
    
    // Box body - special colors for upgrades
    val boxColor = when (box.type) {
        BoxType.UPGRADE_EXTRA_BALL -> Color(0xFF2196F3)  // Blue for extra ball
        BoxType.UPGRADE_DAMAGE -> Color(0xFF9C27B0)      // Purple for damage
        BoxType.NORMAL -> {
            // Normal boxes change color based on HP
            val hpRatio = box.hp.toFloat() / box.maxHp.toFloat()
            when {
                hpRatio > 0.66f -> Color(0xFF4CAF50)
                hpRatio > 0.33f -> Color(0xFFFFA726)
                else -> Color(0xFFEF5350)
            }
        }
    }
    
    drawRect(
        color = boxColor,
        topLeft = screenPos,
        size = Size(BOX_SIZE, BOX_SIZE)
    )
    
    // Border
    drawRect(
        color = Color(0xFF263238),
        topLeft = screenPos,
        size = Size(BOX_SIZE, BOX_SIZE),
        style = Stroke(width = 2f)
    )
    
    // HP text
    val hpText = "${box.hp}"
    val textPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 24f
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    
    drawContext.canvas.nativeCanvas.drawText(
        hpText,
        screenPos.x + BOX_SIZE / 2f,
        screenPos.y + BOX_SIZE / 2f + 8f,
        textPaint
    )
}

private fun DrawScope.drawAimLine(state: BallBusterGameState) {
    if (state.balls.isEmpty()) return
    
    val ballPos = Offset(state.balls[0].x, state.balls[0].y)
    val start = state.aimStart!!
    val current = state.aimCurrent!!
    
    // Calculate direction (opposite of drag)
    val dx = start.x - current.x
    val dy = start.y - current.y
    val distance = sqrt(dx * dx + dy * dy)
    
    if (distance > 10f) {
        // Normalize
        val ndx = dx / distance
        val ndy = dy / distance
        
        // Draw dotted line showing trajectory starting from ball position
        val lineLength = 900f  // 3x longer (was 300f)
        val dotSpacing = 15f
        var traveled = 0f
        
        while (traveled < lineLength) {
            val x = ballPos.x + ndx * traveled
            val y = ballPos.y + ndy * traveled
            
            drawCircle(
                color = Color(0x88FFFFFF),
                radius = 3f,
                center = Offset(x, y)
            )
            
            traveled += dotSpacing
        }
    }
}

private fun DrawScope.drawHud(state: BallBusterGameState, measurer: TextMeasurer) {
    val style = TextStyle(Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

    if (!state.gameOver) {
        // Round
        val roundLayout = measurer.measure("Round ${state.round}", style)
        drawText(roundLayout, topLeft = Offset(16f, 16f))
    } else {
        // Game Over screen with better layout
        val titleStyle = TextStyle(Color.Red, 40.sp, FontWeight.Bold)
        val titleLayout = measurer.measure("GAME OVER", titleStyle)
        
        val roundsText = "Rounds Survived"
        val roundsValueText = "${state.round - 1}"
        val restartText = "Tap to Restart"
        
        val labelStyle = TextStyle(Color(0xFFBBBBBB), 16.sp, FontWeight.Normal)
        val valueStyle = TextStyle(Color.White, 32.sp, FontWeight.Bold)
        val restartStyle = TextStyle(Color(0xFF4CAF50), 20.sp, FontWeight.Bold)
        
        val roundsLabelLayout = measurer.measure(roundsText, labelStyle)
        val roundsValueLayout = measurer.measure(roundsValueText, valueStyle)
        val restartLayout = measurer.measure(restartText, restartStyle)
        
        // Center everything vertically
        val centerY = size.height / 2f
        
        // Title at top
        drawText(titleLayout, topLeft = Offset(
            (size.width - titleLayout.size.width) / 2f,
            centerY - 180f
        ))
        
        // Rounds label
        drawText(roundsLabelLayout, topLeft = Offset(
            (size.width - roundsLabelLayout.size.width) / 2f,
            centerY - 50f
        ))
        
        // Rounds value
        drawText(roundsValueLayout, topLeft = Offset(
            (size.width - roundsValueLayout.size.width) / 2f,
            centerY + 10f
        ))
        
        // Restart text
        drawText(restartLayout, topLeft = Offset(
            (size.width - restartLayout.size.width) / 2f,
            centerY + 140f
        ))
    }
}
