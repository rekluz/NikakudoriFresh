/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel

@Composable
fun QuoteOverlay(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Fade the quote in after a short pause so the background
    // has a moment to breathe before the text appears.
    var quoteVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800L)
        quoteVisible = true
    }

    val quoteAlpha by animateFloatAsState(
        targetValue = if (quoteVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "quoteAlpha"
    )

    // Full screen tap area — tapping anywhere skips the 5 second wait
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { viewModel.dismissQuote() },
        contentAlignment = Alignment.Center
    ) {
        // Minimal quote card — dark background, no heavy border
        Column(
            modifier = Modifier
                .alpha(quoteAlpha)
                .widthIn(max = 420.dp)
                .padding(horizontal = 32.dp)
                .background(
                    color = Color(0xCC0D1A3A),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u201C${uiState.currentQuote}\u201D",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "close",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}