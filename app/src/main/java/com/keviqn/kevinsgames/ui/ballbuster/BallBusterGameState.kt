package com.keviqn.kevinsgames.ui.ballbuster

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// ── Constants ────────────────────────────────────────────────────────────────

const val BALL_RADIUS = 12f
const val BALL_SPEED = 1200f  // px/s (doubled from 600f)
const val BOX_SIZE = 80f
const val BOX_SPACING = 4f

// ── Data classes ─────────────────────────────────────────────────────────────

data class Ball(
    val x: Float,
    val y: Float,
    val vx: Float,  // velocity x
    val vy: Float   // velocity y
)

enum class BoxType {
    NORMAL,
    UPGRADE_EXTRA_BALL,
    UPGRADE_DAMAGE
}

data class Box(
    val id: Int,
    val gridX: Int,
    val gridY: Int,
    val hp: Int,
    val maxHp: Int,
    val type: BoxType = BoxType.NORMAL
)

// ── Game state ───────────────────────────────────────────────────────────────

class BallBusterGameState(
    private val onGameOver: ((score: Int, round: Int) -> Unit)? = null
) {

    // Canvas
    var canvasW by mutableStateOf(0f)
    var canvasH by mutableStateOf(0f)

    // Balls (support multiple)
    var balls by mutableStateOf(listOf<Ball>())
    var ballLaunched by mutableStateOf(false)
    var ballDamage by mutableStateOf(1)
    var numBalls by mutableStateOf(1)  // How many balls to launch
    var ballStartX by mutableStateOf(0f)  // Where balls shoot from
    
    // Ball launching state
    private var ballsToLaunch = 0
    private var launchTimer = 0f
    private val launchDelay = 0.1f  // Delay between each ball launch
    private var launchVelocity: Pair<Float, Float>? = null
    
    // Fast forward state
    var roundTimer by mutableStateOf(0f)
    var showFastForward by mutableStateOf(false)
    var fastForwardActive by mutableStateOf(false)
    private val fastForwardThreshold = 5f

    // Boxes
    var boxes by mutableStateOf(listOf<Box>())
    private var nextBoxId = 0

    // Round
    var round by mutableStateOf(1)
    var gameOver by mutableStateOf(false)
    var isPaused by mutableStateOf(false)

    // Drag state for aiming
    var aimStart by mutableStateOf<Offset?>(null)
    var aimCurrent by mutableStateOf<Offset?>(null)

    // Grid dimensions
    private var gridCols = 0
    private var gridRows = 0
    private var gridOffsetX = 0f
    private var gridOffsetY = 0f

    // ── Initialise ────────────────────────────────────────────────────────────

    fun initCanvas(w: Float, h: Float) {
        if (canvasW != 0f) return
        canvasW = w
        canvasH = h

        // Calculate grid dimensions
        gridCols = ((w - BOX_SPACING) / (BOX_SIZE + BOX_SPACING)).toInt()
        gridRows = ((h * 0.7f - BOX_SPACING) / (BOX_SIZE + BOX_SPACING)).toInt()
        
        // Center the grid horizontally, start one row lower
        val totalGridWidth = gridCols * (BOX_SIZE + BOX_SPACING) - BOX_SPACING
        gridOffsetX = (w - totalGridWidth) / 2f
        gridOffsetY = BOX_SPACING + BOX_SIZE + BOX_SPACING  // Start one row lower

        // Initialize ball at bottom center
        ballStartX = w / 2f
        resetBalls()
        
        // Start first round
        spawnNewRow()
    }

    private fun resetBalls() {
        balls = listOf(Ball(
            x = ballStartX,
            y = canvasH - 100f,
            vx = 0f,
            vy = 0f
        ))
        ballLaunched = false
        aimStart = null
        aimCurrent = null
        roundTimer = 0f
        showFastForward = false
        fastForwardActive = false
    }

    // ── Grid helpers ──────────────────────────────────────────────────────────

    fun gridToScreen(gridX: Int, gridY: Int): Offset {
        val x = gridOffsetX + gridX * (BOX_SIZE + BOX_SPACING)
        val y = gridOffsetY + gridY * (BOX_SIZE + BOX_SPACING)
        return Offset(x, y)
    }

    // ── Wave management ───────────────────────────────────────────────────────

    private fun spawnNewRow() {
        // Randomly spawn boxes in the top row
        val newBoxes = mutableListOf<Box>()
        
        // Determine upgrade boxes: always 1, 10% chance for 2
        val upgradePositions = mutableSetOf<Int>()
        val numUpgrades = if (Random.nextFloat() < 0.1f) 2 else 1
        
        repeat(numUpgrades) {
            var pos: Int
            do {
                pos = Random.nextInt(gridCols)
            } while (pos in upgradePositions)
            upgradePositions.add(pos)
        }
        
        for (x in 0 until gridCols) {
            // 60% chance to spawn a box
            if (Random.nextFloat() < 0.6f) {
                val boxType = when {
                    x in upgradePositions -> {
                        // 5% chance for damage upgrade, 95% for extra ball
                        if (Random.nextFloat() < 0.05f) BoxType.UPGRADE_DAMAGE
                        else BoxType.UPGRADE_EXTRA_BALL
                    }
                    else -> BoxType.NORMAL
                }
                
                newBoxes.add(Box(
                    id = nextBoxId++,
                    gridX = x,
                    gridY = 1,
                    hp = round,
                    maxHp = round,
                    type = boxType
                ))
            }
        }
        
        boxes = boxes + newBoxes
    }

    private fun moveBoxesDown() {
        boxes = boxes.map { box ->
            box.copy(gridY = box.gridY + 1)
        }
        
        // Check if any box reached the bottom
        if (boxes.any { it.gridY >= gridRows }) {
            gameOver = true
            onGameOver?.invoke(round * 100, round)  // Score = round * 100
        }
    }

    // ── Aiming ────────────────────────────────────────────────────────────────

    fun startAim(position: Offset) {
        if (!ballLaunched && balls.isNotEmpty()) {
            aimStart = position
            aimCurrent = position
        }
    }

    fun updateAim(position: Offset) {
        if (!ballLaunched && aimStart != null) {
            aimCurrent = position
        }
    }

    fun launchBall() {
        if (!ballLaunched && balls.isNotEmpty() && aimStart != null && aimCurrent != null) {
            val start = aimStart!!
            val current = aimCurrent!!
            
            // Calculate direction (opposite of drag)
            val dx = start.x - current.x
            val dy = start.y - current.y
            val distance = sqrt(dx * dx + dy * dy)
            
            if (distance > 10f) {
                // Check if ball would go upward (negative dy means upward)
                if (dy >= 0) {
                    // Ball would go down or horizontal, cancel the throw
                    aimStart = null
                    aimCurrent = null
                    return
                }
                
                // Normalize and apply speed
                val vx = (dx / distance) * BALL_SPEED
                val vy = (dy / distance) * BALL_SPEED
                
                // Set up sequential ball launching
                launchVelocity = Pair(vx, vy)
                ballsToLaunch = numBalls
                launchTimer = 0f
                ballLaunched = true
                balls = listOf()  // Clear balls, will add them one by one
                aimStart = null
                aimCurrent = null
            }
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    fun tick(dt: Float) {
        if (canvasW == 0f || gameOver || isPaused) return

        // Handle sequential ball launching
        if (ballLaunched && ballsToLaunch > 0 && launchVelocity != null) {
            launchTimer += dt
            if (launchTimer >= launchDelay) {
                launchTimer = 0f
                val (vx, vy) = launchVelocity!!
                val startBall = Ball(
                    x = ballStartX,
                    y = canvasH - 100f,
                    vx = vx,
                    vy = vy
                )
                balls = balls + startBall
                ballsToLaunch--
                
                if (ballsToLaunch == 0) {
                    launchVelocity = null
                }
            }
        }

        if (balls.isEmpty()) return

        // Update round timer and fast forward button
        if (ballLaunched) {
            roundTimer += dt
            if (roundTimer >= fastForwardThreshold && !showFastForward) {
                showFastForward = true
            }
        }

        // Apply fast forward speed multiplier
        val speedMultiplier = if (fastForwardActive) 10f else 1f
        val effectiveDt = dt * speedMultiplier

        val updatedBalls = mutableListOf<Ball>()
        val boxesToRemove = mutableSetOf<Int>()
        val boxDamageMap = mutableMapOf<Int, Int>()
        var activeBoxes = boxes.toMutableList()
        var firstBallReturned = false

        // Process each ball
        for (currentBall in balls) {
            // Move ball
            var newX = currentBall.x + currentBall.vx * effectiveDt
            var newY = currentBall.y + currentBall.vy * effectiveDt
            var newVx = currentBall.vx
            var newVy = currentBall.vy

            // Wall collisions
            if (newX - BALL_RADIUS < 0f) {
                newX = BALL_RADIUS
                newVx = abs(newVx)
            } else if (newX + BALL_RADIUS > canvasW) {
                newX = canvasW - BALL_RADIUS
                newVx = -abs(newVx)
            }

            if (newY - BALL_RADIUS < 0f) {
                newY = BALL_RADIUS
                newVy = abs(newVy)
            }

            // Check if ball reached bottom
            if (newY + BALL_RADIUS > canvasH) {
                // Track first ball's X position for next round
                if (!firstBallReturned) {
                    ballStartX = newX
                    firstBallReturned = true
                }
                // Ball is done, don't add to updated list
                continue
            }

            // Box collisions
            var ballBounced = false
            for (box in activeBoxes) {
                if (ballBounced) break
                
                val screenPos = gridToScreen(box.gridX, box.gridY)
                val boxLeft = screenPos.x
                val boxTop = screenPos.y
                val boxRight = boxLeft + BOX_SIZE
                val boxBottom = boxTop + BOX_SIZE

                // Simple circle-rectangle collision
                val closestX = newX.coerceIn(boxLeft, boxRight)
                val closestY = newY.coerceIn(boxTop, boxBottom)
                val distX = newX - closestX
                val distY = newY - closestY
                val distSquared = distX * distX + distY * distY

                if (distSquared < BALL_RADIUS * BALL_RADIUS) {
                    // Collision detected - apply damage
                    val currentDamage = boxDamageMap.getOrDefault(box.id, 0)
                    boxDamageMap[box.id] = currentDamage + ballDamage

                    // Bounce ball
                    if (abs(distX) > abs(distY)) {
                        newVx = -newVx
                    } else {
                        newVy = -newVy
                    }
                    
                    ballBounced = true
                }
            }

            updatedBalls.add(Ball(newX, newY, newVx, newVy))
        }

        // Apply damage to boxes and check for destruction
        activeBoxes = activeBoxes.map { box ->
            val damage = boxDamageMap.getOrDefault(box.id, 0)
            val newHp = box.hp - damage
            if (newHp <= 0) {
                // Apply upgrade if it's an upgrade box
                when (box.type) {
                    BoxType.UPGRADE_EXTRA_BALL -> numBalls++
                    BoxType.UPGRADE_DAMAGE -> ballDamage++
                    BoxType.NORMAL -> {}
                }
                boxesToRemove.add(box.id)
            }
            box.copy(hp = newHp)
        }.filter { it.id !in boxesToRemove }.toMutableList()

        boxes = activeBoxes
        balls = updatedBalls

        // If all balls are gone, end round
        if (balls.isEmpty()) {
            moveBoxesDown()
            round++
            spawnNewRow()
            resetBalls()
        }
    }

    // ── Fast Forward ──────────────────────────────────────────────────────────

    fun toggleFastForward() {
        fastForwardActive = !fastForwardActive
    }

    // ── Pause ─────────────────────────────────────────────────────────────────

    fun togglePause() {
        isPaused = !isPaused
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    fun reset() {
        boxes = listOf()
        nextBoxId = 0
        round = 1
        gameOver = false
        ballDamage = 1
        numBalls = 1
        ballStartX = canvasW / 2f
        resetBalls()
        spawnNewRow()
    }
}
