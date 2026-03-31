package com.keviqn.kevinsgames.ui.shooter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

// ── Constants ────────────────────────────────────────────────────────────────

const val PLAYER_WIDTH       = 160f
const val PLAYER_HEIGHT      = 80f
const val ENTITY_W           = 160f
const val ENTITY_H           = 80f
const val BULLET_W           = 8f
const val BULLET_H           = 20f
const val BULLET_SPEED       = 900f  // px/s

private fun entitySpeed(round: Int) = 120f + (round - 1) * 15f
private fun enemyHorizontalSpeed(round: Int) = 30f + (round - 1) * 5f  // Slow horizontal movement
const val BASE_FIRE_INTERVAL = 0.4f  // Boosted from 0.4f for wave 20 feel
const val MIN_FIRE_INTERVAL  = 0.08f
const val BASE_DAMAGE        = 11     // Boosted from 1 for wave 20 feel
private fun enemyCountForRound(round: Int) = (2 + round) * 2  // 2x the enemies

// ── Data classes ─────────────────────────────────────────────────────────────

data class Bullet(val x: Float, val y: Float)

enum class EnemyType {
    NORMAL,   // 1x speed, 1x hp
    SPRINTER, // 2x speed, 0.5x hp
    TANK      // 0.5x speed, 2x hp
}

data class Enemy(
    val id: Int,
    val x: Float,
    val y: Float,
    val hp: Int,
    val maxHp: Int,
    val type: EnemyType,
    val horizontalDirection: Float  // -1 for left, 1 for right
)

data class Upgrade(
    val id: Int,
    val x: Float,
    val y: Float,
    val type: UpgradeType
)

enum class UpgradeType { FIRE_RATE, DAMAGE }

// ── Game state ───────────────────────────────────────────────────────────────

class ShooterGameState(
    private val onGameOver: ((score: Int, round: Int) -> Unit)? = null
) {

    // Canvas
    var canvasW by mutableStateOf(0f)
    var canvasH by mutableStateOf(0f)

    // Player
    var playerX by mutableStateOf(0f)

    // Projectiles
    var bullets  by mutableStateOf(listOf<Bullet>())

    // Enemies & upgrades
    var enemies  by mutableStateOf(listOf<Enemy>())
    var upgrades by mutableStateOf(listOf<Upgrade>())

    // Gun stats
    var fireInterval by mutableStateOf(BASE_FIRE_INTERVAL)
    var bulletDamage by mutableStateOf(BASE_DAMAGE)

    // Round / wave
    var round        by mutableStateOf(1)
    var waveActive   by mutableStateOf(false)
    var waveStarting by mutableStateOf(false)
    var waveTimer    by mutableStateOf(0f)

    // Unique ID counters
    private var nextEnemyId   = 0
    private var nextUpgradeId = 0

    // Internal fire timer
    private var fireTimer = 0f

    // Game over & stats
    var gameOver          by mutableStateOf(false)
    var enemiesDestroyed  by mutableStateOf(0)
    var upgradesCollected by mutableStateOf(0)
    var isPaused          by mutableStateOf(false)

    // ── Initialise ────────────────────────────────────────────────────────────

    fun initCanvas(w: Float, h: Float) {
        if (canvasW != 0f) return
        canvasW = w
        canvasH = h
        playerX = w / 2f
        startWave()
    }

    // ── Wave management ───────────────────────────────────────────────────────

    private fun startWave() {
        val count   = enemyCountForRound(round)
        val margin  = ENTITY_W / 2f
        val usableW = canvasW - ENTITY_W

        // Randomize enemy spawn positions with different types
        val newEnemies = List(count) { i ->
            val type = when (Random.nextInt(3)) {
                0 -> EnemyType.NORMAL
                1 -> EnemyType.SPRINTER
                else -> EnemyType.TANK
            }
            val baseHp = round
            val hp = when (type) {
                EnemyType.NORMAL -> baseHp
                EnemyType.SPRINTER -> (baseHp * 0.5f).toInt().coerceAtLeast(1)
                EnemyType.TANK -> baseHp * 2
            }
            Enemy(
                id    = nextEnemyId++,
                x     = margin + Random.nextFloat() * usableW,
                y     = -(ENTITY_H + i * (ENTITY_H + 24f)),
                hp    = hp,
                maxHp = hp,
                type  = type,
                horizontalDirection = if (Random.nextBoolean()) -1f else 1f
            )
        }

        // Guarantee at least one of each upgrade type per round
        val newUpgrades = mutableListOf<Upgrade>()
        
        // Fire rate upgrade
        newUpgrades.add(Upgrade(
            id   = nextUpgradeId++,
            x    = Random.nextFloat() * (canvasW - ENTITY_W),
            y    = -(ENTITY_H * 3),
            type = UpgradeType.FIRE_RATE
        ))
        
        // Damage upgrade
        newUpgrades.add(Upgrade(
            id   = nextUpgradeId++,
            x    = Random.nextFloat() * (canvasW - ENTITY_W),
            y    = -(ENTITY_H * 4),
            type = UpgradeType.DAMAGE
        ))

        enemies      = newEnemies
        upgrades     = newUpgrades
        waveActive   = true
        waveStarting = false
        waveTimer    = 0f
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    fun tick(dt: Float) {
        if (canvasW == 0f || gameOver || isPaused) return

        // Inter-wave pause
        if (waveStarting) {
            waveTimer -= dt
            if (waveTimer <= 0f) {
                round++
                startWave()
            }
            return
        }

        val baseSpeed = entitySpeed(round)
        val horizontalSpeed = enemyHorizontalSpeed(round)

        // ── Fire bullets ──────────────────────────────────────────────────────
        fireTimer += dt
        val newBullets = bullets.toMutableList()
        if (fireTimer >= fireInterval) {
            fireTimer -= fireInterval
            val playerTop = canvasH - PLAYER_HEIGHT - 16f
            newBullets += Bullet(x = playerX - BULLET_W / 2f, y = playerTop - BULLET_H)
        }

        // ── Move everything ───────────────────────────────────────────────────
        var activeBullets  = newBullets
            .map { it.copy(y = it.y - BULLET_SPEED * dt) }
            .filter { it.y + BULLET_H > 0f }
            .toMutableList()

        var activeEnemies  = enemies
            .map { enemy ->
                val speed = when (enemy.type) {
                    EnemyType.NORMAL -> baseSpeed
                    EnemyType.SPRINTER -> baseSpeed * 2f
                    EnemyType.TANK -> baseSpeed * 0.5f
                }
                
                // Calculate new position with horizontal movement
                var newX = enemy.x + (horizontalSpeed * enemy.horizontalDirection * dt)
                var newDirection = enemy.horizontalDirection
                
                // Bounce off edges
                if (newX < 0f) {
                    newX = 0f
                    newDirection = 1f
                } else if (newX + ENTITY_W > canvasW) {
                    newX = canvasW - ENTITY_W
                    newDirection = -1f
                }
                
                enemy.copy(
                    x = newX,
                    y = enemy.y + speed * dt,
                    horizontalDirection = newDirection
                )
            }
            .toMutableList()

        var activeUpgrades = upgrades
            .map { it.copy(y = it.y + baseSpeed * dt) }
            .filter { it.y < canvasH }
            .toMutableList()

        // ── Bullet × Enemy ────────────────────────────────────────────────────
        val bulletsToRemove = mutableSetOf<Bullet>()
        val enemyDamage     = mutableMapOf<Int, Int>()

        for (bullet in activeBullets) {
            for (enemy in activeEnemies) {
                if (overlaps(bullet.x, bullet.y, BULLET_W, BULLET_H,
                        enemy.x, enemy.y, ENTITY_W, ENTITY_H)) {
                    bulletsToRemove += bullet
                    enemyDamage[enemy.id] = (enemyDamage[enemy.id] ?: 0) + bulletDamage
                    break
                }
            }
        }
        activeBullets.removeAll(bulletsToRemove)
        activeEnemies = activeEnemies.map { enemy ->
            val newHp = enemy.hp - (enemyDamage[enemy.id] ?: 0)
            if (newHp <= 0) enemiesDestroyed++
            enemy.copy(hp = newHp)
        }.filter { it.hp > 0 }.toMutableList()

        // ── Bullet × Upgrade ──────────────────────────────────────────────────
        val bulletsToRemove2 = mutableSetOf<Bullet>()
        val upgradesHit      = mutableSetOf<Int>()

        for (bullet in activeBullets) {
            for (upgrade in activeUpgrades) {
                if (overlaps(bullet.x, bullet.y, BULLET_W, BULLET_H,
                        upgrade.x, upgrade.y, ENTITY_W, ENTITY_H)) {
                    bulletsToRemove2 += bullet
                    upgradesHit      += upgrade.id
                    applyUpgrade(upgrade.type)
                    upgradesCollected++
                    break
                }
            }
        }
        activeBullets.removeAll(bulletsToRemove2)
        activeUpgrades.removeAll { it.id in upgradesHit }

        // Commit state
        bullets  = activeBullets
        enemies  = activeEnemies
        upgrades = activeUpgrades

        // ── Player collision ──────────────────────────────────────────────────
        val playerTop  = canvasH - PLAYER_HEIGHT - 16f
        val playerLeft = playerX - PLAYER_WIDTH / 2f

        // Only game over if enemy touches player
        for (enemy in activeEnemies) {
            if (overlaps(enemy.x, enemy.y, ENTITY_W, ENTITY_H,
                    playerLeft, playerTop, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                gameOver = true
                onGameOver?.invoke(enemiesDestroyed * 10, round)
                return
            }
        }

        // ── Wave clear: only enemies need to be destroyed ─────────────────────
        if (waveActive && activeEnemies.isEmpty()) {
            waveActive   = false
            waveStarting = true
            waveTimer    = 3f
            bullets      = listOf()  // Clear bullets for fresh start
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyUpgrade(type: UpgradeType) {
        when (type) {
            UpgradeType.FIRE_RATE -> fireInterval = (fireInterval * 0.75f).coerceAtLeast(MIN_FIRE_INTERVAL)
            UpgradeType.DAMAGE    -> bulletDamage += 1
        }
    }

    private fun overlaps(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ) = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    fun movePlayer(delta: Float) {
        playerX = (playerX + delta).coerceIn(PLAYER_WIDTH / 2f, canvasW - PLAYER_WIDTH / 2f)
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun reset() {
        bullets  = listOf()
        enemies  = listOf()
        upgrades = listOf()
        fireInterval      = BASE_FIRE_INTERVAL
        bulletDamage      = BASE_DAMAGE
        round             = 1
        waveActive        = false
        waveStarting      = false
        waveTimer         = 0f
        nextEnemyId       = 0
        nextUpgradeId     = 0
        gameOver          = false
        enemiesDestroyed  = 0
        upgradesCollected = 0
        playerX = canvasW / 2f
        startWave()
    }
}
