/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.AlphabetTile
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.MenuPillButton
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.OverlayContainer
import androidx.compose.ui.res.stringResource
import com.rekluzgames.nikakudorimahjong.R
import com.rekluzgames.nikakudorimahjong.BuildConfig
import kotlinx.coroutines.delay

import android.annotation.SuppressLint

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun AboutScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    OverlayContainer {
        // Outer Box: clips to rounded shape and holds the image background + content
        Box(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF00BFFF).copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            // ── Background image ──────────────────────────────────────────────
            Image(
                painter = painterResource(R.drawable.about_bg),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.matchParentSize()
            )

            // ── Dark scrim — adjust alpha (0.0f = invisible, 1.0f = black) ───
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )

            // ── Foreground content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Push content toward the bottom
                Spacer(Modifier.weight(3f))

                when (uiState.aboutStage) {
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT SIDE: Description and GitHub
                            Column(modifier = Modifier.weight(1.2f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(R.string.about_description),
                                    color = Color.White, fontSize = 15.sp, lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.about_github_link),
                                    color = Color(0xFF00BFFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        uriHandler.openUri("https://github.com/rekluz/Nikakudori-Mahjong-Redux/")
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }

                            // RIGHT SIDE: The Alphabet Tile Secret
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row {
                                    "REKLUZ".forEachIndexed { i, c ->
                                        AlphabetTile(c, !uiState.clearedAboutTiles.contains(i)) {
                                            viewModel.onAboutTileClick(i, 11)
                                        }
                                    }
                                }
                                Row {
                                    "GAMES".forEachIndexed { i, c ->
                                        AlphabetTile(c, !uiState.clearedAboutTiles.contains(i + 6)) {
                                            viewModel.onAboutTileClick(i + 6, 11)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                                Box(modifier = Modifier.width(180.dp)) {
                                    MenuPillButton(text = stringResource(R.string.btn_done), color = Color(0xFF2A2A2A)) {
                                        viewModel.changeState(GameState.PLAYING)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // STAGE 2: THE EASTER EGG (Profile View)
                        val scrollState = rememberScrollState()

                        // Auto-peek scroll to hint that content is scrollable
                        LaunchedEffect(Unit) {
                            delay(300)
                            scrollState.animateScrollTo(120)
                            delay(400)
                            scrollState.animateScrollTo(0)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .verticalScroll(scrollState)
                        ) {
                            // helloworld placeholder
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                        .border(3.dp, Color(0xFF00BFFF), CircleShape)
                                        .padding(4.dp)
                                        .clickable {
                                            viewModel.closeAbout()
                                            viewModel.changeState(GameState.PLAYING)
                                        }
                                ) {
                                    val id = context.resources.getIdentifier(
                                        "my_photo", "drawable", context.packageName
                                    )
                                    if (id != 0) {
                                        Image(
                                            painter = painterResource(id),
                                            contentDescription = "Developer Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = stringResource(R.string.about_created_by),
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.developer_name),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Box(Modifier.width(260.dp)) {
                                MenuPillButton(stringResource(R.string.about_thank_you), color = Color(0xFF00BFFF)) {
                                    viewModel.closeAbout()
                                    viewModel.changeState(GameState.PLAYING)
                                }
                            }
                        }
                    }
                }

                // Balance the top spacer
                Spacer(Modifier.weight(1f))
            }
        }
    }
}