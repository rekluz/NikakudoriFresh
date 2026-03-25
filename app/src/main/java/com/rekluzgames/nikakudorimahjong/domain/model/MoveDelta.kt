/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

/**
 * A Data Class that stores exactly what happened during a match.
 * Used to restore tiles during an UNDO without storing the entire board in memory.
 */
data class MoveDelta(
    val p1: Pair<Int, Int>,
    val p2: Pair<Int, Int>,
    val tile1: Tile,
    val tile2: Tile
)