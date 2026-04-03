/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.engine

import org.junit.Test
import com.rekluzgames.nikakudorimahjong.domain.rules.BoardGenerator
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import org.junit.Assert.*

class GameEngineTest {

    private val engine = GameEngine()

    @Test
    fun testGameOverDetectionWhenBoardEmpty() {
        val board = BoardGenerator.createBoard(Difficulty.EASY).map { row ->
            row.map { tile -> tile.copy(isRemoved = true) }
        }

        val isOver = engine.isGameOver(board)
        assertTrue(isOver)
    }

    @Test
    fun testGameNotOverWithActiveTiles() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)

        val isOver = engine.isGameOver(board)
        assertFalse(isOver)
    }

    @Test
    fun testAttemptMatchRemovesTiles() {
        val board = BoardGenerator.createBoard(Difficulty.EASY)
        val hints = HintFinder.findAllMatches(board)

        if (hints.isNotEmpty()) {
            val (p1, p2) = hints.first()
            val resultBoard = engine.attemptMatch(p1, p2, board)

            assertNotNull(resultBoard)
            assertTrue(resultBoard!![p1.first][p1.second].isRemoved)
            assertTrue(resultBoard[p2.first][p2.second].isRemoved)
        }
    }

    @Test
    fun testAttemptMatchReturnsNullForMismatch() {
        val board = BoardGenerator.createBoard(Difficulty.EASY)
        val activeTiles = board.flatten().filter { !it.isRemoved }

        if (activeTiles.size >= 2) {
            var p1: Pair<Int, Int>? = null
            var p2: Pair<Int, Int>? = null

            for (r in board.indices) {
                for (c in board[r].indices) {
                    if (board[r][c] == activeTiles[0]) p1 = r to c
                    if (board[r][c] == activeTiles[1]) p2 = r to c
                }
            }

            if (p1 != null && p2 != null &&
                board[p1.first][p1.second].type != board[p2.first][p2.second].type) {
                val result = engine.attemptMatch(p1, p2, board)
                assertNull(result)
            }
        }
    }

    @Test
    fun testGravityIsApplied() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val boardAfterGravity = engine.applyGravity(board)

        assertNotNull(boardAfterGravity)
        assertTrue(boardAfterGravity.isNotEmpty())
        assertEquals(board.size, boardAfterGravity.size)
    }

    @Test
    fun testBoardStatePreservedForUnremovedTiles() {
        val board = BoardGenerator.createBoard(Difficulty.EASY)
        val hints = HintFinder.findAllMatches(board)

        if (hints.isNotEmpty()) {
            val (p1, p2) = hints.first()
            val resultBoard = engine.attemptMatch(p1, p2, board)

            assertNotNull(resultBoard)
            for (r in board.indices) {
                for (c in board[r].indices) {
                    if (r to c != p1 && r to c != p2) {
                        assertEquals(board[r][c].type, resultBoard!![r][c].type)
                        assertEquals(board[r][c].isRemoved, resultBoard[r][c].isRemoved)
                    }
                }
            }
        }
    }
}