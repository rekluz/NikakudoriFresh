/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameUIState

@Composable
fun BoardGrid(uiState: GameUIState, onTileClick: (Int, Int) -> Unit) {

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 45.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        val availableWidth = maxWidth.value
        val availableHeight = maxHeight.value

        val tileAspectRatio = 0.74f
        val hOverlap = 0.88f
        val vOverlap = 0.80f

        val maxTileWidth = availableWidth / (1f + (uiState.difficulty.cols - 1) * hOverlap)
        val maxTileHeight = availableHeight / (1f + (uiState.difficulty.rows - 1) * vOverlap)

        val tileHeight = if (maxTileWidth / tileAspectRatio < maxTileHeight) {
            maxTileWidth / tileAspectRatio
        } else {
            maxTileHeight
        }

        val tileWidth = tileHeight * tileAspectRatio

        val xStep = tileWidth * hOverlap
        val yStep = tileHeight * vOverlap

        val gridWidth = xStep * (uiState.difficulty.cols - 1) + tileWidth
        val gridHeight = yStep * (uiState.difficulty.rows - 1) + tileHeight

        // Smoothly animate the overlay alpha so the reveal feels gradual
        // rather than jumping on each match.
        val animatedOverlayAlpha by animateFloatAsState(
            targetValue = uiState.boardOverlayAlpha,
            animationSpec = tween(durationMillis = 600),
            label = "boardReveal"
        )

        // --- Compute Burst Position ---
        val burstOffsetPx: Offset? = remember(uiState.lastMatchedPair) {
            uiState.lastMatchedPair?.let { (p1, p2) ->
                val x1 = (xStep * p1.second + tileWidth / 2f)
                val y1 = (yStep * p1.first + tileHeight / 2f)
                val x2 = (xStep * p2.second + tileWidth / 2f)
                val y2 = (yStep * p2.first + tileHeight / 2f)
                with(density) {
                    Offset(
                        x = ((x1 + x2) / 2f).dp.toPx(),
                        y = ((y1 + y2) / 2f).dp.toPx()
                    )
                }
            }
        }

        Box(
            modifier = Modifier.size(width = gridWidth.dp, height = gridHeight.dp)
        ) {

            // --- Background Reveal Overlay ---
            // Sits at zIndex 0 behind all tiles. Starts nearly opaque and
            // fades away as tiles are removed, revealing the background image.
            // MidnightBlue matches the screen background so the reveal is
            // seamless rather than showing a hard black rectangle.
            if (animatedOverlayAlpha > 0f) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(0f)
                ) {
                    drawRect(
                        color = Color(0xFF0D1B2A).copy(alpha = animatedOverlayAlpha),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height)
                    )
                }
            }

            // --- Tile Layer ---
            uiState.board.forEachIndexed { r, row ->
                row.forEachIndexed { c, tile ->
                    key(tile.id) {

                        val isHint = uiState.activeHint?.let {
                            (it.first == r to c) || (it.second == r to c)
                        } ?: false

                        val isExploding = uiState.lastMatchedPair?.let {
                            (it.first == r to c) || (it.second == r to c)
                        } ?: false

                        val zPos = (r * 100 + c).toFloat()

                        Box(
                            modifier = Modifier.zIndex(if (tile.isRemoved) 0f else zPos)
                        ) {
                            TileView(
                                tile = tile,
                                isSelected = uiState.selectedTile == r to c,
                                isHinted = isHint,
                                isExploding = isExploding,
                                width = tileWidth,
                                height = tileHeight,
                                xOffset = xStep * c,
                                yOffset = yStep * r
                            ) {
                                onTileClick(r, c)
                            }
                        }
                    }
                }
            }

            // --- Connection Line Overlay ---
            val pathPoints = uiState.lastMatchPath
            if (pathPoints != null && pathPoints.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .size(width = gridWidth.dp, height = gridHeight.dp)
                        .zIndex(Float.MAX_VALUE)
                ) {
                    val rows = uiState.difficulty.rows
                    val cols = uiState.difficulty.cols

                    val outsideMarginX = with(density) { (tileWidth * 0.6f).dp.toPx() }
                    val outsideMarginY = with(density) { (tileHeight * 0.6f).dp.toPx() }

                    fun getPointPx(row: Int, col: Int): Offset {
                        val px = when {
                            col < 0     -> -outsideMarginX
                            col >= cols -> (xStep * (cols - 1) + tileWidth).dp.toPx() + outsideMarginX
                            else        -> (xStep * col + tileWidth / 2f).dp.toPx()
                        }
                        val py = when {
                            row < 0     -> -outsideMarginY
                            row >= rows -> (yStep * (rows - 1) + tileHeight).dp.toPx() + outsideMarginY
                            else        -> (yStep * row + tileHeight / 2f).dp.toPx()
                        }
                        return Offset(px, py)
                    }

                    val pixelPoints = pathPoints.map { (r, c) -> getPointPx(r, c) }

                    val linePath = Path().apply {
                        moveTo(pixelPoints[0].x, pixelPoints[0].y)
                        for (i in 1 until pixelPoints.size) {
                            lineTo(pixelPoints[i].x, pixelPoints[i].y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = Color(0xFF00BFFF).copy(alpha = 0.8f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    drawPath(
                        path = linePath,
                        color = Color.White.copy(alpha = 0.9f),
                        style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // --- Local Particle Layer ---
            ParticleOverlay(
                triggerVictoryStorm = false,
                selectionPos = burstOffsetPx,
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(Float.MAX_VALUE),
                isScoreEntryActive = false
            )
        }
    }
}