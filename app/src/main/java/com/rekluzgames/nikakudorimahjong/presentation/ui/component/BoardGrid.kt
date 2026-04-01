/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import com.rekluzgames.nikakudorimahjong.presentation.effects.ParticleOverlay
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameUIState
import kotlin.math.abs

// =============================================================================
// Entry point — branches on game mode
// =============================================================================

@Composable
fun BoardGrid(
    uiState: GameUIState,
    onTileClick: (Int, Int) -> Unit,
    onLayeredTileClick: (Int) -> Unit = {}
) {
    if (uiState.isLayeredMode) {
        LayeredBoardGrid(uiState = uiState, onTileClick = onLayeredTileClick)
        return
    }

    // =========================================================================
    // Flat board — unchanged
    // =========================================================================

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 45.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        val availableWidth  = maxWidth.value
        val availableHeight = maxHeight.value

        val tileAspectRatio = 0.74f
        val hOverlap        = 0.88f
        val vOverlap        = 0.80f

        val maxTileWidth  = availableWidth  / (1f + (uiState.difficulty.cols - 1) * hOverlap)
        val maxTileHeight = availableHeight / (1f + (uiState.difficulty.rows - 1) * vOverlap)

        val tileHeight = if (maxTileWidth / tileAspectRatio < maxTileHeight)
            maxTileWidth / tileAspectRatio else maxTileHeight
        val tileWidth  = tileHeight * tileAspectRatio

        val xStep = tileWidth  * hOverlap
        val yStep = tileHeight * vOverlap

        val gridWidth  = xStep * (uiState.difficulty.cols - 1) + tileWidth
        val gridHeight = yStep * (uiState.difficulty.rows - 1) + tileHeight

        val animatedOverlayAlpha by animateFloatAsState(
            targetValue    = uiState.boardOverlayAlpha,
            animationSpec  = tween(durationMillis = 600),
            label          = "boardReveal"
        )

        val burstPositions: List<Offset> = remember(uiState.lastMatchedPair) {
            uiState.lastMatchedPair?.let { (p1, p2) ->
                with(density) {
                    listOf(
                        Offset(
                            x = (xStep * p1.second + tileWidth  / 2f).dp.toPx(),
                            y = (yStep * p1.first  + tileHeight / 2f).dp.toPx()
                        ),
                        Offset(
                            x = (xStep * p2.second + tileWidth  / 2f).dp.toPx(),
                            y = (yStep * p2.first  + tileHeight / 2f).dp.toPx()
                        )
                    )
                }
            } ?: emptyList()
        }

        Box(modifier = Modifier.size(width = gridWidth.dp, height = gridHeight.dp)) {

            if (animatedOverlayAlpha > 0f) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(0f)
                ) {
                    drawRect(
                        color    = Color(0xFF0D1B2A).copy(alpha = animatedOverlayAlpha),
                        topLeft  = Offset.Zero,
                        size     = Size(size.width, size.height)
                    )
                }
            }

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

                        Box(modifier = Modifier.zIndex(if (tile.isRemoved) 0f else zPos)) {
                            TileView(
                                tile        = tile,
                                isSelected  = uiState.selectedTile == r to c,
                                isHinted    = isHint,
                                isExploding = isExploding,
                                width       = tileWidth,
                                height      = tileHeight,
                                xOffset     = xStep * c,
                                yOffset     = yStep * r
                            ) { onTileClick(r, c) }
                        }
                    }
                }
            }

            val pathPoints = uiState.lastMatchPath
            if (pathPoints != null && pathPoints.size >= 2) {
                var lineProgress by remember { mutableStateOf(0f) }
                LaunchedEffect(pathPoints) {
                    lineProgress = 0f
                    animate(
                        initialValue  = 0f,
                        targetValue   = 1f,
                        animationSpec = tween(durationMillis = 150, easing = LinearEasing)
                    ) { value, _ -> lineProgress = value }
                }

                Canvas(
                    modifier = Modifier
                        .size(width = gridWidth.dp, height = gridHeight.dp)
                        .zIndex(Float.MAX_VALUE)
                ) {
                    val rows = uiState.difficulty.rows
                    val cols = uiState.difficulty.cols

                    val outsideMarginX = with(density) { (tileWidth  * 0.6f).dp.toPx() }
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
                    val fullPath = Path().apply {
                        moveTo(pixelPoints[0].x, pixelPoints[0].y)
                        for (i in 1 until pixelPoints.size) lineTo(pixelPoints[i].x, pixelPoints[i].y)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, false)
                    val animatedPath = Path()
                    pathMeasure.getSegment(0f, pathMeasure.length * lineProgress, animatedPath, true)

                    drawPath(
                        path  = animatedPath,
                        color = Color(0xFF00BFFF).copy(alpha = 0.8f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path  = animatedPath,
                        color = Color.White.copy(alpha = 0.9f),
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            ParticleOverlay(
                triggerVictoryStorm = false,
                selectionPositions  = burstPositions,
                modifier            = Modifier
                    .matchParentSize()
                    .zIndex(Float.MAX_VALUE),
                isScoreEntryActive  = false
            )
        }
    }
}

// =============================================================================
// Layered board rendering
// =============================================================================

@Composable
private fun LayeredBoardGrid(
    uiState: GameUIState,
    onTileClick: (Int) -> Unit
) {
    val tiles = uiState.layeredTiles
    if (tiles.isEmpty()) return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 45.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        val tileAspectRatio = 0.74f
        val layerOffsetX    = 6f
        val layerOffsetY    = 6f

        val maxCol   = tiles.maxOf { it.col }
        val maxRow   = tiles.maxOf { it.row }
        val maxLayer = tiles.maxOf { it.layer }

        val numCols = maxCol / 2 + 1
        val numRows = maxRow / 2 + 1

        val usableWidth  = maxWidth.value  - maxLayer * layerOffsetX
        val usableHeight = maxHeight.value - maxLayer * layerOffsetY

        val tileWidth  = minOf(
            usableWidth  / numCols,
            usableHeight * tileAspectRatio / numRows
        )
        val tileHeight = tileWidth / tileAspectRatio

        val halfStep  = tileWidth  / 2f
        val halfStepH = tileHeight / 2f

        val gridWidth  = (maxCol + 2) * halfStep  + maxLayer * layerOffsetX
        val gridHeight = (maxRow + 2) * halfStepH + maxLayer * layerOffsetY

        val freeTileIds: Set<Int> = remember(tiles) {
            tiles.filter { tile ->
                if (tile.isRemoved) return@filter false
                val blockedAbove = tiles.any { o ->
                    !o.isRemoved &&
                            o.layer == tile.layer + 1 &&
                            abs(o.col - tile.col) < 2 &&
                            abs(o.row - tile.row) < 2
                }
                if (blockedAbove) return@filter false
                val blockedLeft = tiles.any { o ->
                    !o.isRemoved &&
                            o.layer == tile.layer &&
                            o.col == tile.col - 2 &&
                            abs(o.row - tile.row) < 2
                }
                val blockedRight = tiles.any { o ->
                    !o.isRemoved &&
                            o.layer == tile.layer &&
                            o.col == tile.col + 2 &&
                            abs(o.row - tile.row) < 2
                }
                !blockedLeft || !blockedRight
            }.map { it.id }.toSet()
        }

        Box(modifier = Modifier.size(width = gridWidth.dp, height = gridHeight.dp)) {
            tiles
                .filter { !it.isRemoved }
                .sortedWith(compareBy({ it.layer }, { it.row }, { it.col }))
                .forEach { tile ->
                    key(tile.id) {
                        val xOffset = tile.col * halfStep  + tile.layer              * layerOffsetX
                        val yOffset = tile.row * halfStepH + (maxLayer - tile.layer) * layerOffsetY

                        val isFree     = tile.id in freeTileIds
                        val isSelected = uiState.selectedLayeredTileId == tile.id
                        val isHinted   = uiState.activeLayeredHint?.let {
                            it.first == tile.id || it.second == tile.id
                        } ?: false

                        val zIndex = tile.layer * 10_000f + tile.row * 10f + tile.col

                        Box(
                            modifier = Modifier
                                .zIndex(zIndex)
                                .alpha(if (isFree) 1f else 0.4f)
                        ) {
                            TileView(
                                tile = Tile(
                                    id        = tile.id,
                                    type      = tile.type,
                                    isRemoved = tile.isRemoved
                                ),
                                isSelected  = isSelected,
                                isHinted    = isHinted,
                                isExploding = false,
                                width       = tileWidth,
                                height      = tileHeight,
                                xOffset     = xOffset,
                                yOffset     = yOffset
                            ) {
                                if (isFree) onTileClick(tile.id)
                            }
                        }
                    }
                }
        }
    }
}