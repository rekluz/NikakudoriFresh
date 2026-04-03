/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

data class Tile(val id: Int, val type: Int, val isRemoved: Boolean = false) {
    val imageName: String get() = when (type) {
        in 0..8 -> "tile_dot_${type + 1}"
        in 9..17 -> "tile_bamboo_${type - 9 + 1}"
        in 18..26 -> "tile_char_${type - 18 + 1}"
        27 -> "tile_wind_e"
        28 -> "tile_wind_s"
        29 -> "tile_wind_w"
        30 -> "tile_wind_n"
        31 -> "tile_drag_r"
        32 -> "tile_drag_g"
        33 -> "tile_drag_b"
        else -> "tile_back"
    }
}