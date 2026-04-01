/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import com.rekluzgames.nikakudorimahjong.domain.model.*

data class GameUIState(
    val gameState: GameState = GameState.LOADING,
    val previousState: GameState = GameState.PLAYING,
    val board: List<List<Tile>> = emptyList(),
    val originalBoard: List<List<Tile>> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val selectedTile: Pair<Int, Int>? = null,
    val allAvailableHints: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> = emptyList(),
    val currentHintIndex: Int = -1,
    val shufflesRemaining: Int = 5,
    val undoHistory: List<List<List<Tile>>> = emptyList(),
    val aboutStage: Int = 0,
    val clearedAboutTiles: Set<Int> = emptySet(),
    val playerName: String = "",
    val highScores: Map<String, List<HighScore>> = emptyMap(),
    val selectedScoreTab: String = Difficulty.NORMAL.label,
    val lastMatchPath: List<Pair<Int, Int>>? = null,
    val lastMatchedPair: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
    val usedHint: Boolean = false,
    val usedShuffle: Boolean = false,
    val lastSavedScore: HighScore? = null,
    val backgroundImageName: String = "bg_001",
    val currentQuote: String = "",
    val isLayeredMode: Boolean = false,
    val currentLayeredLayout: LayeredLayout? = null,
    val layeredTiles: List<LayeredTile> = emptyList(),
    val originalLayeredTiles: List<LayeredTile> = emptyList(),
    val selectedLayeredTileId: Int? = null,
    val layeredHints: List<Pair<Int, Int>> = emptyList(),
    val currentLayeredHintIndex: Int = -1,
    val layeredUndoHistory: List<List<LayeredTile>> = emptyList()
) {
    val remainingTilesCount: Int get() = if (isLayeredMode)
        layeredTiles.count { !it.isRemoved }
    else
        board.flatten().count { !it.isRemoved }

    val totalTilesCount: Int get() = if (isLayeredMode)
        layeredTiles.size
    else
        board.flatten().count()

    val canFinish: Boolean get() = remainingTilesCount in 1..12

    val canUndo: Boolean get() = if (isLayeredMode)
        layeredUndoHistory.isNotEmpty()
    else
        undoHistory.isNotEmpty()

    val activeHint: Pair<Pair<Int, Int>, Pair<Int, Int>>?
        get() = if (currentHintIndex in allAvailableHints.indices)
            allAvailableHints[currentHintIndex]
        else null

    val activeLayeredHint: Pair<Int, Int>?
        get() = if (currentLayeredHintIndex in layeredHints.indices)
            layeredHints[currentLayeredHintIndex]
        else null

    val earnedMedals: List<Medal>
        get() = buildList {
            if (!usedHint) add(Medal.SNIPER)
            if (!usedShuffle) add(Medal.STRATEGIST)
        }

    val boardOverlayAlpha: Float
        get() = if (totalTilesCount == 0) 0f else
            (remainingTilesCount.toFloat() / totalTilesCount.toFloat())
                .coerceIn(0f, 0.85f)
}