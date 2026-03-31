/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

import com.rekluzgames.nikakudorimahjong.presentation.quote.QuoteProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteManager @Inject constructor() {
    // Separate pool per language so switching languages mid-session works correctly
    private val pools = mutableMapOf<String, MutableList<String>>()

    fun next(language: String): String {
        val pool = pools.getOrPut(language) {
            QuoteProvider.getShuffledQuotes(language)
        }
        if (pool.isEmpty()) {
            pools[language] = QuoteProvider.getShuffledQuotes(language)
            return pools[language]!!.removeAt(0)
        }
        return pool.removeAt(0)
    }
}
