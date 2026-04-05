/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.audio

import android.content.Context
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import android.annotation.SuppressLint

@SuppressLint("DiscouragedApi")
@Singleton
class SoundManager @Inject constructor(
    // Added @param: to resolve the compiler warning
    @param:ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = SoundPool.Builder().setMaxStreams(6).build()
    private val sounds = mutableMapOf<String, Int>()
    private val loadedSounds = mutableSetOf<Int>()
    var isEnabled: Boolean = true

    init {
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            }
        }

        listOf("tile_click", "tile_error", "tile_match", "tile_tada", "secret_unlocked").forEach { name ->
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) {
                val id = soundPool?.load(context, resId, 1) ?: 0
                sounds[name] = id
            }
        }
    }

    fun play(name: String) {
        val soundId = sounds[name] ?: return
        if (isEnabled && loadedSounds.contains(soundId)) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}