/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

data class PetalParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val alpha: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val life: Float,
    val driftFrequency: Float,
    val isFromBurst: Boolean = false
)

private fun PetalParticle.step(): PetalParticle {
    val decay = if (isFromBurst) 0.014f else 0.005f
    val newLife = life - decay
    val newAlpha = (if (newLife < 0.3f) newLife / 0.3f else 1f).coerceIn(0f, 1f)
    // Gravity: only applied to burst fragments so they arc naturally
    val newVy = if (isFromBurst) vy + 0.11f else vy
    return copy(
        x = x + vx + if (!isFromBurst) sin(life * driftFrequency) * 1.5f else 0f,
        y = y + newVy,
        vy = newVy,
        rotation = rotation + rotationSpeed,
        life = newLife,
        alpha = newAlpha
    )
}

// Cherry-blossom petals for the victory storm
private val petalColors = listOf(
    Color(0xFFFFB7C5), Color(0xFFFFC0CB), Color(0xFFFAE1DD), Color(0xFFFFD1DC)
)

// Tile-fragment colors for the match burst — gold, red, jade, white, cyan
private val burstColors = listOf(
    Color(0xFFFFD700), // gold
    Color(0xFFFF4444), // red
    Color(0xFF44BB66), // jade green
    Color(0xFFFFFFFF), // white
    Color(0xFF00BFFF), // cyan (matches selection glow)
    Color(0xFFFF8C00), // orange
)

// ─────────────────────────────────────────────────────────────────────────────
// Spawns a radial burst of tile-fragment particles at a single position.
// Call once per matched tile for two independent explosions.
// ─────────────────────────────────────────────────────────────────────────────
private fun spawnBurst(pos: Offset, count: Int = 18): List<PetalParticle> {
    val angleStep = 360f / count
    return List(count) { i ->
        // Spread evenly around 360° with a small random jitter per particle
        val angle = angleStep * i + (Math.random().toFloat() * angleStep * 0.8f)
        val angleRad = Math.toRadians(angle.toDouble()).toFloat()
        // Vary speed so fragments don't all travel the same distance
        val speed = Math.random().toFloat() * 2.5f + 1.5f   // 1.5 – 4.0 px/frame (Slower for visual impact)
        val size  = Math.random().toFloat() * 7f + 3f       // 3 – 10 px
        PetalParticle(
            x = pos.x,
            y = pos.y,
            vx = cos(angleRad) * speed,
            vy = sin(angleRad) * speed,
            rotation = Math.random().toFloat() * 360f,
            rotationSpeed = Math.random().toFloat() * 18f - 9f,
            alpha = 1f,
            color = burstColors.random(),
            width = size,
            height = size * (Math.random().toFloat() * 0.6f + 0.5f), // slight rect variation
            life = 1.0f,
            driftFrequency = 0f,
            isFromBurst = true
        )
    }
}

@Composable
fun ParticleOverlay(
    triggerVictoryStorm: Boolean,
    selectionPositions: List<Offset> = emptyList(),
    modifier: Modifier = Modifier,
    isScoreEntryActive: Boolean = false
) {
    var particles by remember { mutableStateOf<List<PetalParticle>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Main physics loop — runs every frame
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                // Update particles
                particles = particles.map { it.step() }.filter { it.life > 0f }
            }
        }
    }

    // ── Match burst: wait 150ms for energy line, then explode!
    LaunchedEffect(selectionPositions) {
        if (selectionPositions.isNotEmpty()) {
            delay(150) // Wait for the energy line and tile implosion

            val newParticles = selectionPositions.flatMap { pos -> spawnBurst(pos) }
            particles = particles + newParticles
        }
    }

    // Victory Storm — unchanged
    LaunchedEffect(triggerVictoryStorm) {
        if (!triggerVictoryStorm) return@LaunchedEffect
        while (isActive) {
            val newPetals = List(3) {
                PetalParticle(
                    x = (Math.random().toFloat() * (canvasSize.width + 200f)) - 100f,
                    y = -50f,
                    vx = (Math.random().toFloat() * 1.2f - 0.6f),
                    vy = (Math.random().toFloat() * 2f + 2f),
                    rotation = Math.random().toFloat() * 360f,
                    rotationSpeed = Math.random().toFloat() * 4f - 2f,
                    alpha = 1f,
                    color = petalColors.random(),
                    width = 15f,
                    height = 10f,
                    life = 2.5f,
                    driftFrequency = Math.random().toFloat() * 3f + 1f,
                    isFromBurst = false
                )
            }
            particles = particles + newPetals
            withFrameMillis { }
        }
    }

    Canvas(modifier = modifier.fillMaxSize().zIndex(10f)) {
        canvasSize = size
        val centerX = size.width / 2
        val centerY = size.height / 2
        val left  = centerX - 180f
        val right = centerX + 180f
        val top   = centerY - 220f
        val bottom = centerY + 220f

        // Draw Particles
        particles.forEach { p ->
            val shouldSkip = !p.isFromBurst && isScoreEntryActive &&
                    p.x in left..right && p.y in top..bottom

            if (!shouldSkip) {
                withTransform({
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y))
                }) {
                    if (p.isFromBurst) {
                        // Tile fragments drawn as small rectangles
                        drawRect(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(p.x - p.width / 2f, p.y - p.height / 2f),
                            size = Size(p.width, p.height)
                        )
                    } else {
                        // Victory petals keep their oval shape
                        drawOval(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(p.x - p.width / 2f, p.y - p.height / 2f),
                            size = Size(p.width, p.height)
                        )
                    }
                }
            }
        }
    }
}