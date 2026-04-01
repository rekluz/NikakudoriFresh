/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.rules

import com.rekluzgames.nikakudorimahjong.domain.model.LayeredLayout
import com.rekluzgames.nikakudorimahjong.domain.model.LayeredTile
import com.rekluzgames.nikakudorimahjong.domain.model.TilePosition
import kotlin.math.abs

object LayeredBoardGenerator {

    private const val MAX_ATTEMPTS = 20
    private const val TOTAL_TILE_TYPES = 34

    fun generate(layout: LayeredLayout): List<LayeredTile> {
        repeat(MAX_ATTEMPTS) {
            val result = tryGenerate(layout)
            if (result != null) return result
        }
        return fallback(layout)
    }

    private fun tryGenerate(layout: LayeredLayout): List<LayeredTile>? {
        val remaining = layout.positions.toMutableList()
        val typeAssignment = mutableMapOf<TilePosition, Int>()
        val typePool = generateTypePool(layout.tileCount)
        var poolIndex = 0

        while (remaining.size >= 2) {
            val free = remaining.filter { isFreeInRemaining(it, remaining) }
            if (free.size < 2) return null

            val shuffled = free.shuffled()
            val p1 = shuffled[0]
            val p2 = shuffled[1]
            val type = typePool[poolIndex++]

            typeAssignment[p1] = type
            typeAssignment[p2] = type
            remaining.remove(p1)
            remaining.remove(p2)
        }

        return layout.positions.mapIndexed { index, pos ->
            LayeredTile(
                id = index,
                type = typeAssignment[pos] ?: 0,
                col = pos.col,
                row = pos.row,
                layer = pos.layer
            )
        }
    }

    private fun isFreeInRemaining(pos: TilePosition, remaining: List<TilePosition>): Boolean {
        val blockedFromAbove = remaining.any { other ->
            other !== pos &&
                    other.layer == pos.layer + 1 &&
                    abs(other.col - pos.col) < 2 &&
                    abs(other.row - pos.row) < 2
        }
        if (blockedFromAbove) return false

        val blockedLeft = remaining.any { other ->
            other !== pos &&
                    other.layer == pos.layer &&
                    other.col == pos.col - 2 &&
                    abs(other.row - pos.row) < 2
        }

        val blockedRight = remaining.any { other ->
            other !== pos &&
                    other.layer == pos.layer &&
                    other.col == pos.col + 2 &&
                    abs(other.row - pos.row) < 2
        }

        return !blockedLeft || !blockedRight
    }

    private fun generateTypePool(totalTiles: Int): IntArray {
        val pool = IntArray(totalTiles)
        var index = 0
        val types = (0 until TOTAL_TILE_TYPES).shuffled().toMutableList()
        var typeIdx = 0

        fun nextType(): Int {
            if (typeIdx >= types.size) {
                types.shuffle()
                typeIdx = 0
            }
            return types[typeIdx++]
        }

        while (index + 4 <= totalTiles) {
            val type = nextType()
            repeat(4) { pool[index++] = type }
        }
        while (index + 2 <= totalTiles) {
            val type = nextType()
            repeat(2) { pool[index++] = type }
        }

        pool.shuffle()
        return pool
    }

    private fun fallback(layout: LayeredLayout): List<LayeredTile> {
        val typePool = generateTypePool(layout.tileCount)
        return layout.positions.mapIndexed { index, pos ->
            LayeredTile(
                id = index,
                type = typePool[index % typePool.size],
                col = pos.col,
                row = pos.row,
                layer = pos.layer
            )
        }
    }
}