package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.model.GameMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUIState(
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isFullScreen: Boolean = false,
    val gameMode: GameMode = GameMode.REGULAR,
    val version: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUIState())
    val uiState = _uiState.asStateFlow()
    
    // Tracks if game mode was actually changed, so GameViewModel knows if it needs to restart
    private val _modeWasChanged = MutableStateFlow(false)
    val modeWasChanged = _modeWasChanged.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val settings = repository.loadInitialSettings()
        _uiState.update {
            it.copy(
                isSoundEnabled = settings.isSoundEnabled,
                isVibrationEnabled = settings.isVibrationEnabled,
                isFullScreen = settings.isFullScreen,
                gameMode = settings.gameMode
            )
        }
        soundManager.isEnabled = settings.isSoundEnabled
    }

    fun setVersion(v: String) {
        _uiState.update { it.copy(version = v) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        repository.setSoundEnabled(enabled)
        soundManager.isEnabled = enabled
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        repository.setVibrationEnabled(enabled)
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun toggleFullScreen() {
        val next = !_uiState.value.isFullScreen
        repository.setFullScreen(next)
        _uiState.update { it.copy(isFullScreen = next) }
    }

    fun toggleGameMode() {
        val newMode = if (_uiState.value.gameMode == GameMode.REGULAR) GameMode.GRAVITY else GameMode.REGULAR
        repository.setGameMode(newMode)
        _uiState.update { it.copy(gameMode = newMode) }
        _modeWasChanged.value = true
    }
    
    fun acknowledgeModeChange() {
        _modeWasChanged.value = false
    }
}
