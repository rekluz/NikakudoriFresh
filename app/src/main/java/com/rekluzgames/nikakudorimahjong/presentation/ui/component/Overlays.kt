/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel

@Composable
fun OverlayContainer(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f)
        ) { content() }
    }
}

@Composable
fun OverlayCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .widthIn(max = 450.dp)
            .fillMaxWidth(0.9f)
            .background(Color(0xFF0D1A3A), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF00BFFF).copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

@Composable
fun OverlayTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = 24.dp)
    )
}

@Composable
fun LargeMenuButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A3A5C))
            .border(1.dp, Color(0xFF00BFFF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PauseOverlay(viewModel: GameViewModel, onExit: () -> Unit) {
    OverlayContainer {
        OverlayCard {
            OverlayTitle(stringResource(R.string.btn_menu))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LargeMenuButton(
                        stringResource(R.string.btn_new_game),
                        Modifier.weight(1f)
                    ) { viewModel.changeState(GameState.BOARDS) }
                    LargeMenuButton(
                        stringResource(R.string.btn_retry),
                        Modifier.weight(1f)
                    ) { viewModel.retryGame() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LargeMenuButton(
                        stringResource(R.string.btn_resume),
                        Modifier.weight(1f)
                    ) { viewModel.changeState(GameState.PLAYING) }
                    LargeMenuButton(
                        stringResource(R.string.btn_about),
                        Modifier.weight(1f)
                    ) { viewModel.changeState(GameState.ABOUT) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.width(140.dp)) {
                MenuPillButton(
                    stringResource(R.string.btn_exit),
                    color = Color(0xFFFF4444)
                ) { onExit() }
            }
        }
    }
}

@Composable
fun StalemateOverlay(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    OverlayContainer {
        OverlayCard {
            Text(
                stringResource(R.string.stalemate_title),
                color = Color(0xFFFF4444),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                stringResource(R.string.stalemate_subtitle),
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LargeMenuButton(
                        stringResource(R.string.btn_shuffle_format, uiState.shufflesRemaining),
                        Modifier.weight(1f)
                    ) { if (uiState.shufflesRemaining > 0) { viewModel.shuffle(); viewModel.changeState(GameState.PLAYING) } }
                    LargeMenuButton(
                        stringResource(R.string.btn_retry),
                        Modifier.weight(1f)
                    ) { viewModel.retryGame() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LargeMenuButton(
                        stringResource(R.string.btn_new_game),
                        Modifier.weight(1f)
                    ) { viewModel.changeState(GameState.BOARDS) }
                    LargeMenuButton(
                        stringResource(R.string.btn_undo),
                        Modifier.weight(1f)
                    ) { viewModel.undo(); viewModel.changeState(GameState.PLAYING) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.width(140.dp)) {
                MenuPillButton(
                    stringResource(R.string.btn_cancel),
                    color = Color.Gray
                ) { viewModel.changeState(GameState.PLAYING) }
            }
        }
    }
}

@Composable
fun LanguageOverlay(onSelect: (String) -> Unit, onClose: () -> Unit) {
    OverlayContainer {
        OverlayCard {
            OverlayTitle(stringResource(R.string.btn_language))

            // 2x2x2 grid — six languages in three rows of two
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MenuPillButton("English", color = Color(0xFF2A2A2A)) { onSelect("en") }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MenuPillButton("日本語", color = Color(0xFF2A2A2A)) { onSelect("ja") }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MenuPillButton("Italiano", color = Color(0xFF2A2A2A)) { onSelect("it") }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MenuPillButton("Français", color = Color(0xFF2A2A2A)) { onSelect("fr") }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MenuPillButton("Filipino", color = Color(0xFF2A2A2A)) { onSelect("tl") }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MenuPillButton("Español", color = Color(0xFF2A2A2A)) { onSelect("es") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.width(140.dp)) {
                MenuPillButton(
                    stringResource(R.string.btn_close),
                    color = Color(0xFFFF4444)
                ) { onClose() }
            }
        }
    }
}