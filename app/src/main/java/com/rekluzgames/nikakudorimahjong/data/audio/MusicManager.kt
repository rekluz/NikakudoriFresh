/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rekluzgames.nikakudorimahjong.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = null
    var isEnabled: Boolean = true

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun start() {
        if (mediaPlayer == null) {
            val player = MediaPlayer.create(context, R.raw.gamemusic) ?: return
            player.isLooping = true
            player.start()
            mediaPlayer = player
        }
    }

    fun resume() {
        if (isEnabled) mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    override fun onStop(owner: LifecycleOwner) {
        mediaPlayer?.pause()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isEnabled) mediaPlayer?.start()
    }

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}