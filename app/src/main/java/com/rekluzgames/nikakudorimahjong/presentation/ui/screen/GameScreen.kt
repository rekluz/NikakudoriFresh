/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.rekluzgames.nikakudorimahjong.R
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.presentation.effects.ParticleOverlay
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.*
import com.rekluzgames.nikakudorimahjong.presentation.ui.theme.MidnightBlue
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settingsViewModel: com.rekluzgames.nikakudorimahjong.presentation.viewmodel.SettingsViewModel,
    onLanguageChange: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLanguageOverlay by remember { mutableStateOf(false) }
    val screenPadding = if (!settingsState.isFullScreen)
        Modifier.windowInsetsPadding(WindowInsets.systemBars)
    else
        Modifier.padding(0.dp)

    val bgResId = remember(uiState.backgroundImageName) {
        context.resources.getIdentifier(
            uiState.backgroundImageName, "drawable", context.packageName
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue)
            .then(screenPadding)
    ) {
        if (uiState.gameState == GameState.LOADING) {
            LoadingOverlay()
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bgResId != 0) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgResId)
                                .decoderFactory(GifDecoder.Factory())
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BoardGrid(uiState) { r, c -> viewModel.handleTileClick(r, c) }
                }

                Column(
                    modifier = Modifier
                        .width(125.dp)
                        .fillMaxHeight()
                        .background(Color(0x99000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MenuPillButton(
                        text = stringResource(R.string.btn_menu),
                        color = Color(0xFF2A2A2A)
                    ) { viewModel.changeState(GameState.PAUSED) }

                    val hText = if (uiState.canFinish)
                        stringResource(R.string.btn_finish)
                    else
                        stringResource(R.string.btn_hint)
                    val hColor = if (uiState.canFinish) Color(0xFFCC2200) else Color(0xFF00BFFF)
                    MenuPillButton(text = hText, color = hColor) { viewModel.getHint() }

                    MenuPillButton(
                        text = stringResource(R.string.btn_shuffle_format, uiState.shufflesRemaining),
                        enabled = uiState.shufflesRemaining > 0,
                        color = Color(0xFF708090)
                    ) { viewModel.shuffle() }

                    MenuPillButton(
                        text = stringResource(R.string.btn_undo),
                        enabled = uiState.canUndo,
                        color = Color(0xFF708090)
                    ) { viewModel.undo() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    MenuPillButton(
                        text = stringResource(R.string.btn_settings),
                        color = Color(0xFF2A2A2A)
                    ) { viewModel.changeState(GameState.OPTIONS) }

                    MenuPillButton(
                        text = stringResource(R.string.btn_boards),
                        color = Color(0xFF2A2A2A)
                    ) { viewModel.changeState(GameState.BOARDS) }

                    MenuPillButton(
                        text = stringResource(R.string.btn_language),
                        color = Color(0xFF2A2A2A)
                    ) { showLanguageOverlay = true }

                    Spacer(modifier = Modifier.weight(1f))

                    if (uiState.remainingTilesCount > 0) {
                        Text(
                            text = stringResource(R.string.remaining_tiles_format, uiState.remainingTilesCount),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TimerDisplay(viewModel = viewModel)
                }
            }
        }

        // Overlays
        when (uiState.gameState) {
            GameState.PAUSED      -> PauseOverlay(viewModel) { (context as? Activity)?.finish() }
            GameState.BOARDS      -> BoardsOverlay(viewModel)
            GameState.OPTIONS     -> SettingsOverlay(viewModel, settingsViewModel)
            GameState.ABOUT       -> AboutScreen(viewModel)
            GameState.SCORE_ENTRY -> ScoreEntryOverlay(viewModel)
            GameState.SCORE       -> ScoreboardOverlay(viewModel)
            GameState.NO_MOVES    -> StalemateOverlay(viewModel)
            GameState.QUOTE       -> QuoteOverlay(viewModel)
            else -> {}
        }

        // ── CHANGED: selectionPositions replaces selectionPos.
        //    This top-level overlay handles only the victory storm;
        //    match bursts are fired by the BoardGrid's own ParticleOverlay.
        ParticleOverlay(
            triggerVictoryStorm = uiState.gameState == GameState.WON ||
                    uiState.gameState == GameState.SCORE_ENTRY,
            selectionPositions = emptyList(),
            isScoreEntryActive = uiState.gameState == GameState.SCORE_ENTRY
        )

        if (showLanguageOverlay) {
            LanguageOverlay(
                onSelect = { lang ->
                    showLanguageOverlay = false
                    onLanguageChange(lang)
                },
                onClose = { showLanguageOverlay = false }
            )
        }
    }
}