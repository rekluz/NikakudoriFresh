package com.rekluzgames.nikakudorimahjong.domain.rules

import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import java.util.BitSet

object PathFinder {

    // =========================
    // PUBLIC API (RESTORED)
    // =========================

    fun canConnect(
        p1: Pair<Int, Int>,
        p2: Pair<Int, Int>,
        board: List<List<Tile>>
    ): Boolean {
        val r1 = p1.first
        val c1 = p1.second
        val r2 = p2.first
        val c2 = p2.second

        if (board[r1][c1].type != board[r2][c2].type) return false
        return canConnectInternal(r1, c1, r2, c2, board)
    }

    fun getPath(
        p1: Pair<Int, Int>,
        p2: Pair<Int, Int>,
        board: List<List<Tile>>
    ): List<Pair<Int, Int>>? {
        val r1 = p1.first
        val c1 = p1.second
        val r2 = p2.first
        val c2 = p2.second

        if (board[r1][c1].type != board[r2][c2].type) return null

        // Straight
        if (lineClearBoard(r1, c1, r2, c2, board)) {
            return listOf(p1, p2)
        }

        // 1 turn
        if (isPassable(r1, c2, board) &&
            lineClearBoard(r1, c1, r1, c2, board) &&
            lineClearBoard(r1, c2, r2, c2, board)
        ) return listOf(p1, Pair(r1, c2), p2)

        if (isPassable(r2, c1, board) &&
            lineClearBoard(r1, c1, r2, c1, board) &&
            lineClearBoard(r2, c1, r2, c2, board)
        ) return listOf(p1, Pair(r2, c1), p2)

        val rows = board.size
        val cols = board[0].size

        // 2 turns (rows)
        for (r in -1..rows) {
            if (isPassable(r, c1, board) &&
                isPassable(r, c2, board) &&
                lineClearBoard(r1, c1, r, c1, board) &&
                lineClearBoard(r, c1, r, c2, board) &&
                lineClearBoard(r, c2, r2, c2, board)
            ) {
                return listOf(p1, Pair(r, c1), Pair(r, c2), p2)
            }
        }

        // 2 turns (cols)
        for (c in -1..cols) {
            if (isPassable(r1, c, board) &&
                isPassable(r2, c, board) &&
                lineClearBoard(r1, c1, r1, c, board) &&
                lineClearBoard(r1, c, r2, c, board) &&
                lineClearBoard(r2, c, r2, c2, board)
            ) {
                return listOf(p1, Pair(r1, c), Pair(r2, c), p2)
            }
        }

        return null
    }

    // =========================
    // FAST VERSION (GENERATOR)
    // =========================

    fun canConnectFast(
        r1: Int, c1: Int,
        r2: Int, c2: Int,
        board: IntArray,
        rows: Int,
        cols: Int
    ): Boolean {
        if (board[r1 * cols + c1] != board[r2 * cols + c2]) return false

        fun isFree(r: Int, c: Int): Boolean {
            if (r < 0 || r >= rows || c < 0 || c >= cols) return true
            return board[r * cols + c] == -1
        }

        if (lineClear(r1, c1, r2, c2, rows, cols, ::isFree)) return true

        if (isFree(r1, c2) && lineClear(r1, c1, r1, c2, rows, cols, ::isFree) && lineClear(r1, c2, r2, c2, rows, cols, ::isFree)) return true
        if (isFree(r2, c1) && lineClear(r1, c1, r2, c1, rows, cols, ::isFree) && lineClear(r2, c1, r2, c2, rows, cols, ::isFree)) return true

        for (r in -1..rows) {
            if (isFree(r, c1) && isFree(r, c2) &&
                lineClear(r1, c1, r, c1, rows, cols, ::isFree) &&
                lineClear(r, c1, r, c2, rows, cols, ::isFree) &&
                lineClear(r, c2, r2, c2, rows, cols, ::isFree)
            ) return true
        }

        for (c in -1..cols) {
            if (isFree(r1, c) && isFree(r2, c) &&
                lineClear(r1, c1, r1, c, rows, cols, ::isFree) &&
                lineClear(r1, c, r2, c, rows, cols, ::isFree) &&
                lineClear(r2, c, r2, c2, rows, cols, ::isFree)
            ) return true
        }

        return false
    }

    fun canConnectBitSet(
        r1: Int, c1: Int,
        r2: Int, c2: Int,
        removed: BitSet,
        rows: Int,
        cols: Int
    ): Boolean {

        fun isFree(r: Int, c: Int): Boolean {
            if (r < 0 || r >= rows || c < 0 || c >= cols) return true
            return removed.get(r * cols + c)
        }

        if (lineClear(r1, c1, r2, c2, rows, cols, ::isFree)) return true

        if (isFree(r1, c2) && lineClear(r1, c1, r1, c2, rows, cols, ::isFree) && lineClear(r1, c2, r2, c2, rows, cols, ::isFree)) return true
        if (isFree(r2, c1) && lineClear(r1, c1, r2, c1, rows, cols, ::isFree) && lineClear(r2, c1, r2, c2, rows, cols, ::isFree)) return true

        for (r in -1..rows) {
            if (isFree(r, c1) && isFree(r, c2) &&
                lineClear(r1, c1, r, c1, rows, cols, ::isFree) &&
                lineClear(r, c1, r, c2, rows, cols, ::isFree) &&
                lineClear(r, c2, r2, c2, rows, cols, ::isFree)
            ) return true
        }

        for (c in -1..cols) {
            if (isFree(r1, c) && isFree(r2, c) &&
                lineClear(r1, c1, r1, c, rows, cols, ::isFree) &&
                lineClear(r1, c, r2, c, rows, cols, ::isFree) &&
                lineClear(r2, c, r2, c2, rows, cols, ::isFree)
            ) return true
        }

        return false
    }

    // =========================
    // INTERNAL (LIST VERSION)
    // =========================

    private fun canConnectInternal(
        r1: Int, c1: Int,
        r2: Int, c2: Int,
        board: List<List<Tile>>
    ): Boolean {

        if (lineClearBoard(r1, c1, r2, c2, board)) return true

        if (isPassable(r1, c2, board) &&
            lineClearBoard(r1, c1, r1, c2, board) &&
            lineClearBoard(r1, c2, r2, c2, board)
        ) return true

        if (isPassable(r2, c1, board) &&
            lineClearBoard(r1, c1, r2, c1, board) &&
            lineClearBoard(r2, c1, r2, c2, board)
        ) return true

        val rows = board.size
        val cols = board[0].size

        for (r in -1..rows) {
            if (isPassable(r, c1, board) &&
                isPassable(r, c2, board) &&
                lineClearBoard(r1, c1, r, c1, board) &&
                lineClearBoard(r, c1, r, c2, board) &&
                lineClearBoard(r, c2, r2, c2, board)
            ) return true
        }

        for (c in -1..cols) {
            if (isPassable(r1, c, board) &&
                isPassable(r2, c, board) &&
                lineClearBoard(r1, c1, r1, c, board) &&
                lineClearBoard(r1, c, r2, c, board) &&
                lineClearBoard(r2, c, r2, c2, board)
            ) return true
        }

        return false
    }

    private fun isPassable(r: Int, c: Int, board: List<List<Tile>>): Boolean {
        if (r < 0 || r >= board.size || c < 0 || c >= board[0].size) return true
        return board[r][c].isRemoved
    }

    // =========================
    // GENERIC lineClear (DRY)
    // =========================

    private fun lineClear(
        r1: Int, c1: Int,
        r2: Int, c2: Int,
        rows: Int,
        cols: Int,
        isFree: (Int, Int) -> Boolean
    ): Boolean {
        if (r1 == r2) {
            val (s, e) = if (c1 < c2) c1 to c2 else c2 to c1
            for (c in s + 1 until e) if (!isFree(r1, c)) return false
            return true
        }
        if (c1 == c2) {
            val (s, e) = if (r1 < r2) r1 to r2 else r2 to r1
            for (r in s + 1 until e) if (!isFree(r, c1)) return false
            return true
        }
        return false
    }

    private fun lineClearBoard(
        r1: Int, c1: Int,
        r2: Int, c2: Int,
        board: List<List<Tile>>
    ): Boolean {
        if (r1 == r2) {
            val (s, e) = if (c1 < c2) c1 to c2 else c2 to c1
            for (c in s + 1 until e) if (!isPassable(r1, c, board)) return false
            return true
        }
        if (c1 == c2) {
            val (s, e) = if (r1 < r2) r1 to r2 else r2 to r1
            for (r in s + 1 until e) if (!isPassable(r, c1, board)) return false
            return true
        }
        return false
    }
}