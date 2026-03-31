package com.keviqn.kevinsgames.ui.gatewalker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.random.Random

// ── Constants ────────────────────────────────────────────────────────────────

const val PLAYER_SIZE = 40f
const val GATE_HEIGHT = 160f
const val GATE_SPACING = 800f  // Distance between gate pairs (increased for less frequency)
const val ENEMY_SIZE = 60f
const val FORWARD_SPEED = 600f  // Base forward movement speed (50% faster than doubled: 400 * 1.5 = 600)
const val HORIZONTAL_SPEED = 400f  // Player left/right movement speed
const val CROWD_SPACING = 25f  // Space between crowd members
const val BOSS_SIZE = 200f  // Boss size
const val BOSS_FIGHT_DURATION = 10f  // Boss fight should always take 10 seconds

// ── Data classes ─────────────────────────────────────────────────────────────

enum class GateType {
    ADD,      // +N
    MULTIPLY, // ×N
    SUBTRACT, // -N
    DIVIDE    // ÷N
}

data class Gate(
    val id: Int,
    val x: Float,
    val y: Float,
    val type: GateType,
    val value: Int,
    val isLeft: Boolean  // true = left gate, false = right gate
)

data class Obstacle(
    val id: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val rotation: Float = 0f,
    val type: ObstacleType
)

enum class ObstacleType {
    SPINNING_BAR,
    WALL,
    MOVING_HAZARD
}

data class Enemy(
    val id: Int,
    val x: Float,
    val y: Float,
    val count: Int
)

data class CrowdMember(
    val id: Int,
    val offsetX: Float,
    val offsetY: Float
)

data class Boss(
    val hp: Int,
    val maxHp: Int,
    val rotation: Float = 0f
)

// ── Game state ───────────────────────────────────────────────────────────────

class GateWalkerGameState(
    private val onGameOver: ((score: Int, level: Int) -> Unit)? = null
) {

    // Canvas
    var canvasW by mutableStateOf(0f)
    var canvasH by mutableStateOf(0f)

    // Player position (center of crowd)
    var playerX by mutableStateOf(0f)
    var playerY by mutableStateOf(0f)

    // Crowd
    var crowdCount by mutableStateOf(1)
    var crowdMembers by mutableStateOf(listOf<CrowdMember>())

    // Game elements
    var gates by mutableStateOf(listOf<Gate>())
    var obstacles by mutableStateOf(listOf<Obstacle>())
    var enemies by mutableStateOf(listOf<Enemy>())

    // Level & progress
    var level by mutableStateOf(1)
    var distanceTraveled by mutableStateOf(0f)
    var levelLength by mutableStateOf(6000f)  // Distance to complete level (doubled from 3000f)

    // Boss fight
    var boss by mutableStateOf<Boss?>(null)
    var bossActive by mutableStateOf(false)
    var bossAttackTimer by mutableStateOf(0f)
    var bossAttackInterval by mutableStateOf(0f)  // Dynamically calculated per fight

    // Game state
    var gameOver by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var score by mutableStateOf(0)

    // ID counters
    private var nextGateId = 0
    private var nextObstacleId = 0
    private var nextEnemyId = 0
    private var nextCrowdId = 0

    // Camera offset (for scrolling effect)
    var cameraY by mutableStateOf(0f)

    // ── Initialise ────────────────────────────────────────────────────────────

    fun initCanvas(w: Float, h: Float) {
        if (canvasW != 0f) return
        canvasW = w
        canvasH = h
        playerX = w / 2f
        playerY = h * 0.75f  // Player near bottom
        updateCrowdFormation()
        generateLevel()
    }

    // ── Level generation ──────────────────────────────────────────────────────

    private fun generateLevel() {
        val newGates = mutableListOf<Gate>()
        val newObstacles = mutableListOf<Obstacle>()
        val newEnemies = mutableListOf<Enemy>()

        var currentY = 0f

        // Generate gates, obstacles, and enemies along the path
        while (currentY < levelLength) {
            // Generate gate pair
            if (Random.nextFloat() < 0.8f) {  // 80% chance of gates
                val gatePair = generateGatePair(currentY)
                newGates.addAll(gatePair)
            }

            // Generate obstacle
            if (Random.nextFloat() < 0.3f) {  // 30% chance of obstacle
                newObstacles.add(generateObstacle(currentY + 150f))
            }

            // Generate enemy
            if (Random.nextFloat() < 0.25f && level > 1) {  // 25% chance, not in level 1
                newEnemies.add(generateEnemy(currentY + 200f))
            }

            currentY += GATE_SPACING
        }

        // Add final multiplier zone (represented as special gates)
        val finalY = levelLength + 200f
        newGates.add(Gate(
            id = nextGateId++,
            x = canvasW * 0.25f,
            y = finalY,
            type = GateType.MULTIPLY,
            value = 2,
            isLeft = true
        ))
        newGates.add(Gate(
            id = nextGateId++,
            x = canvasW * 0.75f,
            y = finalY,
            type = GateType.MULTIPLY,
            value = 3,
            isLeft = false
        ))

        gates = newGates
        obstacles = newObstacles
        enemies = newEnemies
    }

    private fun generateGatePair(y: Float): List<Gate> {
        // Gates take up 50% of screen width each
        val leftX = canvasW * 0.25f  // Center of left gate
        val rightX = canvasW * 0.75f  // Center of right gate

        // Generate complementary gates
        val types = listOf(GateType.ADD, GateType.MULTIPLY, GateType.SUBTRACT, GateType.DIVIDE)
        val leftType = types.random()
        val rightType = types.random()

        val leftValue = when (leftType) {
            GateType.ADD -> Random.nextInt(5, 15 + level * 2)
            GateType.MULTIPLY -> Random.nextInt(2, 4)
            GateType.SUBTRACT -> Random.nextInt(3, 10)
            GateType.DIVIDE -> Random.nextInt(2, 4)
        }

        val rightValue = when (rightType) {
            GateType.ADD -> Random.nextInt(5, 15 + level * 2)
            GateType.MULTIPLY -> Random.nextInt(2, 4)
            GateType.SUBTRACT -> Random.nextInt(3, 10)
            GateType.DIVIDE -> Random.nextInt(2, 4)
        }

        return listOf(
            Gate(nextGateId++, leftX, y, leftType, leftValue, true),
            Gate(nextGateId++, rightX, y, rightType, rightValue, false)
        )
    }

    private fun generateObstacle(y: Float): Obstacle {
        val type = ObstacleType.entries.random()
        val obstacleSize = canvasW * 0.25f  // 25% of screen width
        val x = Random.nextFloat() * (canvasW - obstacleSize)
        
        return Obstacle(
            id = nextObstacleId++,
            x = x,
            y = y,
            size = obstacleSize,
            rotation = Random.nextFloat() * 360f,
            type = type
        )
    }

    private fun generateEnemy(y: Float): Enemy {
        val x = Random.nextFloat() * (canvasW - ENEMY_SIZE)
        val count = Random.nextInt(crowdCount / 2, crowdCount + level * 3)
        
        return Enemy(
            id = nextEnemyId++,
            x = x,
            y = y,
            count = count
        )
    }

    // ── Crowd formation ───────────────────────────────────────────────────────

    private fun updateCrowdFormation() {
        val members = mutableListOf<CrowdMember>()
        
        // Arrange crowd in a grid formation
        val cols = when {
            crowdCount <= 3 -> crowdCount
            crowdCount <= 8 -> 3
            crowdCount <= 15 -> 4
            else -> 5
        }
        
        var id = 0
        var remaining = crowdCount
        var row = 0
        
        while (remaining > 0) {
            val inThisRow = minOf(cols, remaining)
            val rowWidth = (inThisRow - 1) * CROWD_SPACING
            val startX = -rowWidth / 2f
            
            for (col in 0 until inThisRow) {
                members.add(CrowdMember(
                    id = nextCrowdId++,
                    offsetX = startX + col * CROWD_SPACING,
                    offsetY = -row * CROWD_SPACING
                ))
                remaining--
            }
            row++
        }
        
        crowdMembers = members
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    fun tick(dt: Float) {
        if (canvasW == 0f || gameOver || isPaused) return

        // Boss fight logic
        if (bossActive && boss != null) {
            // Rotate boss
            boss = boss!!.copy(rotation = (boss!!.rotation + 90f * dt) % 360f)
            
            // Attack boss with crowd members
            bossAttackTimer += dt
            if (bossAttackTimer >= bossAttackInterval && crowdCount > 0) {
                bossAttackTimer = 0f
                crowdCount--
                boss = boss!!.copy(hp = boss!!.hp - 1)
                updateCrowdFormation()
                
                // Check if boss defeated
                if (boss!!.hp <= 0) {
                    score += level * 100  // Bonus for beating boss
                    advanceToNextLevel()
                }
                
                // Check if we ran out of crowd
                if (crowdCount <= 0 && boss!!.hp > 0) {
                    endGame()
                }
            }
            return
        }

        // Normal gameplay
        // Move forward
        val moveDistance = FORWARD_SPEED * dt
        distanceTraveled += moveDistance

        // Update obstacle rotations
        obstacles = obstacles.map { obstacle ->
            if (obstacle.type == ObstacleType.SPINNING_BAR) {
                obstacle.copy(rotation = (obstacle.rotation + 180f * dt) % 360f)
            } else {
                obstacle
            }
        }

        // Check if level complete - start boss fight
        if (distanceTraveled >= levelLength) {
            startBossFight()
            return
        }

        // Check collisions
        checkGateCollisions()
        checkObstacleCollisions()
        checkEnemyCollisions()

        // Remove passed elements (below screen)
        gates = gates.filter { distanceTraveled - it.y < canvasH + 200f }
        obstacles = obstacles.filter { distanceTraveled - it.y < canvasH + 200f }
        enemies = enemies.filter { distanceTraveled - it.y < canvasH + 200f }
    }

    // ── Collision detection ───────────────────────────────────────────────────

    private fun checkGateCollisions() {
        gates.filter { gate ->
            val gateScreenY = distanceTraveled - gate.y
            val gateWidth = canvasW * 0.5f  // 50% of screen width
            // Check if player is passing through gate (gate is at player's Y position)
            gateScreenY >= playerY - GATE_HEIGHT &&
            gateScreenY <= playerY + PLAYER_SIZE &&
            overlaps(playerX - PLAYER_SIZE/2, playerY - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE,
                     gate.x - gateWidth/2, gateScreenY, gateWidth, GATE_HEIGHT)
        }.forEach { gate ->
            applyGateEffect(gate)
            gates = gates.filter { it.id != gate.id }
        }
    }

    private fun applyGateEffect(gate: Gate) {
        val oldCount = crowdCount
        crowdCount = when (gate.type) {
            GateType.ADD -> crowdCount + gate.value
            GateType.MULTIPLY -> crowdCount * gate.value
            GateType.SUBTRACT -> max(0, crowdCount - gate.value)
            GateType.DIVIDE -> max(1, crowdCount / gate.value)
        }
        
        score += (crowdCount - oldCount) * 10
        
        if (crowdCount <= 0) {
            endGame()
        } else {
            updateCrowdFormation()
        }
    }

    private fun checkObstacleCollisions() {
        obstacles.filter { obstacle ->
            val obstacleScreenY = distanceTraveled - obstacle.y
            obstacleScreenY >= playerY - obstacle.size &&
            obstacleScreenY <= playerY + PLAYER_SIZE &&
            overlaps(playerX - PLAYER_SIZE/2, playerY - PLAYER_SIZE/2,
                    PLAYER_SIZE, PLAYER_SIZE,
                    obstacle.x, obstacleScreenY, obstacle.size, obstacle.size)
        }.forEach { obstacle ->
            // Lose some crowd members
            val loss = Random.nextInt(1, max(2, crowdCount / 3))
            crowdCount = max(0, crowdCount - loss)
            score = max(0, score - loss * 5)
            
            if (crowdCount <= 0) {
                endGame()
            } else {
                updateCrowdFormation()
            }
            
            obstacles = obstacles.filter { it.id != obstacle.id }
        }
    }

    private fun checkEnemyCollisions() {
        enemies.filter { enemy ->
            val enemyScreenY = distanceTraveled - enemy.y
            enemyScreenY >= playerY - ENEMY_SIZE &&
            enemyScreenY <= playerY + PLAYER_SIZE &&
            overlaps(playerX - PLAYER_SIZE/2, playerY - PLAYER_SIZE/2,
                    PLAYER_SIZE, PLAYER_SIZE,
                    enemy.x, enemyScreenY, ENEMY_SIZE, ENEMY_SIZE)
        }.forEach { enemy ->
            if (crowdCount > enemy.count) {
                // Win but lose units
                crowdCount -= enemy.count
                score += enemy.count * 20
                updateCrowdFormation()
            } else {
                // Game over
                endGame()
            }
            
            enemies = enemies.filter { it.id != enemy.id }
        }
    }

    private fun overlaps(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ) = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    // ── Player movement ───────────────────────────────────────────────────────

    fun movePlayer(deltaX: Float) {
        playerX = (playerX + deltaX).coerceIn(
            PLAYER_SIZE,
            canvasW - PLAYER_SIZE
        )
    }

    // ── Level management ──────────────────────────────────────────────────────

    private fun startBossFight() {
        bossActive = true
        bossAttackTimer = 0f
        val bossHp = level * 2
        boss = Boss(hp = bossHp, maxHp = bossHp, rotation = 0f)
        
        // Calculate attack interval so fight takes exactly 10 seconds
        // Total attacks needed = crowdCount (to defeat boss)
        // Time per attack = 10 seconds / crowdCount
        bossAttackInterval = if (crowdCount > 0) {
            BOSS_FIGHT_DURATION / crowdCount.toFloat()
        } else {
            0.1f  // Fallback if no crowd (will lose immediately)
        }
        
        // Clear all game elements for boss fight
        gates = listOf()
        obstacles = listOf()
        enemies = listOf()
    }

    private fun advanceToNextLevel() {
        level++
        distanceTraveled = 0f
        levelLength += 500f  // Increase level length
        bossActive = false
        boss = null
        bossAttackTimer = 0f
        generateLevel()
    }

    private fun endGame() {
        gameOver = true
        onGameOver?.invoke(score, level)
    }

    // ── Game controls ─────────────────────────────────────────────────────────

    fun togglePause() {
        isPaused = !isPaused
    }

    fun reset() {
        crowdCount = 1
        crowdMembers = listOf()
        gates = listOf()
        obstacles = listOf()
        enemies = listOf()
        boss = null
        bossActive = false
        bossAttackTimer = 0f
        bossAttackInterval = 0f
        level = 1
        distanceTraveled = 0f
        levelLength = 6000f
        gameOver = false
        isPaused = false
        score = 0
        nextGateId = 0
        nextObstacleId = 0
        nextEnemyId = 0
        nextCrowdId = 0
        playerX = canvasW / 2f
        playerY = canvasH * 0.75f
        updateCrowdFormation()
        generateLevel()
    }
}
