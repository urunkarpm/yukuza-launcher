package com.yukuza.launcher.ui.screen.home

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yukuza.launcher.data.remote.GeocodingApi
import com.yukuza.launcher.data.remote.GeocodingResult
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
import kotlinx.coroutines.flow.first
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
    val isNightMode: Boolean = false,
    val isAmbient: Boolean = false,
    val showSettings: Boolean = false,
    val cityQuery: String = "",
    val citySuggestions: List<GeocodingResult> = emptyList(),
    val cityName: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getApps: GetAppsUseCase,
    private val reorderApps: ReorderAppsUseCase,
    private val getWeather: GetWeatherUseCase,
    private val getAqi: GetAqiUseCase,
    private val getNetwork: GetNetworkSpeedUseCase,
    private val getMedia: GetMediaSessionUseCase,
    private val dataStore: DataStore<Preferences>,
    private val geocodingApi: GeocodingApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Default coordinates — will be replaced by real location data from DataStore
    private val defaultLat = 19.07
    private val defaultLon = 72.87

    private val latKey = doublePreferencesKey("lat")
    private val lonKey = doublePreferencesKey("lon")
    private val cityNameKey = stringPreferencesKey("city_name")

    init {
        viewModelScope.launch {
            getApps().collect { apps ->
                _uiState.update { it.copy(apps = apps) }
            }
        }
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val lat = prefs[latKey] ?: defaultLat
            val lon = prefs[lonKey] ?: defaultLon
            val city = prefs[cityNameKey] ?: "Mumbai"
            _uiState.update { it.copy(cityName = city) }
            fetchWeatherForLocation(lat, lon)
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

    private fun fetchWeatherForLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val weather = getWeather(lat, lon)
            _uiState.update { it.copy(weather = weather) }
        }
        viewModelScope.launch {
            val aqi = getAqi(lat, lon)
            _uiState.update { it.copy(aqi = aqi) }
        }
    }

    fun onCityQueryChange(query: String) {
        _uiState.update { it.copy(cityQuery = query, citySuggestions = emptyList()) }
        if (query.length >= 2) {
            viewModelScope.launch {
                try {
                    val results = geocodingApi.searchCity(query)
                    _uiState.update { it.copy(citySuggestions = results.results ?: emptyList()) }
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    fun onCitySelected(result: GeocodingResult) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[latKey] = result.latitude
                prefs[lonKey] = result.longitude
                prefs[cityNameKey] = result.name
            }
            _uiState.update { it.copy(
                cityName = result.name,
                cityQuery = "",
                citySuggestions = emptyList(),
            )}
            fetchWeatherForLocation(result.latitude, result.longitude)
        }
    }

    fun onAppFocused(index: Int) = _uiState.update { it.copy(focusedAppIndex = index) }
    fun enterEditMode() = _uiState.update { it.copy(isEditMode = true) }
    fun exitEditMode() = _uiState.update { it.copy(isEditMode = false) }
    fun reorder(packages: List<String>) = viewModelScope.launch { reorderApps(packages) }
    fun setNightMode(enabled: Boolean) = _uiState.update { it.copy(isNightMode = enabled) }
    fun setAmbient(enabled: Boolean) = _uiState.update { it.copy(isAmbient = enabled) }
    fun toggleSettings() = _uiState.update { it.copy(showSettings = !it.showSettings) }
}
