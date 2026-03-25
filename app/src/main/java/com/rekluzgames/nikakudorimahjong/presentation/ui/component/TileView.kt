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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    // These values "shrink" the glow specifically from the bottom and right
    // to avoid highlighting the 3D "sides" of your tile assets.
    val depthRight = 4.5.dp
    val depthBottom = 4.dp

    // Thickness of the glow line (visible thickness is half this due to parent clip)
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
            repeatMode = RepeatMode.Reverse // Fixed: Corrected reference and added RepeatMode
        ), label = "alpha"
    )

    // Match animation state
    val shakeX = remember { Animatable(0f) }
    val vanishAlpha = remember { Animatable(1f) }

    LaunchedEffect(isExploding) {
        if (isExploding) {
            coroutineScope {
                launch {
                    val shakeDistance = width * 0.12f
                    repeat(4) {
                        shakeX.animateTo(shakeDistance, tween(55, easing = LinearEasing))
                        shakeX.animateTo(-shakeDistance, tween(55, easing = LinearEasing))
                    }
                    shakeX.animateTo(0f, animationSpec = tween(40))
                }
                launch {
                    kotlinx.coroutines.delay(180L)
                    vanishAlpha.animateTo(0f, tween(150, easing = FastOutLinearInEasing))
                }
            }
        } else {
            shakeX.snapTo(0f)
            vanishAlpha.snapTo(1f)
        }
    }

    AnimatedVisibility(
        visible = !tile.isRemoved,
        enter = fadeIn(),
        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.85f)
    ) {
        // Main Box defining the tile footprint. Layout remains untouched.
        Box(
            modifier = Modifier
                .size(width.dp, height.dp)
                .offset(animatedX.dp, animatedY.dp)
                // CLIP: Essential. It chops off the 1dp bleed on the top/left
                // so the glow looks flush without overlapping neighbors.
                .clip(RectangleShape)
                .graphicsLayer {
                    if (isExploding) {
                        translationX = shakeX.value
                        alpha = vanishAlpha.value
                    }
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
            if (isSelected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // ASYMMETRIC PADDING: Pulls the glow in from the 3D sides,
                        // but stays locked at 0.dp on the Top and Left.
                        .padding(start = 0.dp, top = 0.dp, end = depthRight, bottom = depthBottom)
                        .border(borderThickness, Color.Cyan, RectangleShape)
                        .background(Color(0xFF00BFFF).copy(alpha = 0.4f))
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