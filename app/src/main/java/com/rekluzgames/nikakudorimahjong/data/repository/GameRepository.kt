/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.repository

import com.rekluzgames.nikakudorimahjong.data.preference.PreferenceManager
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameMode
import com.rekluzgames.nikakudorimahjong.domain.model.HighScore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val prefs: PreferenceManager
) {
    // Settings reads
    fun isSoundEnabled() = prefs.isSoundEnabled()
    fun isVibrationEnabled() = prefs.isVibrationEnabled()
    fun isFullScreen() = prefs.isFullScreen()
    fun getGameMode() = prefs.getGameMode()

    // Settings writes
    fun setSoundEnabled(v: Boolean) = prefs.setSoundEnabled(v)
    fun setVibrationEnabled(v: Boolean) = prefs.setVibrationEnabled(v)
    fun setFullScreen(v: Boolean) = prefs.setFullScreen(v)
    fun setGameMode(mode: GameMode) = prefs.setGameMode(mode)

    // Scores
    fun getAllHighScores(): Map<String, List<HighScore>> =
        Difficulty.entries.associate { it.label to getHighScores(it.label) }

    fun getHighScores(diffLabel: String): List<HighScore> =
        prefs.getHighScores(diffLabel)
            .mapNotNull { HighScore.deserialise(it) }
            .sortedBy { it.time }
            .take(5)

    fun saveHighScore(score: HighScore) {
        val trimmed = (prefs.getHighScores(score.difficulty) + score.serialise())
            .mapNotNull { HighScore.deserialise(it) }
            .sortedBy { it.time }
            .take(5)
            .map { it.serialise() }
            .toSet()
        prefs.saveHighScores(score.difficulty, trimmed)
    }

    fun clearHighScores(diffLabel: String) = prefs.clearHighScores(diffLabel)

    fun loadInitialSettings() = InitialSettings(
        isSoundEnabled = prefs.isSoundEnabled(),
        isVibrationEnabled = prefs.isVibrationEnabled(),
        isFullScreen = prefs.isFullScreen(),
        gameMode = prefs.getGameMode()
    )
}

data class InitialSettings(
    val isSoundEnabled: Boolean,
    val isVibrationEnabled: Boolean,
    val isFullScreen: Boolean,
    val gameMode: GameMode
)