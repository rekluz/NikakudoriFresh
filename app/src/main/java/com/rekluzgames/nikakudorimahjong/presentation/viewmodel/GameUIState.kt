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
    val gameMode: GameMode = GameMode.REGULAR,
    val selectedTile: Pair<Int, Int>? = null,
    val allAvailableHints: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> = emptyList(),
    val currentHintIndex: Int = -1,
    val shufflesRemaining: Int = 5,
    val undoHistory: List<List<List<Tile>>> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isFullScreen: Boolean = false,
    val aboutStage: Int = 0,
    val clearedAboutTiles: Set<Int> = emptySet(),
    val version: String = "",
    val playerName: String = "",
    val highScores: Map<String, List<HighScore>> = emptyMap(),
    val selectedScoreTab: String = Difficulty.NORMAL.label,
    val lastMatchPath: List<Pair<Int, Int>>? = null,
    val lastMatchedPair: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
    val usedHint: Boolean = false,
    val usedShuffle: Boolean = false,
    val lastSavedScore: HighScore? = null,
    // Randomly selected at the start of each new game
    val backgroundImageName: String = "bg_001",
    val currentQuote: String = ""
) {
    val remainingTilesCount: Int get() = board.flatten().count { !it.isRemoved }
    val totalTilesCount: Int get() = board.flatten().count()
    val canFinish: Boolean get() = remainingTilesCount in 1..12
    val canUndo: Boolean get() = undoHistory.isNotEmpty()

    val activeHint: Pair<Pair<Int, Int>, Pair<Int, Int>>?
        get() = if (currentHintIndex in allAvailableHints.indices) allAvailableHints[currentHintIndex] else null

    // FLASH medal is NOT computed here — it requires timeSeconds, which lives in
    // the ViewModel's dedicated _timeSeconds flow. earnedMedals is finalised at
    // save-time inside saveScoreAndShowBoard(), where _timeSeconds.value is available.
    val earnedMedals: List<Medal>
        get() = buildList {
            if (!usedHint) add(Medal.SNIPER)
            if (!usedShuffle) add(Medal.STRATEGIST)
        }

    // Overlay alpha for the board darkening effect.
    // Starts near 1.0 (fully hidden) and approaches 0.0 as tiles are removed.
    // A small minimum of 0.15 keeps the image slightly visible even at the start.
    val boardOverlayAlpha: Float
        get() = if (totalTilesCount == 0) 0f else
            (remainingTilesCount.toFloat() / totalTilesCount.toFloat())
                .coerceIn(0f, 0.85f)
}

data class HighScore(
    val name: String,
    val time: Int,
    val difficulty: String,
    val medals: List<Medal> = emptyList()
) {
    val timeFormatted: String get() = "%02d:%02d".format(time / 60, time % 60)

    fun serialise(): String {
        val base = "$name|$time|$difficulty"
        return if (medals.isEmpty()) base else "$base|${medals.joinToString(",") { it.name }}"
    }

    companion object {
        fun deserialise(raw: String): HighScore? {
            return try {
                val parts = raw.split("|")
                if (parts.size < 3) return null
                val medals = if (parts.size >= 4 && parts[3].isNotBlank()) {
                    parts[3].split(",").mapNotNull {
                        try { Medal.valueOf(it.trim()) } catch (e: Exception) { null }
                    }
                } else emptyList()
                HighScore(parts[0], parts[1].toInt(), parts[2], medals)
            } catch (e: Exception) { null }
        }
    }
}