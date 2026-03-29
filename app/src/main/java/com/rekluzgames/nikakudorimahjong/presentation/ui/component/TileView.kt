/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rekluzgames.nikakudorimahjong.domain.model.Tile

@Composable
fun TileView(
    tile: Tile,
    isSelected: Boolean,
    isHinted: Boolean,
    isExploding: Boolean,
    width: Float,
    height: Float,
    xOffset: Float,
    yOffset: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // -------------------------------------------------------------------------
    // 3D DEPTH ADJUSTMENT (TWEAK THESE VALUES)
    // -------------------------------------------------------------------------
    val depthRight = 4.5.dp
    val depthBottom = 4.dp
    val borderThickness = 2.dp

    // Smooth gravity movement
    val animatedX by animateFloatAsState(
        targetValue = xOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "slideX"
    )
    val animatedY by animateFloatAsState(
        targetValue = yOffset,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "slideY"
    )

    // Pulsing glow for hint
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    // 1. Calculate the target scale: 1.2f stretch for explosion, 1.1f pop for selection
    val targetScale = when {
        isExploding -> 1.2f  // Pre-burst implosion stretch (150ms window)
        //  isSelected -> 1.1f   // Tactile selection pop (NO TILT)
        else -> 1.0f         // Normal state
    }

    // Animate scale: Use a bouncy spring for selection, and a smooth tween for the explosion.
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isExploding) tween(durationMillis = 150) else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // 2. Shrink to absolutely nothing starting exactly at 150ms (The Implosion)
    AnimatedVisibility(
        visible = !tile.isRemoved,
        enter = fadeIn(),
        exit = scaleOut(
            animationSpec = tween(durationMillis = 100, delayMillis = 150, easing = FastOutLinearInEasing),
            targetScale = 0.0f // Shrink to 0 violently
        ) + fadeOut(
            animationSpec = tween(durationMillis = 100, delayMillis = 150)
        )
    ) {
        Box(
            modifier = Modifier
                .size(width.dp, height.dp)
                .offset(animatedX.dp, animatedY.dp)
                .clip(RectangleShape)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    // NO rotationZ applied here, so the tile stays straight!
                }
                .clickable { onClick() },
            contentAlignment = Alignment.TopStart
        ) {
            val resId = remember(tile.imageName) {
                context.resources.getIdentifier(tile.imageName, "drawable", context.packageName)
            }

            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // SELECTION OVERLAY
            // SELECTION OVERLAY
            if (isSelected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 0.dp, top = 0.dp, end = depthRight, bottom = depthBottom)
                        .border(borderThickness, Color.Cyan.copy(alpha = glowAlpha), RectangleShape)
                        .background(Color(0xFF00BFFF).copy(alpha = glowAlpha * 0.4f))
                )
            }


            // HINT OVERLAY
            if (isHinted) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 0.dp, top = 0.dp, end = depthRight, bottom = depthBottom)
                        .border(borderThickness, Color.Yellow.copy(alpha = glowAlpha), RectangleShape)
                        .background(Color(0xFFFFEB3B).copy(alpha = glowAlpha * 0.4f))
                )
            }
        }
    }
}