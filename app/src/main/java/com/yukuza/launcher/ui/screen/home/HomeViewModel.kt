package com.yukuza.launcher.ui.screen.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yukuza.launcher.domain.model.AqiData
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.domain.model.MediaData
import com.yukuza.launcher.domain.model.NetworkData
import com.yukuza.launcher.domain.model.WeatherData
import com.yukuza.launcher.domain.usecase.GetAqiUseCase
import com.yukuza.launcher.domain.usecase.GetAppsUseCase
import com.yukuza.launcher.domain.usecase.GetMediaSessionUseCase
import com.yukuza.launcher.domain.usecase.GetNetworkSpeedUseCase
import com.yukuza.launcher.domain.usecase.GetWeatherUseCase
import com.yukuza.launcher.domain.usecase.ReorderAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val apps: ImmutableList<AppInfo> = persistentListOf(),
    val weather: WeatherData? = null,
    val aqi: AqiData? = null,
    val network: NetworkData? = null,
    val nowPlaying: MediaData? = null,
    val focusedAppIndex: Int = 0,
    val isEditMode: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getApps: GetAppsUseCase,
    private val reorderApps: ReorderAppsUseCase,
    private val getWeather: GetWeatherUseCase,
    private val getAqi: GetAqiUseCase,
    private val getNetwork: GetNetworkSpeedUseCase,
    private val getMedia: GetMediaSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Default coordinates — will be replaced by real location data from DataStore
    private val defaultLat = 19.07
    private val defaultLon = 72.87

    init {
        viewModelScope.launch {
            getApps().collect { apps ->
                _uiState.update { it.copy(apps = apps) }
            }
        }
        viewModelScope.launch {
            val weather = getWeather(defaultLat, defaultLon)
            _uiState.update { it.copy(weather = weather) }
        }
        viewModelScope.launch {
            val aqi = getAqi(defaultLat, defaultLon)
            _uiState.update { it.copy(aqi = aqi) }
        }
        viewModelScope.launch {
            getNetwork().collect { net ->
                _uiState.update { it.copy(network = net) }
            }
        }
        viewModelScope.launch {
            getMedia().collect { media ->
                _uiState.update { it.copy(nowPlaying = media) }
            }
        }
    }

    fun onAppFocused(index: Int) = _uiState.update { it.copy(focusedAppIndex = index) }
    fun enterEditMode() = _uiState.update { it.copy(isEditMode = true) }
    fun exitEditMode() = _uiState.update { it.copy(isEditMode = false) }
    fun reorder(packages: List<String>) = viewModelScope.launch { reorderApps(packages) }
}
