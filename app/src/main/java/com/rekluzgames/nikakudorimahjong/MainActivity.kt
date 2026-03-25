/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rekluzgames.nikakudorimahjong.data.preference.PreferenceManager
import com.rekluzgames.nikakudorimahjong.presentation.ui.screen.GameScreen
import com.rekluzgames.nikakudorimahjong.presentation.ui.theme.NikakudoriTheme
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferenceManager: PreferenceManager

    override fun attachBaseContext(newBase: Context) {
        // Hilt isn't ready yet at this point, so we build PreferenceManager
        // manually here just for locale — this is intentional and correct.
        val prefs = PreferenceManager(newBase)
        val lang = prefs.getLanguage()
        if (lang.isNotEmpty()) {
            val locale = Locale.forLanguageTag(lang)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val gameViewModel: GameViewModel by viewModels()
            val uiState by gameViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                gameViewModel.setVersion(BuildConfig.VERSION_NAME)
            }

            LaunchedEffect(uiState.isFullScreen) {
                val window = this@MainActivity.window
                val controller = WindowCompat.getInsetsController(window, window.decorView)

                if (uiState.isFullScreen) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            NikakudoriTheme {
                GameScreen(viewModel = gameViewModel) { newLang ->
                    preferenceManager.setLanguage(newLang)
                    recreate()
                }
            }
        }
    }
}