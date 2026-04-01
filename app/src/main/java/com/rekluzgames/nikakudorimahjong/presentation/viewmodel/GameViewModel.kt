/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.audio.MusicManager
import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.engine.BackgroundManager
import com.rekluzgames.nikakudorimahjong.domain.engine.GameEngine
import com.rekluzgames.nikakudorimahjong.domain.engine.LayeredGameEngine
import com.rekluzgames.nikakudorimahjong.domain.engine.PostMatchProcessor
import com.rekluzgames.nikakudorimahjong.domain.model.*
import com.rekluzgames.nikakudorimahjong.domain.rules.BoardGenerator
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.LayeredBoardGenerator
import com.rekluzgames.nikakudorimahjong.domain.rules.LayeredHintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.PathFinder
import com.rekluzgames.nikakudorimahjong.presentation.score.ScoreManager
import com.rekluzgames.nikakudorimahjong.presentation.timer.GameTimer
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.AboutInteractionHandler
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.TileInteractionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val soundManager: SoundManager,
    private val musicManager: MusicManager,
    private val hapticManager: HapticManager,
    private val engine: GameEngine,
    private val layeredEngine: LayeredGameEngine,
    private val quoteManager: QuoteManager,
    private val scoreManager: ScoreManager,
    private val gameTimer: GameTimer,
    private val repository: GameRepository,
    private val backgroundManager: BackgroundManager,
    private val postMatchProcessor: PostMatchProcessor
) : ViewModel() {

    private val tileHandler = TileInteractionHandler(engine)
    private val aboutHandler = AboutInteractionHandler(soundManager, hapticManager)

    private val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()

    val timeSeconds = gameTimer.timeSeconds
    val timeFormatted: StateFlow<String> = gameTimer.timeSeconds
        .map { s -> "%02d:%02d".format(s / 60, s % 60) }
        .stateIn(viewModelScope, SharingStarted.Lazily, "00:00")

    private var autoCompleteJob: Job? = null
    private var matchLineJob: Job? = null
    private var inactivityJob: Job? = null
    private var quoteJob: Job? = null

    init {
        loadSettingsAndScores()
        startNewGame(Difficulty.NORMAL)
    }

    private fun loadSettingsAndScores() {
        _uiState.update { it.copy(highScores = scoreManager.getAllHighScores()) }
    }

    // -------------------------------------------------------------------------
    // Simple state modifiers & Menus
    // -------------------------------------------------------------------------

    fun changeState(newState: GameState) {
        _uiState.update {
            if (it.gameState == GameState.PLAYING && newState != GameState.PLAYING) {
                gameTimer.pause()
            } else if (it.gameState != GameState.PLAYING && newState == GameState.PLAYING) {
                gameTimer.start(viewModelScope)
            }
            it.copy(previousState = it.gameState, gameState = newState)
        }
    }

    fun applySettingsAndResume(
        modeChanged: Boolean,
        boardTypeChanged: Boolean,
        acknowledgeModeChange: () -> Unit,
        acknowledgeBoardTypeChange: () -> Unit,
        currentDifficulty: Difficulty,
        isLayeredMode: Boolean
    ) {
        acknowledgeBoardTypeChange()

        when {
            // Board type switched to 3D → start a layered game
            boardTypeChanged && isLayeredMode -> {
                acknowledgeModeChange()
                startNewLayeredGame(LayeredLayouts.PYRAMID)
            }

            // Board type switched to 2D → start a fresh flat game
            boardTypeChanged && !isLayeredMode -> {
                acknowledgeModeChange()
                startNewGame(currentDifficulty)
            }

            // Only the flat game mode (Regular/Gravity) changed
            modeChanged -> {
                startNewGame(currentDifficulty)
                acknowledgeModeChange()
            }

            // Nothing structural changed — just resume
            else -> changeState(GameState.PLAYING)
        }
    }

    fun goBack() {
        val s = _uiState.value
        when (s.gameState) {
            GameState.OPTIONS -> changeState(s.previousState)
            GameState.BOARDS  -> changeState(s.previousState)
            GameState.PAUSED  -> changeState(GameState.PLAYING)
            GameState.SCORE   -> changeState(s.previousState)
            GameState.ABOUT   -> changeState(s.previousState)
            else -> {}
        }
    }

    // -------------------------------------------------------------------------
    // Score handling
    // -------------------------------------------------------------------------

    fun selectScoreTab(tab: String) { _uiState.update { it.copy(selectedScoreTab = tab) } }
    fun clearLastSavedScore() { _uiState.update { it.copy(lastSavedScore = null) } }
    fun updatePlayerName(name: String) { _uiState.update { it.copy(playerName = name) } }

    fun clearScores(diffLabel: String) {
        scoreManager.clearScores(diffLabel)
        _uiState.update { it.copy(highScores = scoreManager.getAllHighScores()) }
    }

    fun saveScoreAndShowBoard() {
        val state = _uiState.value
        val newScore = scoreManager.processWin(
            playerName = state.playerName,
            timeSeconds = gameTimer.timeSeconds.value,
            difficulty = state.difficulty,
            usedHint = state.usedHint,
            usedShuffle = state.usedShuffle
        )
        _uiState.update {
            it.copy(
                highScores = scoreManager.getAllHighScores(),
                selectedScoreTab = state.difficulty.label,
                lastSavedScore = newScore,
                gameState = GameState.SCORE
            )
        }
    }

    // -------------------------------------------------------------------------
    // Core game flow — flat mode
    // -------------------------------------------------------------------------

    private fun cancelAllGameLogicJobs() {
        inactivityJob?.cancel()
        autoCompleteJob?.cancel()
        matchLineJob?.cancel()
        quoteJob?.cancel()
    }

    fun startNewGame(diff: Difficulty) {
        cancelAllGameLogicJobs()
        gameTimer.reset()

        _uiState.update {
            it.copy(
                gameState = GameState.LOADING,
                difficulty = diff,
                isLayeredMode = false,
                undoHistory = emptyList(),
                selectedTile = null,
                allAvailableHints = emptyList(),
                currentHintIndex = -1,
                lastMatchPath = null,
                lastMatchedPair = null,
                usedHint = false,
                usedShuffle = false,
                lastSavedScore = null,
                playerName = "",
                backgroundImageName = backgroundManager.next(it.backgroundImageName),
                currentQuote = quoteManager.next(repository.getLanguage())
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val board = BoardGenerator.createBoard(diff)
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = BOARD_GENERATION_MIN_DELAY_MS - elapsed
            if (remaining > 0) delay(remaining)

            _uiState.update {
                it.copy(
                    board = board,
                    originalBoard = board,
                    gameState = GameState.PLAYING,
                    shufflesRemaining = diff.shuffles
                )
            }
            gameTimer.start(viewModelScope)
            musicManager.start()
            resetInactivityTimer()
        }
    }

    fun retryGame() {
        if (_uiState.value.isLayeredMode) { retryLayeredGame(); return }

        cancelAllGameLogicJobs()
        gameTimer.reset()

        val originalBoard = _uiState.value.originalBoard
        _uiState.update {
            it.copy(
                board = originalBoard,
                gameState = GameState.PLAYING,
                shufflesRemaining = it.difficulty.shuffles,
                undoHistory = emptyList(),
                selectedTile = null,
                allAvailableHints = emptyList(),
                currentHintIndex = -1,
                lastMatchPath = null,
                lastMatchedPair = null,
                usedHint = false,
                usedShuffle = false,
                playerName = ""
            )
        }
        gameTimer.start(viewModelScope)
        resetInactivityTimer()
    }

    private fun handleWin() {
        cancelAllGameLogicJobs()
        gameTimer.pause()
        soundManager.play("tile_tada")
        hapticManager.gameWin()

        _uiState.update {
            it.copy(gameState = GameState.QUOTE, lastMatchPath = null, lastMatchedPair = null)
        }

        quoteJob = viewModelScope.launch {
            delay(QUOTE_SCREEN_DURATION_MS)
            if (_uiState.value.gameState == GameState.QUOTE) {
                _uiState.update { it.copy(playerName = "", gameState = GameState.SCORE_ENTRY) }
            }
        }
    }

    fun dismissQuote() {
        quoteJob?.cancel()
        _uiState.update { it.copy(playerName = "", gameState = GameState.SCORE_ENTRY) }
    }

    fun refreshQuote() {
        _uiState.update {
            it.copy(currentQuote = quoteManager.next(repository.getLanguage()))
        }
    }

    // -------------------------------------------------------------------------
    // Core game flow — layered mode
    // -------------------------------------------------------------------------

    fun startNewLayeredGame(layout: LayeredLayout) {
        cancelAllGameLogicJobs()
        gameTimer.reset()

        _uiState.update {
            it.copy(
                gameState = GameState.LOADING,
                isLayeredMode = true,
                currentLayeredLayout = layout,
                layeredTiles = emptyList(),
                originalLayeredTiles = emptyList(),
                selectedLayeredTileId = null,
                layeredHints = emptyList(),
                currentLayeredHintIndex = -1,
                layeredUndoHistory = emptyList(),
                lastMatchPath = null,
                lastMatchedPair = null,
                usedHint = false,
                usedShuffle = false,
                lastSavedScore = null,
                playerName = "",
                backgroundImageName = backgroundManager.next(it.backgroundImageName),
                currentQuote = quoteManager.next(repository.getLanguage())
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val tiles = LayeredBoardGenerator.generate(layout)
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = BOARD_GENERATION_MIN_DELAY_MS - elapsed
            if (remaining > 0) delay(remaining)

            _uiState.update {
                it.copy(
                    layeredTiles = tiles,
                    originalLayeredTiles = tiles,
                    gameState = GameState.PLAYING,
                    shufflesRemaining = 0
                )
            }
            gameTimer.start(viewModelScope)
            musicManager.start()
            resetInactivityTimer()
        }
    }

    private fun retryLayeredGame() {
        cancelAllGameLogicJobs()
        gameTimer.reset()

        val original = _uiState.value.originalLayeredTiles
        _uiState.update {
            it.copy(
                layeredTiles = original,
                gameState = GameState.PLAYING,
                selectedLayeredTileId = null,
                layeredHints = emptyList(),
                currentLayeredHintIndex = -1,
                layeredUndoHistory = emptyList(),
                lastMatchPath = null,
                lastMatchedPair = null,
                usedHint = false,
                usedShuffle = false,
                playerName = ""
            )
        }
        gameTimer.start(viewModelScope)
        resetInactivityTimer()
    }

    // -------------------------------------------------------------------------
    // Tile interaction — flat mode
    // -------------------------------------------------------------------------

    fun handleTileClick(r: Int, c: Int) {
        if (autoCompleteJob?.isActive == true) return
        resetInactivityTimer()
        hapticManager.tileSelect()

        val result = tileHandler.handleClick(_uiState.value, r, c)
        _uiState.update { result.newState }
        result.playSound?.let { soundManager.play(it) }

        when (result.playSound) {
            "tile_error" -> hapticManager.tileError()
            "tile_match" -> hapticManager.tileMatch()
        }

        result.matchPath?.let { path ->
            showMatchLine(path, result.matchedPair!!.first, result.matchedPair.second)
        }

        result.matchedBoard?.let { board ->
            viewModelScope.launch {
                postMatchProcessor.process(
                    board = board,
                    getBoard = { _uiState.value.board },
                    updateBoard = { newBoard -> _uiState.update { it.copy(board = newBoard) } },
                    onStalemate = { changeState(GameState.NO_MOVES) },
                    onWin = { handleWin() }
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tile interaction — layered mode
    // -------------------------------------------------------------------------

    fun handleLayeredTileClick(id: Int) {
        if (autoCompleteJob?.isActive == true) return
        resetInactivityTimer()

        val state = _uiState.value
        val tiles = state.layeredTiles
        val tapped = tiles.firstOrNull { it.id == id && !it.isRemoved } ?: return

        if (!layeredEngine.isFree(tapped, tiles)) {
            hapticManager.tileError()
            soundManager.play("tile_error")
            return
        }

        val currentSelection = state.selectedLayeredTileId

        when {
            currentSelection == id -> {
                _uiState.update { it.copy(selectedLayeredTileId = null) }
            }
            currentSelection == null -> {
                hapticManager.tileSelect()
                _uiState.update { it.copy(selectedLayeredTileId = id) }
            }
            else -> {
                val newTiles = layeredEngine.attemptMatch(currentSelection, id, tiles)
                if (newTiles != null) {
                    hapticManager.tileMatch()
                    soundManager.play("tile_match")

                    val snapshot = tiles
                    _uiState.update {
                        it.copy(
                            layeredTiles = newTiles,
                            selectedLayeredTileId = null,
                            layeredHints = emptyList(),
                            currentLayeredHintIndex = -1,
                            layeredUndoHistory = it.layeredUndoHistory + listOf(snapshot)
                        )
                    }

                    viewModelScope.launch {
                        when {
                            layeredEngine.isGameOver(newTiles) -> {
                                delay(HANDLE_WIN_ANIMATION_DELAY_MS)
                                handleWin()
                            }
                            layeredEngine.isStalemate(newTiles) -> {
                                changeState(GameState.NO_MOVES)
                                hapticManager.noMoves()
                            }
                        }
                    }
                } else {
                    hapticManager.tileSelect()
                    _uiState.update { it.copy(selectedLayeredTileId = id) }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Hint
    // -------------------------------------------------------------------------

    fun getHint() {
        if (_uiState.value.isLayeredMode) { getLayeredHint(); return }

        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return
        if (state.canFinish) { autoComplete(); return }

        _uiState.update { it.copy(usedHint = true) }

        if (state.allAvailableHints.isEmpty()) {
            val hints = HintFinder.findAllMatches(state.board)
            if (hints.isNotEmpty()) {
                _uiState.update { it.copy(allAvailableHints = hints, currentHintIndex = 0) }
            } else {
                changeState(GameState.NO_MOVES)
                hapticManager.noMoves()
            }
        } else {
            _uiState.update {
                it.copy(currentHintIndex = (it.currentHintIndex + 1) % it.allAvailableHints.size)
            }
        }
    }

    private fun getLayeredHint() {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return
        if (state.canFinish) { autoCompleteLayered(); return }

        _uiState.update { it.copy(usedHint = true) }

        if (state.layeredHints.isEmpty()) {
            val hints = LayeredHintFinder.findAllMatches(state.layeredTiles, layeredEngine)
            if (hints.isNotEmpty()) {
                _uiState.update { it.copy(layeredHints = hints, currentLayeredHintIndex = 0) }
            } else {
                changeState(GameState.NO_MOVES)
                hapticManager.noMoves()
            }
        } else {
            _uiState.update {
                it.copy(
                    currentLayeredHintIndex =
                        (it.currentLayeredHintIndex + 1) % it.layeredHints.size
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Undo
    // -------------------------------------------------------------------------

    fun undo() {
        if (_uiState.value.isLayeredMode) { undoLayered(); return }

        val state = _uiState.value
        if (state.undoHistory.isEmpty() || autoCompleteJob?.isActive == true) return

        // Ensure we treat the board from history as the correct 2D type
        val previousBoard = state.undoHistory.last()
        _uiState.update {
            it.copy(
                board = previousBoard,
                undoHistory = it.undoHistory.dropLast(1),
                selectedTile = null,
                allAvailableHints = emptyList(),
                currentHintIndex = -1,
                lastMatchPath = null,
                lastMatchedPair = null
            )
        }
        resetInactivityTimer()
        viewModelScope.launch(Dispatchers.Default) {
            if (HintFinder.findAllMatches(previousBoard).isEmpty()) {
                withContext(Dispatchers.Main) {
                    changeState(GameState.NO_MOVES)
                    hapticManager.noMoves()
                }
            }
        }
    }

    private fun undoLayered() {
        val state = _uiState.value
        if (state.layeredUndoHistory.isEmpty() || autoCompleteJob?.isActive == true) return

        val previous = state.layeredUndoHistory.last()
        _uiState.update {
            it.copy(
                layeredTiles = previous,
                layeredUndoHistory = it.layeredUndoHistory.dropLast(1),
                selectedLayeredTileId = null,
                layeredHints = emptyList(),
                currentLayeredHintIndex = -1
            )
        }
        resetInactivityTimer()
    }

    // -------------------------------------------------------------------------
    // Shuffle
    // -------------------------------------------------------------------------

    fun shuffle() {
        if (_uiState.value.isLayeredMode) return

        val state = _uiState.value
        if (state.shufflesRemaining <= 0 || autoCompleteJob?.isActive == true) return

        val activeTiles = state.board.flatten().filter { !it.isRemoved }
        if (activeTiles.isEmpty()) return

        val shuffledTiles = activeTiles.shuffled()
        var index = 0
        val newBoard = state.board.map { row ->
            row.map { tile ->
                if (tile.isRemoved) tile else shuffledTiles[index++]
            }
        }

        _uiState.update {
            it.copy(
                board = newBoard,
                shufflesRemaining = it.shufflesRemaining - 1,
                usedShuffle = true,
                selectedTile = null,
                allAvailableHints = emptyList(),
                currentHintIndex = -1
            )
        }
        soundManager.play("shuffle")
        hapticManager.shuffle()
        resetInactivityTimer()
    }

    // -------------------------------------------------------------------------
    // About Secret delegation
    // -------------------------------------------------------------------------

    fun onAboutTileClick(index: Int, threshold: Int) {
        _uiState.update { aboutHandler.onTileClick(it, index, threshold) }
    }

    fun closeAbout() {
        _uiState.update { aboutHandler.close(it) }
    }

    // -------------------------------------------------------------------------
    // Visuals & Feedback
    // -------------------------------------------------------------------------

    private fun showMatchLine(path: List<Pair<Int, Int>>, p1: Pair<Int, Int>, p2: Pair<Int, Int>) {
        matchLineJob?.cancel()
        matchLineJob = viewModelScope.launch {
            _uiState.update { it.copy(lastMatchPath = path, lastMatchedPair = p1 to p2) }
            delay(MATCH_LINE_DURATION_MS)
            _uiState.update { it.copy(lastMatchPath = null, lastMatchedPair = null) }
        }
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(INACTIVITY_DELAY_MS)
            _uiState.update { it.copy(allAvailableHints = emptyList(), currentHintIndex = -1) }
            getHint()
        }
    }

    private fun autoComplete() {
        autoCompleteJob?.cancel()
        autoCompleteJob = viewModelScope.launch {
            while (isActive && _uiState.value.canFinish) {
                val board = _uiState.value.board
                val matches = HintFinder.findAllMatches(board)
                if (matches.isEmpty()) break

                val (p1, p2) = matches.first()
                val path = PathFinder.getPath(p1, p2, board) ?: listOf(p1, p2)

                soundManager.play("tile_match")
                hapticManager.tileMatch()
                showMatchLine(path, p1, p2)

                val matchedBoard = engine.attemptMatch(p1, p2, board) ?: break
                _uiState.update {
                    it.copy(
                        board = matchedBoard,
                        selectedTile = null,
                        allAvailableHints = emptyList(),
                        currentHintIndex = -1
                    )
                }

                delay(AUTO_COMPLETE_DELAY_MS)
            }

            val finalBoard = _uiState.value.board
            val gravityBoard = engine.applyGravity(finalBoard)
            if (gravityBoard != finalBoard) {
                _uiState.update { it.copy(board = gravityBoard) }
                delay(BOARD_GRAVITY_ANIMATION_DELAY_MS)
            }

            if (isActive && engine.isGameOver(_uiState.value.board)) {
                delay(HANDLE_WIN_ANIMATION_DELAY_MS)
                handleWin()
            }
        }
    }

    private fun autoCompleteLayered() {
        autoCompleteJob?.cancel()
        autoCompleteJob = viewModelScope.launch {
            while (isActive && _uiState.value.canFinish) {
                val tiles = _uiState.value.layeredTiles
                val matches = LayeredHintFinder.findAllMatches(tiles, layeredEngine)
                if (matches.isEmpty()) break

                val (id1, id2) = matches.first()
                val newTiles = layeredEngine.attemptMatch(id1, id2, tiles) ?: break

                soundManager.play("tile_match")
                hapticManager.tileMatch()

                _uiState.update {
                    it.copy(
                        layeredTiles = newTiles,
                        selectedLayeredTileId = null,
                        layeredHints = emptyList(),
                        currentLayeredHintIndex = -1
                    )
                }

                delay(AUTO_COMPLETE_DELAY_MS)
            }

            if (isActive && layeredEngine.isGameOver(_uiState.value.layeredTiles)) {
                delay(HANDLE_WIN_ANIMATION_DELAY_MS)
                handleWin()
            }
        }
    }

    companion object {
        const val BOARD_GRAVITY_ANIMATION_DELAY_MS = 100L
        const val HANDLE_WIN_ANIMATION_DELAY_MS = 500L
        private const val MATCH_LINE_DURATION_MS = 300L
        private const val INACTIVITY_DELAY_MS = 15000L
        private const val AUTO_COMPLETE_DELAY_MS = 400L
        private const val BOARD_GENERATION_MIN_DELAY_MS = 600L
        private const val QUOTE_SCREEN_DURATION_MS = 4000L
    }
}