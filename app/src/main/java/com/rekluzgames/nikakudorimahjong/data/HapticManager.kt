/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.data.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository

@Singleton
class HapticManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: GameRepository
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun vibrate(pattern: LongArray, amplitudes: IntArray, repeat: Int = -1) {
        if (!repository.isVibrationEnabled()) return
        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (it.hasAmplitudeControl()) {
                        it.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, repeat))
                    } else {
                        it.vibrate(VibrationEffect.createWaveform(pattern, repeat))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, repeat)
                }
            }
        }
    }

    fun tileSelect() = vibrate(longArrayOf(0, 10), intArrayOf(0, 50))
    fun tileMatch() = vibrate(longArrayOf(0, 20), intArrayOf(0, 100))
    fun tileError() = vibrate(longArrayOf(0, 30, 50, 30), intArrayOf(0, 200, 0, 200))
    fun shuffle() = vibrate(longArrayOf(0, 15, 30, 15), intArrayOf(0, 80, 0, 80))
    fun noMoves() = vibrate(longArrayOf(0, 100, 50, 100), intArrayOf(0, 255, 0, 255))
    fun gameWin() = vibrate(longArrayOf(0, 100, 50, 100, 50, 200), intArrayOf(0, 150, 0, 200, 0, 255))
}