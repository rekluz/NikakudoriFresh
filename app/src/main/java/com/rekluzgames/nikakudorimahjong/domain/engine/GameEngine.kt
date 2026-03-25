/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.domain.engine

import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import com.rekluzgames.nikakudorimahjong.domain.rules.PathFinder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameEngine @Inject constructor() {
    fun attemptMatch(p1: Pair<Int, Int>, p2: Pair<Int, Int>, board: List<List<Tile>>): List<List<Tile>>? {
        if (PathFinder.canConnect(p1, p2, board)) {
            val newBoard = board.toMutableList()
            val rowsToUpdate = setOf(p1.first, p2.first)
            for (rowIndex in rowsToUpdate) {
                newBoard[rowIndex] = newBoard[rowIndex].toMutableList().apply {
                    if (p1.first == rowIndex) this[p1.second] = this[p1.second].copy(isRemoved = true)
                    if (p2.first == rowIndex) this[p2.second] = this[p2.second].copy(isRemoved = true)
                }
            }
            return newBoard
        }
        return null
    }

    fun applyGravity(board: List<List<Tile>>): List<List<Tile>> {
        if (board.isEmpty()) return board
        val rows = board.size; val cols = board[0].size
        val shifted = (0 until cols).map { c ->
            val col = (0 until rows).map { r -> board[r][c] }
            col.filter { it.isRemoved } + col.filter { !it.isRemoved }
        }
        return List(rows) { r -> List(cols) { c -> shifted[c][r] } }
    }

    fun isGameOver(board: List<List<Tile>>): Boolean = board.flatten().all { it.isRemoved }
}