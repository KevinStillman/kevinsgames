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

const val WALL_H             = 24f   // height of every wall segment

private fun entitySpeed(round: Int) = 120f + (round - 1) * 30f
const val BASE_FIRE_INTERVAL = 0.4f
const val MIN_FIRE_INTERVAL  = 0.08f
const val BASE_DAMAGE        = 1
private fun enemyCountForRound(round: Int) = 2 + round

// ── Data classes ─────────────────────────────────────────────────────────────

data class Bullet(val x: Float, val y: Float)

data class Enemy(
    val id: Int,
    val x: Float,
    val y: Float,
    val hp: Int,
    val maxHp: Int
)

data class Upgrade(
    val id: Int,
    val x: Float,
    val y: Float,
    val type: UpgradeType
)

enum class UpgradeType { FIRE_RATE, DAMAGE }

/**
 * One solid segment of a horizontal wall.  A wall "row" is made of 1–2
 * segments separated by a gap wide enough for the player to pass through.
 * [x] / [y] = top-left corner; [w] = width; height is always [WALL_H].
 */
data class Wall(
    val id: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val hp: Int,
    val maxHp: Int
)

// ── Game state ───────────────────────────────────────────────────────────────

class ShooterGameState {

    // Canvas
    var canvasW by mutableStateOf(0f)
    var canvasH by mutableStateOf(0f)

    // Player
    var playerX by mutableStateOf(0f)

    // Projectiles
    var bullets  by mutableStateOf(listOf<Bullet>())

    // Enemies, upgrades & walls
    var enemies  by mutableStateOf(listOf<Enemy>())
    var upgrades by mutableStateOf(listOf<Upgrade>())
    var walls    by mutableStateOf(listOf<Wall>())

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
    private var nextWallId    = 0

    // Internal fire timer
    private var fireTimer = 0f

    // Game over & stats
    var gameOver          by mutableStateOf(false)
    var enemiesDestroyed  by mutableStateOf(0)
    var upgradesCollected by mutableStateOf(0)

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

        val newEnemies = List(count) { i ->
            val fraction = if (count == 1) 0.5f else i.toFloat() / (count - 1).toFloat()
            Enemy(
                id    = nextEnemyId++,
                x     = margin + fraction * usableW,
                y     = -(ENTITY_H + i * (ENTITY_H + 24f)),
                hp    = round,
                maxHp = round
            )
        }

        val upgradeX    = Random.nextFloat() * (canvasW - ENTITY_W)
        val upgradeType = if (Random.nextBoolean()) UpgradeType.FIRE_RATE else UpgradeType.DAMAGE
        val newUpgrade  = Upgrade(
            id   = nextUpgradeId++,
            x    = upgradeX,
            y    = -(ENTITY_H * 3),
            type = upgradeType
        )

        // 1–3 wall rows, each placed above the enemy formation
        val wallRowCount = Random.nextInt(1, 4)
        val wallHp       = round * 2
        val topEnemyY    = -(ENTITY_H + (count - 1) * (ENTITY_H + 24f))
        val newWalls     = (1..wallRowCount).flatMap { i ->
            val rowY = topEnemyY - i * (WALL_H + 80f)
            buildWallRow(rowY, wallHp)
        }

        enemies      = newEnemies
        upgrades     = listOf(newUpgrade)
        walls        = newWalls
        waveActive   = true
        waveStarting = false
        waveTimer    = 0f
    }

    /**
     * Splits a wall row at [y] into up to two solid segments with a player-
     * sized gap somewhere in the middle so the level stays navigable.
     */
    private fun buildWallRow(y: Float, hp: Int): List<Wall> {
        val minGap  = PLAYER_WIDTH * 1.6f
        val maxGap  = canvasW * 0.55f
        val gapW    = Random.nextFloat() * (maxGap - minGap) + minGap
        val gapLeft = Random.nextFloat() * (canvasW - gapW)
        val gapRight = gapLeft + gapW

        val segments = mutableListOf<Wall>()
        if (gapLeft > 2f)
            segments += Wall(nextWallId++, x = 0f,      y = y, w = gapLeft,            hp = hp, maxHp = hp)
        if (gapRight < canvasW - 2f)
            segments += Wall(nextWallId++, x = gapRight, y = y, w = canvasW - gapRight, hp = hp, maxHp = hp)
        return segments
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    fun tick(dt: Float) {
        if (canvasW == 0f || gameOver) return

        // Inter-wave pause
        if (waveStarting) {
            waveTimer -= dt
            if (waveTimer <= 0f) {
                round++
                startWave()
            }
            return
        }

        val speed = entitySpeed(round)

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
            .map { it.copy(y = it.y + speed * dt) }
            .toMutableList()

        var activeUpgrades = upgrades
            .map { it.copy(y = it.y + speed * dt) }
            .filter { it.y < canvasH }
            .toMutableList()

        var activeWalls    = walls
            .map { it.copy(y = it.y + speed * dt) }
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

        // ── Bullet × Wall ─────────────────────────────────────────────────────
        val bulletsToRemove3 = mutableSetOf<Bullet>()
        val wallDamage       = mutableMapOf<Int, Int>()

        for (bullet in activeBullets) {
            for (wall in activeWalls) {
                if (overlaps(bullet.x, bullet.y, BULLET_W, BULLET_H,
                        wall.x, wall.y, wall.w, WALL_H)) {
                    bulletsToRemove3 += bullet
                    wallDamage[wall.id] = (wallDamage[wall.id] ?: 0) + bulletDamage
                    break
                }
            }
        }
        activeBullets.removeAll(bulletsToRemove3)
        activeWalls = activeWalls.map { wall ->
            wall.copy(hp = wall.hp - (wallDamage[wall.id] ?: 0))
        }.filter { it.hp > 0 }.toMutableList()

        // Commit state
        bullets  = activeBullets
        enemies  = activeEnemies
        upgrades = activeUpgrades
        walls    = activeWalls

        // ── Player collision ──────────────────────────────────────────────────
        val playerTop  = canvasH - PLAYER_HEIGHT - 16f
        val playerLeft = playerX - PLAYER_WIDTH / 2f

        for (enemy in activeEnemies) {
            if (overlaps(enemy.x, enemy.y, ENTITY_W, ENTITY_H,
                    playerLeft, playerTop, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                gameOver = true; return
            }
            if (enemy.y + ENTITY_H >= canvasH) {
                gameOver = true; return
            }
        }

        for (wall in activeWalls) {
            if (overlaps(wall.x, wall.y, wall.w, WALL_H,
                    playerLeft, playerTop, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                gameOver = true; return
            }
        }

        // ── Wave clear: enemies AND walls must all be destroyed ───────────────
        if (waveActive && activeEnemies.isEmpty() && activeWalls.isEmpty()) {
            waveActive   = false
            waveStarting = true
            waveTimer    = 3f
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

    fun reset() {
        bullets  = listOf()
        enemies  = listOf()
        upgrades = listOf()
        walls    = listOf()
        fireInterval      = BASE_FIRE_INTERVAL
        bulletDamage      = BASE_DAMAGE
        round             = 1
        waveActive        = false
        waveStarting      = false
        waveTimer         = 0f
        nextEnemyId       = 0
        nextUpgradeId     = 0
        nextWallId        = 0
        gameOver          = false
        enemiesDestroyed  = 0
        upgradesCollected = 0
        playerX = canvasW / 2f
        startWave()
    }
}
