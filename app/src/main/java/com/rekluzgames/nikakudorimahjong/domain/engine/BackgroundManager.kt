package com.rekluzgames.nikakudorimahjong.domain.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundManager @Inject constructor() {

    private val pool = (1..10).map { "bg_%03d".format(it) }

    fun next(current: String?): String =
        pool.filter { it != current }.ifEmpty { pool }.random()
}