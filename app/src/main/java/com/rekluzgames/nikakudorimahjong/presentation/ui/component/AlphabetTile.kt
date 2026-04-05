/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import android.annotation.SuppressLint

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun AlphabetTile(char: Char, isVisible: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    if (isVisible) {
        val resId = remember(char) {
            context.resources.getIdentifier("letter_${char.lowercaseChar()}", "drawable", context.packageName)
        }
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 50.dp) // Maintain Mahjong aspect ratio
                .padding(1.dp)
                .clickable { onClick() }
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Keeps the layout stable when tiles are cleared
        Spacer(Modifier.size(width = 38.dp, height = 50.dp).padding(1.dp))
    }
}