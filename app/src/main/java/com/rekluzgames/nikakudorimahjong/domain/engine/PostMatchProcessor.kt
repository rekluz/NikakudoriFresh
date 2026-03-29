package com.rekluzgames.nikakudorimahjong.domain.engine

import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.model.GameMode
import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel.Companion.BOARD_GRAVITY_ANIMATION_DELAY_MS
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel.Companion.HANDLE_WIN_ANIMATION_DELAY_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PostMatchProcessor @Inject constructor(
    private val engine: GameEngine,
    private val repository: GameRepository,
    private val hapticManager: HapticManager
) {

    suspend fun process(
        board: List<List<Tile>>,
        getBoard: () -> List<List<Tile>>,
        updateBoard: (List<List<Tile>>) -> Unit,
        onStalemate: () -> Unit,
        onWin: () -> Unit
    ) {
        val mode = repository.getGameMode()

        val processedBoard = if (mode == GameMode.GRAVITY) {
            delay(BOARD_GRAVITY_ANIMATION_DELAY_MS)
            val gravityBoard = engine.applyGravity(board)
            if (gravityBoard != board) updateBoard(gravityBoard)
            gravityBoard
        } else {
            board
        }

        withContext(Dispatchers.Default) {
            if (!engine.isGameOver(processedBoard) && HintFinder.findAllMatches(processedBoard).isEmpty()) {
                withContext(Dispatchers.Main) {
                    onStalemate()
                    hapticManager.noMoves()
                }
            }
        }

        if (engine.isGameOver(getBoard())) {
            delay(HANDLE_WIN_ANIMATION_DELAY_MS)
            onWin()
        }
    }
}