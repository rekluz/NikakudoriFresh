/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.engine

import com.rekluzgames.nikakudorimahjong.domain.model.LayeredTile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LayeredGameEngine @Inject constructor() {

    // -------------------------------------------------------------------------
    // Freedom check
    // -------------------------------------------------------------------------

    /**
     * A tile is free when:
     *   1. No unremoved tile on layer+1 overlaps it (|Δcol| < 2 AND |Δrow| < 2).
     *   2. At least one horizontal side is open — no unremoved tile at the same
     *      layer immediately to the left (Δcol == -2) or right (Δcol == +2)
     *      within the overlapping row range (|Δrow| < 2).
     */
    fun isFree(tile: LayeredTile, all: List<LayeredTile>): Boolean {
        if (tile.isRemoved) return false

        val blockedFromAbove = all.any { other ->
            !other.isRemoved &&
                    other.layer == tile.layer + 1 &&
                    abs(other.col - tile.col) < 2 &&
                    abs(other.row - tile.row) < 2
        }
        if (blockedFromAbove) return false

        val blockedLeft = all.any { other ->
            !other.isRemoved &&
                    other.layer == tile.layer &&
                    other.col == tile.col - 2 &&
                    abs(other.row - tile.row) < 2
        }

        val blockedRight = all.any { other ->
            !other.isRemoved &&
                    other.layer == tile.layer &&
                    other.col == tile.col + 2 &&
                    abs(other.row - tile.row) < 2
        }

        return !blockedLeft || !blockedRight
    }

    // -------------------------------------------------------------------------
    // Matching
    // -------------------------------------------------------------------------

    /**
     * Attempts to match two tiles. Returns the updated tile list on success,
     * or null if the match is invalid (types differ or either tile is not free).
     */
    fun attemptMatch(
        id1: Int,
        id2: Int,
        all: List<LayeredTile>
    ): List<LayeredTile>? {
        val t1 = all.firstOrNull { it.id == id1 } ?: return null
        val t2 = all.firstOrNull { it.id == id2 } ?: return null

        if (t1.type != t2.type) return null
        if (!isFree(t1, all) || !isFree(t2, all)) return null

        return all.map { tile ->
            when (tile.id) {
                id1, id2 -> tile.copy(isRemoved = true)
                else -> tile
            }
        }
    }

    // -------------------------------------------------------------------------
    // Win condition
    // -------------------------------------------------------------------------

    fun isGameOver(all: List<LayeredTile>): Boolean = all.all { it.isRemoved }

    // -------------------------------------------------------------------------
    // Stalemate check
    // -------------------------------------------------------------------------

    /**
     * Returns true if no valid match exists among currently free tiles.
     * Used by PostMatchProcessor equivalent after each match.
     */
    fun isStalemate(all: List<LayeredTile>): Boolean {
        val freeTiles = all.filter { !it.isRemoved && isFree(it, all) }
        val grouped = freeTiles.groupBy { it.type }
        return grouped.none { (_, tiles) -> tiles.size >= 2 }
    }
}