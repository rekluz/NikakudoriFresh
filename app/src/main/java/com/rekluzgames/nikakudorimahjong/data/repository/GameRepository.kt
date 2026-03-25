/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.repository

import com.rekluzgames.nikakudorimahjong.data.preference.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val prefs: PreferenceManager
) {
    fun getSettings() = prefs
}