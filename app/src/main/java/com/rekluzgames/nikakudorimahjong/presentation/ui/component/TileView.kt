/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
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

import androidx.compose.ui.unit.IntOffset
import android.annotation.SuppressLint

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun TileView(
    tile: Tile,
    isSelected: Boolean,
    isHinted: Boolean,
    isExploding: Boolean,
    isBlocked: Boolean, // <--- New Parameter
    width: Float,
    height: Float,
    xOffset: Float,
    yOffset: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val depthRight = 4.5.dp
    val depthBottom = 4.dp
    val borderThickness = 2.dp

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

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    val targetScale = when {
        isExploding -> 1.2f
        else -> 1.0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isExploding) tween(durationMillis = 150) else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    AnimatedVisibility(
        visible = !tile.isRemoved,
        enter = fadeIn(),
        exit = scaleOut(
            animationSpec = tween(durationMillis = 100, delayMillis = 150, easing = FastOutLinearInEasing),
            targetScale = 0.0f
        ) + fadeOut(
            animationSpec = tween(durationMillis = 100, delayMillis = 150)
        )
    ) {
        Box(
            modifier = Modifier
                .size(width.dp, height.dp)
                .offset { IntOffset(animatedX.dp.roundToPx(), animatedY.dp.roundToPx()) }
                .clip(RectangleShape)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
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

            // BLOCKED OVERLAY
            if (isBlocked) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(end = depthRight, bottom = depthBottom)
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

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