package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameUIState  // ← adjust if GameUIState lives elsewhere

class AboutInteractionHandler(
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager
) {

    fun nextStage(state: GameUIState): GameUIState =
        state.copy(aboutStage = state.aboutStage + 1)

    fun previousStage(state: GameUIState): GameUIState =
        state.copy(aboutStage = (state.aboutStage - 1).coerceAtLeast(0))

    fun close(state: GameUIState): GameUIState =
        state.copy(aboutStage = 0, clearedAboutTiles = emptySet())

    fun onTileClick(state: GameUIState, index: Int, threshold: Int = 11): GameUIState {
        soundManager.play("tile_match")
        hapticManager.tileMatch()
        val updated = state.clearedAboutTiles + index
        return if (updated.size >= threshold) {
            soundManager.play("secret_unlocked")
            state.copy(clearedAboutTiles = updated, aboutStage = state.aboutStage + 1)
        } else {
            state.copy(clearedAboutTiles = updated)
        }
    }
}