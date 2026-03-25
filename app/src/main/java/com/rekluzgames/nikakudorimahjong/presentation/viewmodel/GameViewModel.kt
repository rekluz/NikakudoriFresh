/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
// ADD THIS IMPORT:
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.engine.GameEngine
import com.rekluzgames.nikakudorimahjong.domain.model.*
import com.rekluzgames.nikakudorimahjong.domain.rules.BoardGenerator
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.PathFinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val repository: GameRepository,
    private val engine: GameEngine,
    private val quoteManager: QuoteManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()

    private val _timeSeconds = MutableStateFlow(0)
    val timeSeconds = _timeSeconds.asStateFlow()
    val timeFormatted: StateFlow<String> = _timeSeconds
        .map { s -> "%02d:%02d".format(s / 60, s % 60) }
        .stateIn(viewModelScope, SharingStarted.Lazily, "00:00")

    private val backgroundPool = listOf(
        "bg_001", "bg_002", "bg_003", "bg_004", "bg_005",
        "bg_006", "bg_007", "bg_008", "bg_009", "bg_010"
    )

    private var timerJob: Job? = null
    private var generationJob: Job? = null
    private var stalemateCheckJob: Job? = null
    private var autoCompleteJob: Job? = null
    private var matchLineJob: Job? = null
    private var inactivityJob: Job? = null
    private var quoteJob: Job? = null

    private var modeWasChanged = false

    init {
        loadSettingsAndScores()
        startNewGame(Difficulty.NORMAL)
    }

    private fun loadSettingsAndScores() {
        val settings = repository.getSettings()
        val allScores = Difficulty.entries.associate { diff ->
            val parsed = settings.getHighScores(diff.label)
                .mapNotNull { HighScore.deserialise(it) }
                .sortedBy { it.time }
                .take(5)
            diff.label to parsed
        }
        _uiState.update {
            it.copy(
                isSoundEnabled = settings.isSoundEnabled(),
                isVibrationEnabled = settings.isVibrationEnabled(),
                isFullScreen = settings.isFullScreen(),
                gameMode = settings.getGameMode(),
                highScores = allScores
            )
        }
        soundManager.isEnabled = _uiState.value.isSoundEnabled
    }

    fun setVersion(v: String) {
        _uiState.update { it.copy(version = v) }
    }

    fun saveScoreAndShowBoard() {
        val state = _uiState.value
        val medals = state.earnedMedals.toMutableList()
        if (_timeSeconds.value < 120) medals.add(Medal.FLASH)

        val newScore = HighScore(
            name = state.playerName.ifBlank { "???" },
            time = _timeSeconds.value,
            difficulty = state.difficulty.label,
            medals = medals
        )

        val settings = repository.getSettings()
        val trimmed = (settings.getHighScores(state.difficulty.label) + newScore.serialise())
            .mapNotNull { HighScore.deserialise(it) }
            .sortedBy { it.time }
            .take(5)
            .map { it.serialise() }
            .toSet()

        settings.saveHighScores(state.difficulty.label, trimmed)
        loadSettingsAndScores()

        _uiState.update {
            it.copy(
                selectedScoreTab = state.difficulty.label,
                lastSavedScore = newScore,
                gameState = GameState.SCORE
            )
        }
    }

    fun clearScores(diffLabel: String) {
        repository.getSettings().clearHighScores(diffLabel)
        loadSettingsAndScores()
    }

    fun selectScoreTab(tab: String) {
        _uiState.update { it.copy(selectedScoreTab = tab) }
    }

    fun clearLastSavedScore() {
        _uiState.update { it.copy(lastSavedScore = null) }
    }

    fun startNewGame(diff: Difficulty) {
        timerJob?.cancel()
        generationJob?.cancel()
        stalemateCheckJob?.cancel()
        inactivityJob?.cancel()
        autoCompleteJob?.cancel()
        matchLineJob?.cancel()
        quoteJob?.cancel()
        modeWasChanged = false
        _timeSeconds.value = 0

        val currentBg = _uiState.value.backgroundImageName
        val nextBg = if (backgroundPool.size > 1) {
            backgroundPool.filter { it != currentBg }.random()
        } else {
            backgroundPool.random()
        }

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
                backgroundImageName = nextBg,
                currentQuote = quoteManager.next()
            )
        }

        generationJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val board = BoardGenerator.createBoard(diff)
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = 3000L - elapsed
            if (remaining > 0) delay(remaining)

            _uiState.update {
                it.copy(
                    board = board,
                    originalBoard = board,
                    gameState = GameState.PLAYING,
                    shufflesRemaining = diff.shuffles
                )
            }
            startTimer()
            resetInactivityTimer()
        }
    }

    fun retryGame() {
        timerJob?.cancel()
        stalemateCheckJob?.cancel()
        inactivityJob?.cancel()
        autoCompleteJob?.cancel()
        matchLineJob?.cancel()
        quoteJob?.cancel()
        _timeSeconds.value = 0

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
        startTimer()
        resetInactivityTimer()
    }

    private fun handleWin() {
        timerJob?.cancel()
        inactivityJob?.cancel()
        autoCompleteJob?.cancel()
        matchLineJob?.cancel()
        soundManager.play("tile_tada")
        haptic { hapticManager.gameWin() }

        _uiState.update {
            it.copy(
                gameState = GameState.QUOTE,
                lastMatchPath = null,
                lastMatchedPair = null
            )
        }

        quoteJob = viewModelScope.launch {
            delay(5_000L)
            if (_uiState.value.gameState == GameState.QUOTE) {
                _uiState.update { it.copy(playerName = "", gameState = GameState.SCORE_ENTRY) }
            }
        }
    }

    fun dismissQuote() {
        quoteJob?.cancel()
        _uiState.update { it.copy(playerName = "", gameState = GameState.SCORE_ENTRY) }
    }

    fun handleTileClick(r: Int, c: Int) {
        val state = _uiState.value
        if (autoCompleteJob?.isActive == true) return
        if (state.gameState != GameState.PLAYING || state.board[r][c].isRemoved) return

        resetInactivityTimer()

        if (state.selectedTile == null) {
            _uiState.update {
                it.copy(selectedTile = r to c, allAvailableHints = emptyList(), currentHintIndex = -1)
            }
            soundManager.play("tile_click")
            haptic { hapticManager.tileSelect() }
        } else {
            val p1 = state.selectedTile!!
            if (p1 == r to c) {
                _uiState.update { it.copy(selectedTile = null) }
                return
            }

            val p2 = r to c
            val path = PathFinder.getPath(p1, p2, state.board)
            val matchedBoard = if (path != null) engine.attemptMatch(p1, p2, state.board) else null

            if (matchedBoard != null) {
                soundManager.play("tile_match")
                haptic { hapticManager.tileMatch() }
                showMatchLine(path!!, p1, p2)

                val boardBeforeMove = state.board

                _uiState.update {
                    it.copy(
                        board = matchedBoard,
                        selectedTile = null,
                        allAvailableHints = emptyList(),
                        currentHintIndex = -1,
                        undoHistory = (it.undoHistory + listOf(boardBeforeMove)).takeLast(20)
                    )
                }

                viewModelScope.launch {
                    val finalBoard = if (state.gameMode == GameMode.GRAVITY) {
                        delay(180L)
                        val gravityBoard = engine.applyGravity(matchedBoard)
                        _uiState.update { it.copy(board = gravityBoard) }
                        gravityBoard
                    } else {
                        matchedBoard
                    }

                    if (engine.isGameOver(finalBoard)) {
                        delay(400L)
                        handleWin()
                    } else {
                        checkForStalemate(finalBoard)
                    }
                }
            } else {
                _uiState.update {
                    it.copy(selectedTile = p2, allAvailableHints = emptyList(), currentHintIndex = -1)
                }
                soundManager.play("tile_error")
                haptic { hapticManager.tileError() }
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
                haptic { hapticManager.noMoves() }
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
        checkForStalemate(previousBoard)
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
        haptic { hapticManager.shuffle() }
        resetInactivityTimer()
        checkForStalemate(newBoard)
    }

    fun autoComplete() {
        if (!_uiState.value.canFinish || autoCompleteJob?.isActive == true) return
        timerJob?.cancel()
        inactivityJob?.cancel()
        matchLineJob?.cancel()

        autoCompleteJob = viewModelScope.launch {
            while (isActive) {
                val currentBoard = _uiState.value.board
                val matches = HintFinder.findAllMatches(currentBoard)
                if (matches.isEmpty()) {
                    if (engine.isGameOver(currentBoard)) { delay(500L); handleWin() }
                    break
                }

                val (p1, p2) = matches.first()
                val path = PathFinder.getPath(p1, p2, currentBoard) ?: listOf(p1, p2)
                soundManager.play("tile_match")
                haptic { hapticManager.tileMatch() }
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

                if (_uiState.value.gameMode == GameMode.GRAVITY) {
                    delay(180L)
                    val gravityBoard = engine.applyGravity(matchedBoard)
                    _uiState.update { it.copy(board = gravityBoard) }
                }

                delay(450L)
            }
        }
    }

    fun changeState(s: GameState) {
        _uiState.update { it.copy(previousState = it.gameState, gameState = s) }
    }

    fun goBack() {
        _uiState.update { it.copy(gameState = it.previousState) }
    }

    fun toggleFullScreen() {
        val next = !_uiState.value.isFullScreen
        repository.getSettings().setFullScreen(next)
        _uiState.update { it.copy(isFullScreen = next) }
    }

    fun toggleGameMode() {
        val newMode = if (_uiState.value.gameMode == GameMode.REGULAR) GameMode.GRAVITY else GameMode.REGULAR
        repository.getSettings().setGameMode(newMode)
        _uiState.update { it.copy(gameMode = newMode) }
        modeWasChanged = true
    }

    fun updateSoundEnabled(enabled: Boolean) {
        repository.getSettings().setSoundEnabled(enabled)
        soundManager.isEnabled = enabled
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        repository.getSettings().setVibrationEnabled(enabled)
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun applySettingsAndResume() {
        if (modeWasChanged) startNewGame(_uiState.value.difficulty)
        else changeState(GameState.PLAYING)
        modeWasChanged = false
    }

    fun updatePlayerName(name: String) {
        if (name.length <= 3) {
            val sanitised = name.uppercase().filter { it != '|' }
            _uiState.update { it.copy(playerName = sanitised) }
        }
    }

    fun onAboutTileClick(id: Int, target: Int) {
        soundManager.play("tile_click")
        _uiState.update { state ->
            val newSet = state.clearedAboutTiles + id
            if (newSet.size >= target) {
                soundManager.play("secret_unlocked")
                state.copy(aboutStage = 1, clearedAboutTiles = emptySet())
            } else {
                state.copy(clearedAboutTiles = newSet)
            }
        }
    }

    fun resetAbout() {
        _uiState.update { it.copy(aboutStage = 0, clearedAboutTiles = emptySet()) }
    }

    private fun haptic(action: () -> Unit) {
        if (_uiState.value.isVibrationEnabled) action()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.gameState == GameState.PLAYING) {
                    _timeSeconds.value += 1
                }
            }
        }
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(25_000L)
            val state = _uiState.value
            if (state.gameState == GameState.PLAYING &&
                state.allAvailableHints.isEmpty() &&
                !state.canFinish
            ) {
                val hints = HintFinder.findAllMatches(state.board)
                if (hints.isNotEmpty()) {
                    _uiState.update { it.copy(allAvailableHints = hints, currentHintIndex = 0) }
                }
            }
        }
    }

    private fun showMatchLine(path: List<Pair<Int, Int>>, p1: Pair<Int, Int>, p2: Pair<Int, Int>) {
        matchLineJob?.cancel()
        _uiState.update { it.copy(lastMatchPath = path, lastMatchedPair = p1 to p2) }
        matchLineJob = viewModelScope.launch {
            delay(400L)
            _uiState.update { it.copy(lastMatchPath = null, lastMatchedPair = null) }
        }
    }

    private fun checkForStalemate(board: List<List<Tile>>) {
        stalemateCheckJob?.cancel()
        stalemateCheckJob = viewModelScope.launch(Dispatchers.Default) {
            if (engine.isGameOver(board)) return@launch
            if (HintFinder.findAllMatches(board).isEmpty()) {
                withContext(Dispatchers.Main) {
                    changeState(GameState.NO_MOVES)
                    haptic { hapticManager.noMoves() }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        generationJob?.cancel()
        stalemateCheckJob?.cancel()
        inactivityJob?.cancel()
        autoCompleteJob?.cancel()
        matchLineJob?.cancel()
        quoteJob?.cancel()
        soundManager.release()
    }
}