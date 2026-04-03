/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.rules

import org.junit.Test
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import org.junit.Assert.*

class HintFinderTest {

    @Test
    fun testFindAllMatchesReturnsValidPairs() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hints = HintFinder.findAllMatches(board)

        assertFalse(hints.isEmpty())
    }

    @Test
    fun testFoundHintsAreMatchingTiles() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hints = HintFinder.findAllMatches(board)

        if (hints.isNotEmpty()) {
            val (p1, p2) = hints.first()
            val tile1 = board[p1.first][p1.second]
            val tile2 = board[p2.first][p2.second]

            assertEquals(tile1.type, tile2.type)
        }
    }

    @Test
    fun testHintsAreNotRemovedTiles() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hints = HintFinder.findAllMatches(board)

        hints.forEach { (p1, p2) ->
            assertFalse(board[p1.first][p1.second].isRemoved)
            assertFalse(board[p2.first][p2.second].isRemoved)
        }
    }

    @Test
    fun testEmptyBoardReturnsNoHints() {
        val board = BoardGenerator.createBoard(Difficulty.EASY).map { row ->
            row.map { tile -> tile.copy(isRemoved = true) }
        }

        val hints = HintFinder.findAllMatches(board)
        assertTrue(hints.isEmpty())
    }

    @Test
    fun testHintPositionsAreValid() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hints = HintFinder.findAllMatches(board)

        hints.forEach { (p1, p2) ->
            assertTrue(p1.first >= 0 && p1.first < board.size)
            assertTrue(p1.second >= 0 && p1.second < board[0].size)
            assertTrue(p2.first >= 0 && p2.first < board.size)
            assertTrue(p2.second >= 0 && p2.second < board[0].size)
        }
    }

    @Test
    fun testHintsCanBeConnected() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hints = HintFinder.findAllMatches(board)

        hints.forEach { (p1, p2) ->
            assertTrue(PathFinder.canConnect(p1, p2, board))
        }
    }
}