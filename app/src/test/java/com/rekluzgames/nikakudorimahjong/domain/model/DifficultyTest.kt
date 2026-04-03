/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

import org.junit.Test
import org.junit.Assert.*

class DifficultyTest {

    @Test
    fun testDifficultyEnumHasAllValues() {
        val difficulties = listOf(
            Difficulty.EASY,
            Difficulty.NORMAL,
            Difficulty.HARD,
            Difficulty.EXTREME
        )

        assertEquals(4, difficulties.size)
    }

    @Test
    fun testDifficultyHasLabel() {
        assertTrue(Difficulty.EASY.label.isNotEmpty())
        assertTrue(Difficulty.NORMAL.label.isNotEmpty())
        assertTrue(Difficulty.HARD.label.isNotEmpty())
        assertTrue(Difficulty.EXTREME.label.isNotEmpty())
    }

    @Test
    fun testDifficultyLabelsAreUnique() {
        val labels = listOf(
            Difficulty.EASY.label,
            Difficulty.NORMAL.label,
            Difficulty.HARD.label,
            Difficulty.EXTREME.label
        )

        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun testAllDifficultiesHaveFiveShuffles() {
        assertEquals(5, Difficulty.EASY.shuffles)
        assertEquals(5, Difficulty.NORMAL.shuffles)
        assertEquals(5, Difficulty.HARD.shuffles)
        assertEquals(5, Difficulty.EXTREME.shuffles)
    }

    @Test
    fun testDifficultiesDifferInDimensions() {
        val easyTiles = Difficulty.EASY.rows * Difficulty.EASY.cols
        val normalTiles = Difficulty.NORMAL.rows * Difficulty.NORMAL.cols
        val hardTiles = Difficulty.HARD.rows * Difficulty.HARD.cols
        val extremeTiles = Difficulty.EXTREME.rows * Difficulty.EXTREME.cols

        assertTrue(easyTiles < normalTiles)
        assertTrue(normalTiles < hardTiles)
        assertTrue(hardTiles < extremeTiles)
    }

    @Test
    fun testEasyBoardIs6x12() {
        assertEquals(6, Difficulty.EASY.rows)
        assertEquals(12, Difficulty.EASY.cols)
    }

    @Test
    fun testNormalBoardIs7x16() {
        assertEquals(7, Difficulty.NORMAL.rows)
        assertEquals(16, Difficulty.NORMAL.cols)
    }

    @Test
    fun testHardBoardIs8x17() {
        assertEquals(8, Difficulty.HARD.rows)
        assertEquals(17, Difficulty.HARD.cols)
    }

    @Test
    fun testExtremeBoardIs8x22() {
        assertEquals(8, Difficulty.EXTREME.rows)
        assertEquals(22, Difficulty.EXTREME.cols)
    }
}