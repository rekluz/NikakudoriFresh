/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

import com.rekluzgames.nikakudorimahjong.presentation.ui.component.QuoteProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteManager @Inject constructor() {
    private var pool = QuoteProvider.getShuffledQuotes().toMutableList()

    fun next(): String {
        if (pool.isEmpty()) {
            pool = QuoteProvider.getShuffledQuotes().toMutableList()
        }
        return if (pool.isNotEmpty()) pool.removeAt(0) else "Matching Master!"
    }
}