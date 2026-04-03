/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.rules

import org.junit.Test
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import org.junit.Assert.*

class BoardGeneratorTest {

    @Test
    fun testBoardGenerationCreatesValidBoard() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)

        assertNotNull(board)
        assertFalse(board.isEmpty())
        assertEquals(Difficulty.NORMAL.rows, board.size)
        assertEquals(Difficulty.NORMAL.cols, board[0].size)
    }

    @Test
    fun testBoardHasEvenNumberOfTiles() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val tileCount = board.flatten().count { !it.isRemoved }

        assertEquals(0, tileCount % 2)
    }

    @Test
    fun testDifficultiesDifferInSize() {
        val easyBoard = BoardGenerator.createBoard(Difficulty.EASY)
        val normalBoard = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hardBoard = BoardGenerator.createBoard(Difficulty.HARD)

        val easyCount = easyBoard.flatten().count { !it.isRemoved }
        val normalCount = normalBoard.flatten().count { !it.isRemoved }
        val hardCount = hardBoard.flatten().count { !it.isRemoved }

        assertTrue(easyCount < normalCount)
        assertTrue(normalCount < hardCount)
    }

    @Test
    fun testAllTilesHaveValidTypes() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)

        board.flatten().forEach { tile ->
            assertTrue(tile.type >= 0 && tile.type < 34)
        }
    }

    @Test
    fun testAllTilesHaveUniqueIds() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val ids = board.flatten().map { it.id }

        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun testBoardCanBeSolved() {
        val board = BoardGenerator.createBoard(Difficulty.NORMAL)
        val hints = HintFinder.findAllMatches(board)

        assertTrue(hints.isNotEmpty())
    }
}