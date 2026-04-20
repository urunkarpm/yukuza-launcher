package com.yukuza.launcher.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yukuza.launcher.data.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkModeEnabled: Boolean = true,
    val auroraAnimationEnabled: Boolean = true,
    val ambientModeEnabled: Boolean = true,
    val weatherWidgetEnabled: Boolean = true,
    val nowPlayingWidgetEnabled: Boolean = true,
    val networkSpeedWidgetEnabled: Boolean = true,
    val autoUpdateEnabled: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combinePreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(),
    )

    private fun combinePreferences() = 
        kotlinx.coroutines.flow.combine(
            preferencesManager.darkModeEnabled,
            preferencesManager.auroraAnimationEnabled,
            preferencesManager.ambientModeEnabled,
            preferencesManager.weatherWidgetEnabled,
            preferencesManager.nowPlayingWidgetEnabled,
            preferencesManager.networkSpeedWidgetEnabled,
            preferencesManager.autoUpdateEnabled,
        ) { darkMode, aurora, ambient, weather, nowPlaying, networkSpeed, autoUpdate ->
            SettingsUiState(
                darkModeEnabled = darkMode,
                auroraAnimationEnabled = aurora,
                ambientModeEnabled = ambient,
                weatherWidgetEnabled = weather,
                nowPlayingWidgetEnabled = nowPlaying,
                networkSpeedWidgetEnabled = networkSpeed,
                autoUpdateEnabled = autoUpdate,
                isLoading = false,
            )
        }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkModeEnabled(enabled)
        }
    }

    fun toggleAuroraAnimation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAuroraAnimationEnabled(enabled)
        }
    }

    fun toggleAmbientMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAmbientModeEnabled(enabled)
        }
    }

    fun toggleWeatherWidget(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setWeatherWidgetEnabled(enabled)
        }
    }

    fun toggleNowPlayingWidget(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNowPlayingWidgetEnabled(enabled)
        }
    }

    fun toggleNetworkSpeedWidget(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNetworkSpeedWidgetEnabled(enabled)
        }
    }

    fun toggleAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoUpdateEnabled(enabled)
        }
    }
}
