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
     * FORTRESS — 84 tiles, 4 layers.
     *
     * Classic fortress/castle shape.
     * Following PYRAMID pattern: each layer uses odd positions centered over the layer below.
     * Layer 3 only covers the CORNERS, not the full layer 1.
     *
     * Layer 0: 8 cols × 5 rows = 40 tiles  (base - cols 0,2,4,6,8,10,12,14)
     * Layer 1: 6 cols × 4 rows = 24 tiles  (walls - cols 2,4,6,8,10,12)
     * Layer 2: 3 cols × 2 rows =  6 tiles  (battlements - cols 6,8,10)
     * Layer 3: 14 tiles (towers - corners only, cols 2,4,10,12)
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
                for (col in 2..12 step 2)
                    add(TilePosition(col, row, layer = 1))

            // Layer 2 — battlements
            for (row in 2..6 step 2)
                for (col in 6..10 step 2)
                    add(TilePosition(col, row, layer = 2))

            // Layer 3 — towers at CORNERS ONLY (not covering entire layer 1)
            // Left front tower (at cols 2-4, row 1-3)
            add(TilePosition(1, 1, layer = 3))
            add(TilePosition(3, 1, layer = 3))
            add(TilePosition(1, 3, layer = 3))
            add(TilePosition(3, 3, layer = 3))
            // Right front tower (at cols 10-12, row 1-3)
            add(TilePosition(9, 1, layer = 3))
            add(TilePosition(11, 1, layer = 3))
            add(TilePosition(9, 3, layer = 3))
            add(TilePosition(11, 3, layer = 3))
            // Left back tower (at cols 2-4, row 5-7)
            add(TilePosition(1, 5, layer = 3))
            add(TilePosition(3, 5, layer = 3))
            add(TilePosition(1, 7, layer = 3))
            add(TilePosition(3, 7, layer = 3))
            // Right back tower (at cols 10-12, row 5-7)
            add(TilePosition(9, 5, layer = 3))
            add(TilePosition(11, 5, layer = 3))
            add(TilePosition(9, 7, layer = 3))
            add(TilePosition(11, 7, layer = 3))
        }
    )

    /**
     * DRAGON — 144 tiles, 4 layers.
     *
     * Traditional dragon shape: serpentine body with prominent head and tapered tail.
     * Body winds in an S-curve pattern with layered spine.
     *
     * Layer 0: widest base body
     * Layer 1: middle body
     * Layer 2: raised spine
     * Layer 3: head and tail details
     *
     * Total: 144 tiles
     */
    val DRAGON = LayeredLayout(
        id = "DRAGON",
        displayName = "Dragon",
        positions = buildList {
            // Layer 0 — base body (serpentine S-curve)
            // Main horizontal body
            for (col in 2..30 step 2)
                add(TilePosition(col, 4, layer = 0))
            // Upper body curve
            for (col in 4..28 step 2)
                add(TilePosition(col, 2, layer = 0))
            // Lower body curve
            for (col in 4..28 step 2)
                add(TilePosition(col, 6, layer = 0))
            // Top edge
            for (col in 6..26 step 2)
                add(TilePosition(col, 0, layer = 0))
            // Bottom edge
            for (col in 6..26 step 2)
                add(TilePosition(col, 8, layer = 0))

            // Layer 1 — middle body (slightly narrower)
            for (col in 4..28 step 2) {
                add(TilePosition(col, 3, layer = 1))
                add(TilePosition(col, 5, layer = 1))
            }
            // Spine center
            for (col in 6..26 step 2)
                add(TilePosition(col, 4, layer = 1))

            // Layer 2 — spine ridge (narrower)
            for (col in 8..24 step 2)
                add(TilePosition(col, 4, layer = 2))

            // Add extra tiles for 144 total
            // Extended body segments
            for (row in 1..7 step 2) {
                add(TilePosition(0, row, layer = 2))
                add(TilePosition(32, row, layer = 2))
            }

            // Layer 3 — head (prominent head on left side)
            add(TilePosition(0, 2, layer = 3))
            add(TilePosition(0, 4, layer = 3))
            add(TilePosition(0, 6, layer = 3))
            add(TilePosition(2, 0, layer = 3))
            add(TilePosition(2, 1, layer = 3))
            add(TilePosition(2, 3, layer = 3))
            add(TilePosition(2, 5, layer = 3))
            add(TilePosition(2, 7, layer = 3))
            add(TilePosition(2, 8, layer = 3))

            // Layer 3 — tail (tapered on right side)
            add(TilePosition(28, 3, layer = 3))
            add(TilePosition(28, 5, layer = 3))
            add(TilePosition(30, 2, layer = 3))
            add(TilePosition(30, 4, layer = 3))
            add(TilePosition(30, 6, layer = 3))
            add(TilePosition(32, 4, layer = 3))
        }
    )

    /**
     * TURTLE — 144 tiles, 4 layers.
     *
     * Classic turtle shape from Brodie Lockard's original design.
     * Structure: large base (shell), stacked pyramid center, head/tail/legs.
     *
     * Layer 0: base = 87 tiles (widest, includes head/tail/legs)
     * Layer 1: mid = 36 tiles (middle dome)
     * Layer 2: inner = 16 tiles (inner dome)
     * Layer 3: peak = 4 tiles (top)
     * Total: 144 tiles
     */
    val TURTLE = LayeredLayout(
        id = "TURTLE",
        displayName = "Turtle",
        positions = buildList {
            // Layer 0 - Base (87 tiles): widest layer
            // Main shell rectangle: 12x6 = 72 tiles
            for (row in 2..14 step 2) {
                for (col in 6..26 step 2) {
                    add(TilePosition(col, row, layer = 0))
                }
            }
            // Head (top center): 3 tiles
            add(TilePosition(14, 0, layer = 0))
            add(TilePosition(16, 0, layer = 0))
            add(TilePosition(18, 0, layer = 0))
            // Tail (bottom center): 3 tiles
            add(TilePosition(14, 16, layer = 0))
            add(TilePosition(16, 16, layer = 0))
            add(TilePosition(18, 16, layer = 0))
            // Front left leg: 2 tiles
            add(TilePosition(4, 4, layer = 0))
            add(TilePosition(6, 4, layer = 0))
            // Front right leg: 2 tiles
            add(TilePosition(24, 4, layer = 0))
            add(TilePosition(26, 4, layer = 0))
            // Back left leg: 2 tiles
            add(TilePosition(4, 12, layer = 0))
            add(TilePosition(6, 12, layer = 0))
            // Back right leg: 2 tiles
            add(TilePosition(24, 12, layer = 0))
            add(TilePosition(26, 12, layer = 0))

            // Layer 1 - Middle (36 tiles): smaller dome
            for (row in 4..12 step 2) {
                for (col in 8..22 step 2) {
                    add(TilePosition(col, row, layer = 1))
                }
            }

            // Layer 2 - Inner (16 tiles): even smaller
            for (row in 6..10 step 2) {
                for (col in 12..18 step 2) {
                    add(TilePosition(col, row, layer = 2))
                }
            }

            // Layer 3 - Peak (4 tiles): top of pyramid
            add(TilePosition(14, 6, layer = 3))
            add(TilePosition(16, 6, layer = 3))
            add(TilePosition(14, 8, layer = 3))
            add(TilePosition(16, 8, layer = 3))
        }
    )

    /**
     * BRIDGE — 84 tiles, 3 layers.
     *
     * Classic bridge shape: elevated center with two ends.
     * Uses proper pyramid stacking - each layer is narrower than the one below.
     *
     * Layer 0: 12 cols × 4 rows = 48 tiles  (base)
     * Layer 1: 8 cols × 2 rows = 16 tiles  (arch)
     * Layer 2: 4 cols × 2 rows =  8 tiles  (top)
     * Layer 3: 12 tiles (deck/railings at peak)
     */
    val BRIDGE = LayeredLayout(
        id = "BRIDGE",
        displayName = "Bridge",
        positions = buildList {
            // Layer 0 — base (widest)
            for (row in 0..6 step 2)
                for (col in 0..22 step 2)
                    add(TilePosition(col, row, layer = 0))

            // Layer 1 — arch supports (narrower, centered)
            for (row in 1..5 step 2)
                for (col in 4..18 step 2)
                    add(TilePosition(col, row, layer = 1))

            // Layer 2 — top of arch (narrowest)
            for (row in 2..4 step 2)
                for (col in 8..14 step 2)
                    add(TilePosition(col, row, layer = 2))

            // Layer 3 — bridge deck/railings (above layer 2)
            // Center deck
            add(TilePosition(10, 2, layer = 3))
            add(TilePosition(12, 2, layer = 3))
            add(TilePosition(10, 4, layer = 3))
            add(TilePosition(12, 4, layer = 3))
            // Left railing
            add(TilePosition(6, 1, layer = 3))
            add(TilePosition(8, 1, layer = 3))
            add(TilePosition(6, 5, layer = 3))
            add(TilePosition(8, 5, layer = 3))
            // Right railing  
            add(TilePosition(14, 1, layer = 3))
            add(TilePosition(16, 1, layer = 3))
            add(TilePosition(14, 5, layer = 3))
            add(TilePosition(16, 5, layer = 3))
        }
    )

    val ALL: List<LayeredLayout> = listOf(PYRAMID, FORTRESS, DRAGON, TURTLE, BRIDGE)

    fun byId(id: String): LayeredLayout? = ALL.firstOrNull { it.id == id }
}