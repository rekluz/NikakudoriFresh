package com.rekluzgames.nikakudorimahjong.presentation.score

import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.HighScore
import com.rekluzgames.nikakudorimahjong.domain.model.Medal
import javax.inject.Inject

class ScoreManager @Inject constructor(
    private val repository: GameRepository
) {
    fun getAllHighScores(): Map<String, List<HighScore>> =
        repository.getAllHighScores()

    fun clearScores(diffLabel: String) {
        repository.clearHighScores(diffLabel)
    }

    fun processWin(
        playerName: String,
        timeSeconds: Int,
        difficulty: Difficulty,
        usedHint: Boolean,
        usedShuffle: Boolean
    ): HighScore {
        val medals = mutableListOf<Medal>()
        if (!usedHint) medals.add(Medal.SNIPER)
        if (!usedShuffle) medals.add(Medal.STRATEGIST)
        if (timeSeconds < 120) medals.add(Medal.FLASH)

        val finalName = playerName.ifBlank { "???" }
        val newScore = HighScore(
            name = finalName,
            time = timeSeconds,
            difficulty = difficulty.label,
            medals = medals
        )

        repository.saveHighScore(newScore)
        return newScore
    }
}
