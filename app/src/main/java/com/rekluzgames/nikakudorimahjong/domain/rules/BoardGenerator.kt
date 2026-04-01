package com.rekluzgames.nikakudorimahjong.domain.rules

import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import kotlin.random.Random
import java.util.BitSet

object BoardGenerator {

    private const val MAX_ATTEMPTS = 15
    private const val TOTAL_TILE_TYPES = 34

    private val random = Random.Default

    fun createBoard(diff: Difficulty): List<List<Tile>> {
        repeat(MAX_ATTEMPTS) {
            val candidate = tryBackwardGeneration(diff)
            if (candidate != null && isBoardFullySolvable(candidate)) {
                return candidate
            }
        }
        return fallbackGeneration(diff)
    }

    private fun generateTypePool(totalTiles: Int): IntArray {
        val pool = IntArray(totalTiles)
        var index = 0

        var availableTypes = (0 until TOTAL_TILE_TYPES).shuffled(random)
        var typeIdx = 0

        fun getNextType(): Int {
            if (typeIdx >= availableTypes.size) {
                availableTypes = (0 until TOTAL_TILE_TYPES).shuffled(random)
                typeIdx = 0
            }
            return availableTypes[typeIdx++]
        }

        while (index + 4 <= totalTiles) {
            val type = getNextType()
            repeat(4) { pool[index++] = type }
        }
        while (index + 2 <= totalTiles) {
            val type = getNextType()
            repeat(2) { pool[index++] = type }
        }

        pool.shuffle(random)
        return pool
    }

    private fun tryBackwardGeneration(difficulty: Difficulty): List<List<Tile>>? {
        val rows = difficulty.rows
        val cols = difficulty.cols
        val total = rows * cols

        val typePool = generateTypePool(total)
        val board = IntArray(total) { -1 }

        val empty = IntArray(total) { it }
        var emptySize = total
        var poolIdx = 0

        while (emptySize >= 2 && poolIdx < typePool.size) {
            val type = typePool[poolIdx]
            var placed = false

            for (i in emptySize - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val tmp = empty[i]
                empty[i] = empty[j]
                empty[j] = tmp
            }

            outer@ for (i in 0 until emptySize) {
                for (j in i + 1 until emptySize) {
                    val idx1 = empty[i]
                    val idx2 = empty[j]

                    val r1 = idx1 / cols
                    val c1 = idx1 % cols
                    val r2 = idx2 / cols
                    val c2 = idx2 % cols

                    board[idx1] = type
                    board[idx2] = type

                    if (PathFinder.canConnectFast(r1, c1, r2, c2, board, rows, cols)) {
                        empty[i] = empty[emptySize - 1]
                        empty[j] = empty[emptySize - 2]
                        emptySize -= 2

                        poolIdx++
                        placed = true
                        break@outer
                    }

                    board[idx1] = -1
                    board[idx2] = -1
                }
            }

            if (!placed) return null
        }

        return List(rows) { r ->
            List(cols) { c ->
                val idx = r * cols + c
                Tile(idx, board[idx], isRemoved = false)
            }
        }
    }

    private fun isBoardFullySolvable(board: List<List<Tile>>): Boolean {
        val rows = board.size
        val cols = board[0].size
        val total = rows * cols

        val removed = BitSet(total)
        val types = IntArray(total)
        val groups = Array(TOTAL_TILE_TYPES) { mutableListOf<Int>() }

        board.flatten().forEachIndexed { i, tile ->
            types[i] = tile.type
            if (tile.isRemoved) removed.set(i)
            else groups[tile.type].add(i)
        }

        val memo = HashSet<BitSet>()

        fun backtrack(remaining: Int): Boolean {
            if (remaining == 0) return true
            if (memo.contains(removed)) return false

            val matches = findMatchesForSolver(removed, groups, rows, cols)
            if (matches.isEmpty()) {
                memo.add(removed.clone() as BitSet)
                return false
            }

            for (m in matches) {
                val i1 = m.first
                val i2 = m.second
                val t = types[i1]

                removed.set(i1)
                removed.set(i2)

                groups[t].remove(i1)
                groups[t].remove(i2)

                if (backtrack(remaining - 2)) return true

                removed.clear(i1)
                removed.clear(i2)

                groups[t].add(i1)
                groups[t].add(i2)
            }

            memo.add(removed.clone() as BitSet)
            return false
        }

        return backtrack(total - removed.cardinality())
    }

    private fun findMatchesForSolver(
        removed: BitSet,
        groups: Array<MutableList<Int>>,
        rows: Int,
        cols: Int
    ): List<Pair<Int, Int>> {
        val found = ArrayList<Pair<Int, Int>>(16)

        for (type in 0 until TOTAL_TILE_TYPES) {
            val g = groups[type]
            if (g.size < 2) continue

            for (i in 0 until g.size) {
                for (j in i + 1 until g.size) {
                    val idx1 = g[i]
                    val idx2 = g[j]

                    val r1 = idx1 / cols
                    val c1 = idx1 % cols
                    val r2 = idx2 / cols
                    val c2 = idx2 % cols

                    if (PathFinder.canConnectBitSet(r1, c1, r2, c2, removed, rows, cols)) {
                        found.add(idx1 to idx2)
                        if (found.size >= 32) return found
                    }
                }
            }
        }
        return found
    }

    private fun fallbackGeneration(difficulty: Difficulty): List<List<Tile>> {
        val rows = difficulty.rows
        val cols = difficulty.cols
        val total = rows * cols

        val pool = generateTypePool(total)

        repeat(200) {
            pool.shuffle(random)

            val board = List(rows) { r ->
                List(cols) { c ->
                    val idx = r * cols + c
                    Tile(idx, pool[idx])
                }
            }

            if (HintFinder.findAllMatches(board).isNotEmpty()) return board
        }

        return List(rows) { r ->
            List(cols) { c ->
                val idx = r * cols + c
                Tile(idx, pool[idx])
            }
        }
    }
}