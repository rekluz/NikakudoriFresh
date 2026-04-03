/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.audio.MusicManager
import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.engine.*
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.domain.model.QuoteManager
import com.rekluzgames.nikakudorimahjong.presentation.score.ScoreManager
import com.rekluzgames.nikakudorimahjong.presentation.timer.GameTimer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.MutableStateFlow

class GameViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var soundManager: SoundManager

    @Mock
    private lateinit var musicManager: MusicManager

    @Mock
    private lateinit var hapticManager: HapticManager

    @Mock
    private lateinit var engine: GameEngine

    @Mock
    private lateinit var layeredEngine: LayeredGameEngine

    @Mock
    private lateinit var quoteManager: QuoteManager

    @Mock
    private lateinit var scoreManager: ScoreManager

    @Mock
    private lateinit var gameTimer: GameTimer

    @Mock
    private lateinit var repository: GameRepository

    @Mock
    private lateinit var backgroundManager: BackgroundManager

    @Mock
    private lateinit var postMatchProcessor: PostMatchProcessor

    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        whenever(scoreManager.getAllHighScores()).thenReturn(emptyMap())
        whenever(repository.getLanguage()).thenReturn("en")
        whenever(backgroundManager.next(any())).thenReturn("bg_1")
        whenever(quoteManager.next(any())).thenReturn("Test Quote")
        whenever(gameTimer.timeSeconds).thenReturn(MutableStateFlow(0))

        viewModel = GameViewModel(
            soundManager = soundManager,
            musicManager = musicManager,
            hapticManager = hapticManager,
            engine = engine,
            layeredEngine = layeredEngine,
            quoteManager = quoteManager,
            scoreManager = scoreManager,
            gameTimer = gameTimer,
            repository = repository,
            backgroundManager = backgroundManager,
            postMatchProcessor = postMatchProcessor
        )
    }

    @Test
    fun testPlayerNameSanitizationTrimmed() {
        viewModel.updatePlayerName("  ABC  ")

        val state = viewModel.uiState.value
        assertEquals("ABC", state.playerName)
    }

    @Test
    fun testPlayerNameCapAtThreeCharacters() {
        viewModel.updatePlayerName("TOOLONG")

        val state = viewModel.uiState.value
        assertEquals(3, state.playerName.length)
        assertEquals("TOO", state.playerName)
    }

    @Test
    fun testPlayerNameEmptyAfterTrimming() {
        viewModel.updatePlayerName("   ")

        val state = viewModel.uiState.value
        assertEquals("", state.playerName)
    }

    @Test
    fun testChangeStateUpdateGameState() {
        viewModel.changeState(GameState.PAUSED)

        val state = viewModel.uiState.value
        assertEquals(GameState.PAUSED, state.gameState)
    }

    @Test
    fun testSelectScoreTabUpdatesTab() {
        viewModel.selectScoreTab("HARD")

        val state = viewModel.uiState.value
        assertEquals("HARD", state.selectedScoreTab)
    }

    @Test
    fun testClearLastSavedScoreSetsToNull() {
        viewModel.clearLastSavedScore()

        val state = viewModel.uiState.value
        assertNull(state.lastSavedScore)
    }

    @Test
    fun testGoBackFromPausedReturnsToPlaying() {
        viewModel.changeState(GameState.PAUSED)
        viewModel.goBack()

        val state = viewModel.uiState.value
        assertEquals(GameState.PLAYING, state.gameState)
    }

    @Test
    fun testGoBackFromOptionsReturnsToPrevious() {
        viewModel.changeState(GameState.OPTIONS)
        val previousState = viewModel.uiState.value.previousState
        viewModel.goBack()

        val newState = viewModel.uiState.value
        assertEquals(previousState, newState.gameState)
    }
}