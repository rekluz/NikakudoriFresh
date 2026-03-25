/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

data class HighScore(
    val name: String,
    val time: Int,
    val difficulty: String,
    val medals: List<Medal> = emptyList()
) {
    val timeFormatted: String get() = "%02d:%02d".format(time / 60, time % 60)

    fun serialise(): String {
        val medalNames = medals.joinToString(",") { it.name }
        return "v1|$name|$time|$difficulty|$medalNames"
    }

    companion object {
        fun deserialise(raw: String): HighScore? = try {
            val p = raw.split("|")
            if (p[0] == "v1") {
                val m = if (p.size >= 5 && p[4].isNotBlank()) {
                    p[4].split(",").mapNotNull {
                        try { Medal.valueOf(it.trim()) } catch (e: Exception) { null }
                    }
                } else emptyList()
                HighScore(p[1], p[2].toInt(), p[3], m)
            } else {
                // Legacy support for non-v1 strings
                HighScore(p[0], p[1].toInt(), p[2], emptyList())
            }
        } catch (e: Exception) { null }
    }
}