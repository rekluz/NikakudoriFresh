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
import kotlinx.coroutines.isActive
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
    val decay = if (isFromBurst) 0.02f else 0.005f
    val newLife = life - decay
    val newAlpha = (if (newLife < 0.2f) newLife * 5f else 1f).coerceIn(0f, 1f)
    return copy(
        x = x + vx + (sin(life * driftFrequency) * 1.5f),
        y = y + vy,
        rotation = rotation + rotationSpeed,
        life = newLife,
        alpha = newAlpha
    )
}

private val petalColors = listOf(
    Color(0xFFFFB7C5), Color(0xFFFFC0CB), Color(0xFFFAE1DD), Color(0xFFFFD1DC)
)

@Composable
fun ParticleOverlay(
    triggerVictoryStorm: Boolean,
    selectionPos: Offset?,
    modifier: Modifier = Modifier,
    isScoreEntryActive: Boolean = false
) {
    var particles by remember { mutableStateOf<List<PetalParticle>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                particles = particles.map { it.step() }.filter { it.life > 0f }
            }
        }
    }

    // Gameplay selection burst
    LaunchedEffect(selectionPos) {
        selectionPos?.let { pos ->
            val burst = List(10) {
                PetalParticle(
                    x = pos.x,
                    y = pos.y,
                    vx = (Math.random().toFloat() * 8f - 4f),
                    vy = (Math.random().toFloat() * -5f - 2f),
                    rotation = Math.random().toFloat() * 360f,
                    rotationSpeed = Math.random().toFloat() * 12f - 6f,
                    alpha = 1f,
                    color = petalColors.random(),
                    width = 10f,
                    height = 7f,
                    life = 1.0f,
                    driftFrequency = Math.random().toFloat() * 4f,
                    isFromBurst = true
                )
            }
            particles = particles + burst
        }
    }

    // Victory Storm
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
        val left = centerX - 180f
        val right = centerX + 180f
        val top = centerY - 220f
        val bottom = centerY + 220f

        particles.forEach { p ->
            val shouldSkip = !p.isFromBurst && isScoreEntryActive &&
                    p.x in left..right && p.y in top..bottom

            if (!shouldSkip) {
                withTransform({
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y))
                }) {
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