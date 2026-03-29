/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.preference

import android.content.Context
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameMode

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("NikakudoriPrefs", Context.MODE_PRIVATE)

    fun isSoundEnabled() = prefs.getBoolean("sound", true)
    fun setSoundEnabled(v: Boolean) = prefs.edit().putBoolean("sound", v).apply()

    fun isVibrationEnabled() = prefs.getBoolean("vibration", true)
    fun setVibrationEnabled(v: Boolean) = prefs.edit().putBoolean("vibration", v).apply()

    fun isMusicEnabled() = prefs.getBoolean("music", true)
    fun setMusicEnabled(v: Boolean) = prefs.edit().putBoolean("music", v).apply()

    fun getScale() = prefs.getFloat("scale", 1.0f)
    fun setScale(v: Float) = prefs.edit().putFloat("scale", v).apply()

    fun isFullScreen() = prefs.getBoolean("full_screen", false)
    fun setFullScreen(v: Boolean) = prefs.edit().putBoolean("full_screen", v).apply()

    fun getLanguage() = prefs.getString("language", "") ?: ""
    fun setLanguage(lang: String) = prefs.edit().putString("language", lang).apply()

    fun getGameMode(): GameMode {
        val modeName = prefs.getString("game_mode", GameMode.REGULAR.name)
        return GameMode.valueOf(modeName ?: GameMode.REGULAR.name)
    }

    fun setGameMode(mode: GameMode) {
        prefs.edit().putString("game_mode", mode.name).apply()
    }

    private fun scoreKey(difficulty: String) = "scores_$difficulty"

    fun getHighScores(difficulty: String): Set<String> =
        prefs.getStringSet(scoreKey(difficulty), emptySet()) ?: emptySet()

    fun saveHighScores(difficulty: String, scores: Set<String>) =
        prefs.edit().putStringSet(scoreKey(difficulty), scores).apply()

    fun clearHighScores(difficulty: String) =
        prefs.edit().remove(scoreKey(difficulty)).apply()

    fun getAllHighScores(): Map<String, Set<String>> =
        Difficulty.entries.associate { it.label to getHighScores(it.label) }
}