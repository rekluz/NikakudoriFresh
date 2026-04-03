/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import androidx.compose.ui.res.stringResource
import com.rekluzgames.nikakudorimahjong.R

@Composable
fun ScoreEntryOverlay(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFormatted by viewModel.timeFormatted.collectAsState()

    OverlayContainer {
        OverlayCard {
            Text(stringResource(R.string.win_message), color = Color.Yellow, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.final_time_format, timeFormatted), color = Color.White)

            val medals = uiState.earnedMedals
            if (medals.isNotEmpty()) {
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    medals.forEach { Text(it.icon, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.score_enter_name), color = Color.Gray, fontSize = 12.sp)

            OutlinedTextField(
                value = uiState.playerName,
                onValueChange = { viewModel.updatePlayerName(it.take(3).uppercase()) },
                textStyle = TextStyle(color = Color.White, fontSize = 32.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.width(180.dp).padding(vertical = 12.dp),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Characters),
                keyboardActions = KeyboardActions(onDone = { viewModel.saveScoreAndShowBoard() }),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFF00BFFF), focusedBorderColor = Color.Yellow)
            )

            MenuPillButton(stringResource(R.string.btn_save_score), color = Color(0xFF00BFFF)) { viewModel.saveScoreAndShowBoard() }
        }
    }
}

@Composable
fun ScoreboardOverlay(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTab = uiState.selectedScoreTab
    var isConfirmingClear by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) { isConfirmingClear = false }

    OverlayContainer {
        // Added a fixed height constraint to the OverlayCard to ensure it stays within screen bounds
        OverlayCard(modifier = Modifier.fillMaxHeight(0.85f).widthIn(max = 450.dp)) {
            OverlayTitle(stringResource(R.string.title_hall_of_fame))

            // Tab Header
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Difficulty.entries.forEach { diff ->
                    val isSelected = activeTab == diff.label
                    Text(
                        text = stringResource(diff.titleRes),
                        color = if (isSelected) Color(0xFF00BFFF) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.selectScoreTab(diff.label) }.padding(4.dp)
                    )
                }
            }

            val scores = uiState.highScores[activeTab] ?: emptyList()

            // --- SCROLLABLE SECTION ---
            // .weight(1f) ensures this section expands to fill available space,
            // while .verticalScroll keeps it from pushing the footer off-screen.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (scores.isEmpty()) {
                    Text(stringResource(R.string.score_no_scores), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 40.dp), textAlign = TextAlign.Center)
                } else {
                    scores.forEachIndexed { index, score ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${index + 1}", color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                            Text(score.name, color = Color.Yellow, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                score.medals.forEach { Text(it.icon, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp)) }
                            }
                            Text(score.timeFormatted, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- FIXED FOOTER ---
            // This stays at the bottom of the OverlayCard regardless of score count.
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (isConfirmingClear) {
                        MenuPillButton(stringResource(R.string.btn_confirm), color = Color(0xFFFF4444)) {
                            viewModel.clearScores(activeTab)
                            isConfirmingClear = false
                        }
                    } else {
                        val currentDiff = Difficulty.entries.firstOrNull { it.label == activeTab }
                        val diffTitle = currentDiff?.let { stringResource(it.titleRes) } ?: activeTab
                        MenuPillButton(
                            text = stringResource(R.string.btn_clear_format, diffTitle),
                            color = Color(0xFF442222),
                            enabled = scores.isNotEmpty()
                        ) {
                            isConfirmingClear = true
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    MenuPillButton(stringResource(R.string.btn_close)) { viewModel.goBack() }
                }
            }
        }
    }
}