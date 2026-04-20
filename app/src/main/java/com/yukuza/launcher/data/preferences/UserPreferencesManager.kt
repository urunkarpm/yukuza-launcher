package com.yukuza.launcher.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences manager using DataStore.
 * Provides type-safe access to user settings and app configuration.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // Theme preferences
    val darkModeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DARK_MODE_ENABLED] ?: true
    }

    val auroraAnimationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AURORA_ANIMATION_ENABLED] ?: true
    }

    val ambientModeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AMBIENT_MODE_ENABLED] ?: true
    }

    // Widget preferences
    val weatherWidgetEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[WEATHER_WIDGET_ENABLED] ?: true
    }

    val nowPlayingWidgetEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOW_PLAYING_WIDGET_ENABLED] ?: true
    }

    val networkSpeedWidgetEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NETWORK_SPEED_WIDGET_ENABLED] ?: true
    }

    // Location preferences
    val cityLatitude: Flow<Float> = dataStore.data.map { prefs ->
        prefs[CITY_LATITUDE] ?: 0f
    }

    val cityLongitude: Flow<Float> = dataStore.data.map { prefs ->
        prefs[CITY_LONGITUDE] ?: 0f
    }

    val cityName: Flow<String> = dataStore.data.map { prefs ->
        prefs[CITY_NAME] ?: ""
    }

    // App display preferences
    val hiddenApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[HIDDEN_APPS]?.split(",")?.toSet() ?: emptySet()
    }

    val favoriteApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[FAVORITE_APPS]?.split(",")?.toSet() ?: emptySet()
    }

    // Update preferences
    val autoUpdateEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_UPDATE_ENABLED] ?: false
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun setAuroraAnimationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AURORA_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun setAmbientModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AMBIENT_MODE_ENABLED] = enabled
        }
    }

    suspend fun setWeatherWidgetEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[WEATHER_WIDGET_ENABLED] = enabled
        }
    }

    suspend fun setNowPlayingWidgetEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOW_PLAYING_WIDGET_ENABLED] = enabled
        }
    }

    suspend fun setNetworkSpeedWidgetEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NETWORK_SPEED_WIDGET_ENABLED] = enabled
        }
    }

    suspend fun setCityLocation(lat: Float, lon: Float, name: String) {
        dataStore.edit { prefs ->
            prefs[CITY_LATITUDE] = lat
            prefs[CITY_LONGITUDE] = lon
            prefs[CITY_NAME] = name
        }
    }

    suspend fun hideApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIDDEN_APPS]?.split(",")?.toSet() ?: emptySet()
            prefs[HIDDEN_APPS] = (current + packageName).joinToString(",")
        }
    }

    suspend fun unhideApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIDDEN_APPS]?.split(",")?.toSet() ?: emptySet()
            prefs[HIDDEN_APPS] = (current - packageName).joinToString(",")
        }
    }

    suspend fun addFavoriteApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_APPS]?.split(",")?.toSet() ?: emptySet()
            prefs[FAVORITE_APPS] = (current + packageName).joinToString(",")
        }
    }

    suspend fun removeFavoriteApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_APPS]?.split(",")?.toSet() ?: emptySet()
            prefs[FAVORITE_APPS] = (current - packageName).joinToString(",")
        }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_UPDATE_ENABLED] = enabled
        }
    }

    suspend fun clearAllPreferences() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        // Theme keys
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val AURORA_ANIMATION_ENABLED = booleanPreferencesKey("aurora_animation_enabled")
        val AMBIENT_MODE_ENABLED = booleanPreferencesKey("ambient_mode_enabled")

        // Widget keys
        val WEATHER_WIDGET_ENABLED = booleanPreferencesKey("weather_widget_enabled")
        val NOW_PLAYING_WIDGET_ENABLED = booleanPreferencesKey("now_playing_widget_enabled")
        val NETWORK_SPEED_WIDGET_ENABLED = booleanPreferencesKey("network_speed_widget_enabled")

        // Location keys
        val CITY_LATITUDE = floatPreferencesKey("city_latitude")
        val CITY_LONGITUDE = floatPreferencesKey("city_longitude")
        val CITY_NAME = stringPreferencesKey("city_name")

        // App display keys
        val HIDDEN_APPS = stringPreferencesKey("hidden_apps")
        val FAVORITE_APPS = stringPreferencesKey("favorite_apps")

        // Update keys
        val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
    }
}
