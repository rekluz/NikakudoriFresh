package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.audio.MusicManager
import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.engine.BackgroundManager
import com.rekluzgames.nikakudorimahjong.domain.engine.GameEngine
import com.rekluzgames.nikakudorimahjong.domain.engine.PostMatchProcessor
import com.rekluzgames.nikakudorimahjong.domain.model.*
import com.rekluzgames.nikakudorimahjong.domain.rules.BoardGenerator
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
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

    fun applySettingsAndResume(modeChanged: Boolean, acknowledgeModeChange: () -> Unit, currentDifficulty: Difficulty) {
        if (modeChanged) {
            startNewGame(currentDifficulty)
            acknowledgeModeChange()
        } else {
            changeState(GameState.PLAYING)
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
    // Core game flow
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
                currentQuote = quoteManager.next()
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

    // -------------------------------------------------------------------------
    // Tile interaction
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

    fun getHint() {
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

    fun undo() {
        val state = _uiState.value
        if (state.undoHistory.isEmpty() || autoCompleteJob?.isActive == true) return

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

    fun shuffle() {
        val state = _uiState.value
        if (state.shufflesRemaining <= 0 || autoCompleteJob?.isActive == true) return

        val activeTypes = state.board.flatten().filter { !it.isRemoved }.map { it.type }.shuffled()
        var idx = 0
        val newBoard = state.board.map { row ->
            row.map { tile -> if (!tile.isRemoved) tile.copy(type = activeTypes[idx++]) else tile }
        }

        _uiState.update {
            it.copy(
                board = newBoard,
                shufflesRemaining = it.shufflesRemaining - 1,
                usedShuffle = true,
                selectedTile = null,
                allAvailableHints = emptyList(),
                currentHintIndex = -1,
                gameState = GameState.PLAYING,
                lastMatchPath = null,
                lastMatchedPair = null
            )
        }
        hapticManager.shuffle()
        resetInactivityTimer()
        viewModelScope.launch(Dispatchers.Default) {
            if (HintFinder.findAllMatches(newBoard).isEmpty()) {
                withContext(Dispatchers.Main) {
                    changeState(GameState.NO_MOVES)
                    hapticManager.noMoves()
                }
            }
        }
    }

    fun autoComplete() {
        if (!_uiState.value.canFinish || autoCompleteJob?.isActive == true) return
        inactivityJob?.cancel()
        matchLineJob?.cancel()

        autoCompleteJob = viewModelScope.launch {
            while (isActive) {
                val currentBoard = _uiState.value.board
                val matches = HintFinder.findAllMatches(currentBoard)
                if (matches.isEmpty()) {
                    if (engine.isGameOver(currentBoard)) { delay(HANDLE_WIN_ANIMATION_DELAY_MS); handleWin() }
                    break
                }

                val (p1, p2) = matches.first()
                val path = PathFinder.getPath(p1, p2, currentBoard) ?: listOf(p1, p2)
                soundManager.play("tile_match")
                hapticManager.tileMatch()
                showMatchLine(path, p1, p2)

                val matchedBoard = engine.attemptMatch(p1, p2, currentBoard) ?: break

                _uiState.update {
                    it.copy(
                        board = matchedBoard,
                        selectedTile = null,
                        allAvailableHints = emptyList(),
                        currentHintIndex = -1
                    )
                }

                delay(AUTO_COMPLETE_STEP_DELAY_MS)
                fun autoComplete() {
                    if (!_uiState.value.canFinish || autoCompleteJob?.isActive == true) return
                    inactivityJob?.cancel()
                    matchLineJob?.cancel()

                    autoCompleteJob = viewModelScope.launch {
                        var lastBoard = _uiState.value.board

                        while (isActive) {
                            val currentBoard = _uiState.value.board
                            val matches = HintFinder.findAllMatches(currentBoard)
                            if (matches.isEmpty()) {
                                if (engine.isGameOver(currentBoard)) { delay(HANDLE_WIN_ANIMATION_DELAY_MS); handleWin() }
                                break
                            }

                            val (p1, p2) = matches.first()
                            val path = PathFinder.getPath(p1, p2, currentBoard) ?: listOf(p1, p2)
                            soundManager.play("tile_match")
                            hapticManager.tileMatch()
                            showMatchLine(path, p1, p2)

                            val matchedBoard = engine.attemptMatch(p1, p2, currentBoard) ?: break
                            lastBoard = matchedBoard

                            _uiState.update {
                                it.copy(
                                    board = matchedBoard,
                                    selectedTile = null,
                                    allAvailableHints = emptyList(),
                                    currentHintIndex = -1
                                )
                            }

                            delay(AUTO_COMPLETE_STEP_DELAY_MS)
                        }

                        // Apply gravity once after all tiles are cleared
                        val gravityBoard = engine.applyGravity(lastBoard)
                        if (gravityBoard != lastBoard) {
                            delay(AUTO_COMPLETE_STEP_DELAY_MS)
                            _uiState.update { it.copy(board = gravityBoard) }
                        }

                        if (engine.isGameOver(_uiState.value.board)) {
                            delay(AUTO_COMPLETE_WIN_DELAY_MS)
                            handleWin()
                        }
                    }
                }

                if (engine.isGameOver(_uiState.value.board)) {
                    delay(AUTO_COMPLETE_WIN_DELAY_MS)
                    handleWin()
                    break
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // About screen
    // -------------------------------------------------------------------------

    fun nextAboutStage() { _uiState.update { aboutHandler.nextStage(it) } }
    fun previousAboutStage() { _uiState.update { aboutHandler.previousStage(it) } }
    fun closeAbout() { _uiState.update { aboutHandler.close(it) } }
    fun onAboutTileClick(index: Int, totalThreshold: Int = 11) {
        _uiState.update { aboutHandler.onTileClick(it, index, totalThreshold) }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private fun showMatchLine(path: List<Pair<Int, Int>>, t1: Pair<Int, Int>, t2: Pair<Int, Int>) {
        _uiState.update { it.copy(lastMatchPath = path, lastMatchedPair = Pair(t1, t2)) }
        matchLineJob?.cancel()
        matchLineJob = viewModelScope.launch {
            delay(MATCH_LINE_DISPLAY_DURATION_MS)
            _uiState.update { it.copy(lastMatchPath = null, lastMatchedPair = null) }
        }
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        if (_uiState.value.gameState != GameState.PLAYING) return
        inactivityJob = viewModelScope.launch {
            delay(HINT_INACTIVITY_DELAY_MS)
            getHint()
        }
    }

    companion object {
        const val HINT_INACTIVITY_DELAY_MS = 10_000L
        const val AUTO_COMPLETE_STEP_DELAY_MS = 150L
        const val AUTO_COMPLETE_WIN_DELAY_MS = 300L
        const val QUOTE_SCREEN_DURATION_MS = 5_000L
        const val BOARD_GENERATION_MIN_DELAY_MS = 3000L
        const val BOARD_GRAVITY_ANIMATION_DELAY_MS = 400L
        const val HANDLE_WIN_ANIMATION_DELAY_MS = 500L
        const val MATCH_LINE_DISPLAY_DURATION_MS = 500L
    }
}