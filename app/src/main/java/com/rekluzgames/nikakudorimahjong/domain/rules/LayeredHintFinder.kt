/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.rules

import com.rekluzgames.nikakudorimahjong.domain.engine.LayeredGameEngine
import com.rekluzgames.nikakudorimahjong.domain.model.LayeredTile

object LayeredHintFinder {

    /**
     * Returns all valid matchable pairs from the current layered board.
     * A pair is valid when both tiles are free (per LayeredGameEngine.isFree)
     * and share the same type.
     *
     * Result is a list of (tile id 1, tile id 2) pairs, consistent with
     * how flat HintFinder returns (row,col) pairs — consumers just use
     * the ids to look up tiles in the layered board list.
     */
    fun findAllMatches(
        tiles: List<LayeredTile>,
        engine: LayeredGameEngine
    ): List<Pair<Int, Int>> {
        val free = tiles.filter { !it.isRemoved && engine.isFree(it, tiles) }

        val grouped = free.groupBy { it.type }

        val matches = mutableListOf<Pair<Int, Int>>()

        for ((_, group) in grouped) {
            if (group.size < 2) continue
            for (i in 0 until group.size) {
                for (j in i + 1 until group.size) {
                    matches.add(group[i].id to group[j].id)
                }
            }
        }

        return matches
    }

    /**
     * Returns a single hint pair — the first available match.
     * Returns null if no matches exist (stalemate).
     */
    fun findHint(
        tiles: List<LayeredTile>,
        engine: LayeredGameEngine
    ): Pair<Int, Int>? = findAllMatches(tiles, engine).firstOrNull()
}