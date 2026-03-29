/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
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
    val currentQuote: String = ""
) {
    val remainingTilesCount: Int get() = board.flatten().count { !it.isRemoved }
    val totalTilesCount: Int get() = board.flatten().count()
    val canFinish: Boolean get() = remainingTilesCount in 1..12
    val canUndo: Boolean get() = undoHistory.isNotEmpty()

    val activeHint: Pair<Pair<Int, Int>, Pair<Int, Int>>?
        get() = if (currentHintIndex in allAvailableHints.indices) allAvailableHints[currentHintIndex] else null

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