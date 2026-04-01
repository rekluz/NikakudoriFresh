/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

data class LayeredLayout(
    val id: String,
    val displayName: String,
    val positions: List<TilePosition>
) {
    val tileCount: Int get() = positions.size
}

/**
 * Coordinate system (half-tile units):
 *   - Same-layer tiles spaced 2 apart (col/row increments of 2).
 *   - A tile at (col, row, L) is blocked by any tile at layer L+1
 *     where |Δcol| < 2 AND |Δrow| < 2.
 *   - Upper layer tiles at odd positions (col+1, row+1) sit centred
 *     over the four tiles below them.
 *
 * Free tile rules:
 *   - Not covered from above (no tile on layer+1 within 1 unit).
 *   - At least one horizontal side open (no same-layer neighbour
 *     at col±2 within 1 row unit).
 *
 * Tile counts: all divisible by 4.
 */
object LayeredLayouts {

    /**
     * PYRAMID — 72 tiles, 3 layers.
     *
     * Layer 0: 8 cols × 5 rows = 40 tiles  (base)
     * Layer 1: 6 cols × 4 rows = 24 tiles  (middle)
     * Layer 2: 4 cols × 2 rows =  8 tiles  (peak)
     *
     * Free tiles at game start (≈17):
     *   - All layer-2 edge tiles (col 3 and col 9)
     *   - All layer-1 edge tiles (col 1 and col 11)
     *   - All layer-0 right-edge tiles (col 14, unreachable by layer 1)
     */
    val PYRAMID = LayeredLayout(
        id = "PYRAMID",
        displayName = "Pyramid",
        positions = buildList {
            // Layer 0 — base
            for (row in 0..8 step 2)
                for (col in 0..14 step 2)
                    add(TilePosition(col, row, layer = 0))
            // Layer 1 — middle
            for (row in 1..7 step 2)
                for (col in 1..11 step 2)
                    add(TilePosition(col, row, layer = 1))
            // Layer 2 — peak
            for (row in 3..5 step 2)
                for (col in 3..9 step 2)
                    add(TilePosition(col, row, layer = 2))
        }
    )

    /**
     * FORTRESS — 76 tiles, 4 layers.
     *
     * Same base and walls as Pyramid, with an extra tower tier on top.
     *
     * Layer 0: 8 × 5 = 40
     * Layer 1: 6 × 4 = 24
     * Layer 2: 4 × 2 =  8
     * Layer 3: 2 × 2 =  4  (tower top)
     */
    val FORTRESS = LayeredLayout(
        id = "FORTRESS",
        displayName = "Fortress",
        positions = buildList {
            // Layer 0 — base
            for (row in 0..8 step 2)
                for (col in 0..14 step 2)
                    add(TilePosition(col, row, layer = 0))
            // Layer 1 — walls
            for (row in 1..7 step 2)
                for (col in 1..11 step 2)
                    add(TilePosition(col, row, layer = 1))
            // Layer 2 — battlements
            for (row in 3..5 step 2)
                for (col in 3..9 step 2)
                    add(TilePosition(col, row, layer = 2))
            // Layer 3 — tower top
            for (row in 3..5 step 2)
                for (col in 5..7 step 2)
                    add(TilePosition(col, row, layer = 3))
        }
    )

    /**
     * DRAGON — 68 tiles, 3 layers.
     *
     * Wide rectangular body with a raised spine along the centre.
     *
     * Layer 0: 9 cols × 4 rows = 36  (body)
     * Layer 1: 8 cols × 3 rows = 24  (scales)
     * Layer 2: 4 cols × 2 rows =  8  (spine ridge)
     *
     * Free tiles at game start (≈10):
     *   - All layer-2 edge tiles (col 5 and col 11)
     *   - All layer-1 edge tiles (col 1 and col 15)
     */
    val DRAGON = LayeredLayout(
        id = "DRAGON",
        displayName = "Dragon",
        positions = buildList {
            // Layer 0 — body
            for (row in 0..6 step 2)
                for (col in 0..16 step 2)
                    add(TilePosition(col, row, layer = 0))
            // Layer 1 — scales
            for (row in 1..5 step 2)
                for (col in 1..15 step 2)
                    add(TilePosition(col, row, layer = 1))
            // Layer 2 — spine ridge
            for (row in 2..4 step 2)
                for (col in 5..11 step 2)
                    add(TilePosition(col, row, layer = 2))
        }
    )

    val ALL: List<LayeredLayout> = listOf(PYRAMID, FORTRESS, DRAGON)

    fun byId(id: String): LayeredLayout? = ALL.firstOrNull { it.id == id }
}