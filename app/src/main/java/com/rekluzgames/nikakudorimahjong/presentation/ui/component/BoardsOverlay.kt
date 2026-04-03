/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.domain.model.LayeredLayouts
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import androidx.compose.ui.res.stringResource
import com.rekluzgames.nikakudorimahjong.R

@Composable
fun BoardsOverlay(viewModel: GameViewModel) {
    OverlayContainer {
        OverlayCard {
            OverlayTitle(stringResource(R.string.title_select_board))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left side: 2D Boards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.boards_2d),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 2x2 grid for 2D boards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val easyDiff = Difficulty.EASY
                            BoardSmallButton(
                                text = stringResource(easyDiff.titleRes),
                                color = Color(0xFF1A5C2A),
                                accent = Color(0xFF44BB66),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewGame(easyDiff) }

                            val normalDiff = Difficulty.NORMAL
                            BoardSmallButton(
                                text = stringResource(normalDiff.titleRes),
                                color = Color(0xFF1A3A5C),
                                accent = Color(0xFF00BFFF),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewGame(normalDiff) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val hardDiff = Difficulty.HARD
                            BoardSmallButton(
                                text = stringResource(hardDiff.titleRes),
                                color = Color(0xFF5C3A1A),
                                accent = Color(0xFFFFB300),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewGame(hardDiff) }

                            val extremeDiff = Difficulty.EXTREME
                            BoardSmallButton(
                                text = stringResource(extremeDiff.titleRes),
                                color = Color(0xFF5C1A1A),
                                accent = Color(0xFFFF4444),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewGame(extremeDiff) }
                        }
                    }
                }

                // Right side: 3D Boards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.boards_3d),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 2x2 + 1 grid for 3D boards (5 total)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BoardSmallButton(
                                text = stringResource(R.string.board_pyramid),
                                color = Color(0xFF1A3A5C),
                                accent = Color(0xFF00BFFF),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewLayeredGame(LayeredLayouts.PYRAMID) }

                            BoardSmallButton(
                                text = stringResource(R.string.board_fortress),
                                color = Color(0xFF2A4A6A),
                                accent = Color(0xFF44DDFF),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewLayeredGame(LayeredLayouts.FORTRESS) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BoardSmallButton(
                                text = stringResource(R.string.board_turtle),
                                color = Color(0xFF1A5C3A),
                                accent = Color(0xFF44FF88),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewLayeredGame(LayeredLayouts.TURTLE) }

                            BoardSmallButton(
                                text = stringResource(R.string.board_bridge),
                                color = Color(0xFF4A3A6A),
                                accent = Color(0xFFAA66FF),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewLayeredGame(LayeredLayouts.BRIDGE) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BoardSmallButton(
                                text = stringResource(R.string.board_dragon),
                                color = Color(0xFF5C2A1A),
                                accent = Color(0xFFFF6644),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewLayeredGame(LayeredLayouts.DRAGON) }

                            BoardSmallButton(
                                text = stringResource(R.string.board_castle),
                                color = Color(0xFF3A4A5A),
                                accent = Color(0xFF88AACC),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.startNewLayeredGame(LayeredLayouts.CASTLE) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.width(140.dp)) {
                MenuPillButton(stringResource(R.string.btn_done), color = Color.Gray) {
                    viewModel.changeState(GameState.PLAYING)
                }
            }
        }
    }
}

@Composable
private fun BoardSmallButton(
    text: String,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}