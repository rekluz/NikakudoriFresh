/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.preference

import android.content.Context
import androidx.core.content.edit
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameMode

class PreferenceManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("NikakudoriPrefs", Context.MODE_PRIVATE)

    private object Keys {
        const val SOUND = "sound"
        const val VIBRATION = "vibration"
        const val MUSIC = "music"
        const val SCALE = "scale"
        const val FULL_SCREEN = "full_screen"
        const val LANGUAGE = "language"
        const val GAME_MODE = "game_mode"

        fun scoreKey(difficulty: String) = "scores_$difficulty"
    }

    fun isSoundEnabled() = prefs.getBoolean(Keys.SOUND, true)
    fun setSoundEnabled(v: Boolean) = prefs.edit { putBoolean(Keys.SOUND, v) }

    fun isVibrationEnabled() = prefs.getBoolean(Keys.VIBRATION, true)
    fun setVibrationEnabled(v: Boolean) = prefs.edit { putBoolean(Keys.VIBRATION, v) }

    fun isMusicEnabled() = prefs.getBoolean(Keys.MUSIC, true)
    fun setMusicEnabled(v: Boolean) = prefs.edit { putBoolean(Keys.MUSIC, v) }

    fun getScale() = prefs.getFloat(Keys.SCALE, 1.0f)
    fun setScale(v: Float) = prefs.edit { putFloat(Keys.SCALE, v) }

    fun isFullScreen() = prefs.getBoolean(Keys.FULL_SCREEN, false)
    fun setFullScreen(v: Boolean) = prefs.edit { putBoolean(Keys.FULL_SCREEN, v) }

    fun getLanguage() = prefs.getString(Keys.LANGUAGE, "") ?: ""
    fun setLanguage(lang: String) = prefs.edit { putString(Keys.LANGUAGE, lang) }

    fun getGameMode(): GameMode {
        val modeName = prefs.getString(Keys.GAME_MODE, GameMode.REGULAR.name)
        return runCatching {
            GameMode.valueOf(modeName ?: "")
        }.getOrElse {
            GameMode.REGULAR
        }
    }

    fun setGameMode(mode: GameMode) {
        prefs.edit { putString(Keys.GAME_MODE, mode.name) }
    }

    fun getHighScores(difficulty: String): Set<String> =
        prefs.getStringSet(Keys.scoreKey(difficulty), emptySet())
            ?.toSet() ?: emptySet() // IMPORTANT: return a copy

    fun saveHighScores(difficulty: String, scores: Set<String>) = prefs.edit { putStringSet(Keys.scoreKey(difficulty), scores) }

    fun clearHighScores(difficulty: String) = prefs.edit { remove(Keys.scoreKey(difficulty)) }

    fun getAllHighScores(): Map<String, Set<String>> =
        Difficulty.entries.associate { it.label to getHighScores(it.label) }
}