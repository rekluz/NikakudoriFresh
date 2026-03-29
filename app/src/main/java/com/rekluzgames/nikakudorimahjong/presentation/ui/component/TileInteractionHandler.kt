package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import com.rekluzgames.nikakudorimahjong.domain.engine.GameEngine
import com.rekluzgames.nikakudorimahjong.domain.model.*
import com.rekluzgames.nikakudorimahjong.domain.rules.PathFinder
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameUIState

class TileInteractionHandler(
    private val engine: GameEngine
) {

    data class Result(
        val newState: GameUIState,
        val playSound: String? = null,
        val triggerHaptic: (() -> Unit)? = null,
        val matchPath: List<Pair<Int, Int>>? = null,
        val matchedPair: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
        val matchedBoard: List<List<Tile>>? = null
    )

    fun handleClick(
        state: GameUIState,
        row: Int,
        col: Int
    ): Result {

        if (state.gameState != GameState.PLAYING || state.board[row][col].isRemoved) {
            return Result(state)
        }

        // First selection
        if (state.selectedTile == null) {
            return Result(
                newState = state.copy(
                    selectedTile = row to col,
                    allAvailableHints = emptyList(),
                    currentHintIndex = -1
                ),
                playSound = "tile_click"
            )
        }

        val p1 = state.selectedTile
        val p2 = row to col

        if (p1 == p2) {
            return Result(state.copy(selectedTile = null))
        }

        val path = PathFinder.getPath(p1, p2, state.board)
        val matchedBoard = if (path != null) engine.attemptMatch(p1, p2, state.board) else null

        return if (matchedBoard != null) {
            Result(
                newState = state.copy(
                    board = matchedBoard,
                    selectedTile = null,
                    allAvailableHints = emptyList(),
                    currentHintIndex = -1,
                    undoHistory = (state.undoHistory + listOf(state.board)).takeLast(20)
                ),
                playSound = "tile_match",
                matchPath = path,
                matchedPair = p1 to p2,
                matchedBoard = matchedBoard
            )
        } else {
            Result(
                newState = state.copy(
                    selectedTile = p2,
                    allAvailableHints = emptyList(),
                    currentHintIndex = -1
                ),
                playSound = "tile_error"
            )
        }
    }
}