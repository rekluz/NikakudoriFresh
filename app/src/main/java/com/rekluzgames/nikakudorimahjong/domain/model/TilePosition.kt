/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

/**
 * Describes a single slot in a layered layout definition.
 *
 * col and row are in half-tile units (so adjacent tiles are spaced 2 apart),
 * which makes overlap detection simple integer arithmetic throughout the engine.
 *
 * Example: a tile at col=2, row=0 overlaps a tile at col=1, row=1 (layer above)
 * because |Δcol|=1 < 2 AND |Δrow|=1 < 2.
 */
data class TilePosition(
    val col: Int,
    val row: Int,
    val layer: Int
)