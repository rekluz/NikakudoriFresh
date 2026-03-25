package com.rekluzgames.nikakudorimahjong.domain.rules

import com.rekluzgames.nikakudorimahjong.domain.model.Tile

object HintFinder {

    fun findAllMatches(board: List<List<Tile>>): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
        if (board.isEmpty()) return emptyList()

        val rows = board.size
        val cols = board[0].size

        val matches = ArrayList<Pair<Pair<Int, Int>, Pair<Int, Int>>>(32)

        val groups = Array(34) { IntArray(rows * cols) }
        val counts = IntArray(34)

        // Build groups
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val tile = board[r][c]
                if (!tile.isRemoved) {
                    val type = tile.type
                    val idx = r * cols + c
                    groups[type][counts[type]++] = idx
                }
            }
        }

        // 🔥 OPTIMIZATION: build once
        val intBoard = toIntBoard(board)

        for (type in 0 until 34) {
            val count = counts[type]
            if (count < 2) continue

            val group = groups[type]

            for (i in 0 until count) {
                val idx1 = group[i]
                val r1 = idx1 / cols
                val c1 = idx1 % cols

                for (j in i + 1 until count) {
                    val idx2 = group[j]
                    val r2 = idx2 / cols
                    val c2 = idx2 % cols

                    if (PathFinder.canConnectFast(r1, c1, r2, c2, intBoard, rows, cols)) {
                        matches.add(Pair(Pair(r1, c1), Pair(r2, c2)))
                    }
                }
            }
        }

        return matches
    }

    private fun toIntBoard(board: List<List<Tile>>): IntArray {
        val rows = board.size
        val cols = board[0].size
        val arr = IntArray(rows * cols)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val tile = board[r][c]
                arr[idx] = if (tile.isRemoved) -1 else tile.type
            }
        }

        return arr
    }
}