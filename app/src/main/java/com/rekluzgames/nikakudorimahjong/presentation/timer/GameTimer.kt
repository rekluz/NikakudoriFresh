package com.rekluzgames.nikakudorimahjong.presentation.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class GameTimer @Inject constructor() {
    private val _timeSeconds = MutableStateFlow(0)
    val timeSeconds = _timeSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (timerJob?.isActive == true) return
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                _timeSeconds.value += 1
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
    }

    fun reset() {
        pause()
        _timeSeconds.value = 0
    }
}
