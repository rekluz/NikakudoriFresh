package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel

@Composable
fun TimerDisplay(viewModel: GameViewModel) {
    val timeString by viewModel.timeFormatted.collectAsState()
    val seconds by viewModel.timeSeconds.collectAsState()

    val targetColor = when {
        seconds < 120  -> Color.White
        seconds < 300  -> Color(0xFFFFB300)
        else              -> Color(0xFFFF4444)
    }
    val timerColor by animateColorAsState(targetColor, tween(1000), label = "timerColor")

    Box(
        modifier = Modifier.fillMaxWidth().height(38.dp).clip(CircleShape)
            .background(Color(0xFF121212)).border(1.dp, timerColor.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TIME ", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(timeString, color = timerColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}