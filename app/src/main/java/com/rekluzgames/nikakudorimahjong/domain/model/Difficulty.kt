/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

import androidx.annotation.StringRes
import com.rekluzgames.nikakudorimahjong.R

/**
 * Difficulty Enum
 * Standardized to 5 shuffles for all game modes.
 * Adjusted EASY to 6x12 (72 tiles) to support "Rule of 4" generation.
 */
enum class Difficulty(
    val rows: Int,
    val cols: Int,
    val shuffles: Int,
    val label: String,
    @param:StringRes val titleRes: Int
) {
    EASY(6, 12, 5, "EASY", R.string.diff_easy),
    NORMAL(7, 16, 5, "NORMAL", R.string.diff_normal),
    HARD(8, 17, 5, "HARD", R.string.diff_hard),
    EXTREME(8, 22, 5, "EXTREME", R.string.diff_extreme)
}