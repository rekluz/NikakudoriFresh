/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.R
import com.rekluzgames.nikakudorimahjong.domain.model.GameMode
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.domain.model.LayeredLayouts
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsOverlay(gameViewModel: GameViewModel, settingsViewModel: SettingsViewModel) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val gameState by gameViewModel.uiState.collectAsState()

    // Sync the layered flag from game state each time this overlay opens
    LaunchedEffect(Unit) {
        settingsViewModel.syncLayeredMode(gameState.isLayeredMode)
    }

    OverlayContainer {
        OverlayCard {
            OverlayTitle(stringResource(R.string.title_settings))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Row 1: [Mode | Board] | [Sound | Vibration] ──────────────
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Mode + Board type — two half-width buttons sharing one weight slot
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingGridButton(
                            title = stringResource(R.string.setting_mode),
                            status = if (settingsState.gameMode == GameMode.REGULAR)
                                stringResource(R.string.mode_regular)
                            else
                                stringResource(R.string.mode_gravity),
                            isActive = true,
                            modifier = Modifier.weight(1f)
                        ) { settingsViewModel.toggleGameMode() }

                        SettingGridButton(
                            title = stringResource(R.string.setting_board),
                            status = if (settingsState.isLayeredMode) "3D" else "2D",
                            isActive = settingsState.isLayeredMode,
                            modifier = Modifier.weight(1f)
                        ) { settingsViewModel.toggleBoardType() }
                    }

                    // Sound + Vibration — unchanged
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingGridButton(
                            title = stringResource(R.string.setting_sound),
                            status = if (settingsState.isSoundEnabled)
                                stringResource(R.string.status_on)
                            else
                                stringResource(R.string.status_off),
                            isActive = settingsState.isSoundEnabled,
                            modifier = Modifier.weight(1f)
                        ) { settingsViewModel.updateSoundEnabled(!settingsState.isSoundEnabled) }

                        SettingGridButton(
                            title = stringResource(R.string.setting_vibration),
                            status = if (settingsState.isVibrationEnabled)
                                stringResource(R.string.status_on)
                            else
                                stringResource(R.string.status_off),
                            isActive = settingsState.isVibrationEnabled,
                            modifier = Modifier.weight(1f)
                        ) { settingsViewModel.updateVibrationEnabled(!settingsState.isVibrationEnabled) }
                    }
                }

                // ── Row 2: Screen | Music + Scores — unchanged ───────────────
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingGridButton(
                        title = stringResource(R.string.setting_screen),
                        status = if (settingsState.isFullScreen)
                            stringResource(R.string.screen_full)
                        else
                            stringResource(R.string.screen_normal),
                        isActive = settingsState.isFullScreen,
                        modifier = Modifier.weight(1f)
                    ) { settingsViewModel.toggleFullScreen() }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingGridButton(
                            title = stringResource(R.string.setting_music),
                            status = if (settingsState.isMusicEnabled)
                                stringResource(R.string.status_on)
                            else
                                stringResource(R.string.status_off),
                            isActive = settingsState.isMusicEnabled,
                            modifier = Modifier.weight(1f)
                        ) { settingsViewModel.updateMusicEnabled(!settingsState.isMusicEnabled) }

                        SettingGridButton(
                            title = stringResource(R.string.setting_scores),
                            status = stringResource(R.string.status_view),
                            isActive = true,
                            modifier = Modifier.weight(1f)
                        ) { gameViewModel.changeState(GameState.SCORE) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.width(140.dp)) {
                MenuPillButton(stringResource(R.string.btn_done), color = Color(0xFF00BFFF)) {
                    gameViewModel.applySettingsAndResume(
                        modeChanged = settingsViewModel.modeWasChanged.value,
                        boardTypeChanged = settingsViewModel.boardTypeWasChanged.value,
                        acknowledgeModeChange = { settingsViewModel.acknowledgeModeChange() },
                        acknowledgeBoardTypeChange = { settingsViewModel.acknowledgeBoardTypeChange() },
                        currentDifficulty = gameViewModel.uiState.value.difficulty,
                        isLayeredMode = settingsState.isLayeredMode
                    )
                }
            }
        }
    }
}

@Composable
fun SettingGridButton(
    title: String,
    status: String,
    isActive: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bgColor     = if (isActive) Color(0xFF1A3A5C) else Color(0xFF1A1A2A)
    val borderColor = if (isActive) Color(0xFF00BFFF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
    val statusColor = if (isActive) Color(0xFF00BFFF) else Color.Gray

    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                color = statusColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}