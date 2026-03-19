package com.yukuza.launcher.data.repository

import com.yukuza.launcher.data.db.WeatherCacheDao
import com.yukuza.launcher.data.entity.WeatherCacheEntity
import com.yukuza.launcher.data.remote.AirQualityApi
import com.yukuza.launcher.data.remote.OpenMeteoApi
import com.yukuza.launcher.domain.model.AqiData
import com.yukuza.launcher.domain.model.AqiData.AqiCategory.FAIR
import com.yukuza.launcher.domain.model.AqiData.AqiCategory.GOOD
import com.yukuza.launcher.domain.model.AqiData.AqiCategory.MODERATE
import com.yukuza.launcher.domain.model.AqiData.AqiCategory.POOR
import com.yukuza.launcher.domain.model.AqiData.AqiCategory.VERY_POOR
import com.yukuza.launcher.domain.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: OpenMeteoApi,
    private val aqiApi: AirQualityApi,
    private val dao: WeatherCacheDao,
) {
    suspend fun getWeather(lat: Double, lon: Double, cityName: String = ""): WeatherData {
        return try {
            val weather = weatherApi.getForecast(lat, lon)
            val entity = WeatherCacheEntity(
                tempCelsius = weather.current.tempCelsius,
                conditionCode = weather.current.weatherCode,
                locationName = cityName.ifBlank { "Current Location" },
                europeanAqi = dao.get()?.europeanAqi ?: 0,
                fetchedAt = System.currentTimeMillis(),
            )
            dao.upsert(entity)
            entity.toDomain(isStale = false)
        } catch (e: Exception) {
            dao.get()?.toDomain(isStale = true) ?: WeatherData.unavailable()
        }
    }

    suspend fun getAqi(lat: Double, lon: Double): AqiData {
        return try {
            val aqi = aqiApi.getAirQuality(lat, lon)
            val europeanAqi = aqi.current.europeanAqi
            // Update cached aqi value
            dao.get()?.let { cached ->
                dao.upsert(cached.copy(europeanAqi = europeanAqi))
            }
            AqiData(
                europeanAqi = europeanAqi,
                category = aqiToCategory(europeanAqi),
                fetchedAt = System.currentTimeMillis(),
                isStale = false,
            )
        } catch (e: Exception) {
            dao.get()?.let { cached ->
                AqiData(
                    europeanAqi = cached.europeanAqi,
                    category = aqiToCategory(cached.europeanAqi),
                    fetchedAt = cached.fetchedAt,
                    isStale = true,
                )
            } ?: AqiData.unavailable()
        }
    }

    companion object {
        @androidx.annotation.StringRes
        fun wmoCodeToDescription(code: Int): Int = when (code) {
            0 -> com.yukuza.launcher.R.string.weather_clear_sky
            1 -> com.yukuza.launcher.R.string.weather_mainly_clear
            2 -> com.yukuza.launcher.R.string.weather_partly_cloudy
            3 -> com.yukuza.launcher.R.string.weather_overcast
            in 51..55 -> com.yukuza.launcher.R.string.weather_drizzle
            in 61..65 -> com.yukuza.launcher.R.string.weather_rain
            in 71..75 -> com.yukuza.launcher.R.string.weather_snow
            in 95..99 -> com.yukuza.launcher.R.string.weather_thunderstorm
            else -> com.yukuza.launcher.R.string.weather_unknown
        }

        fun aqiToCategory(aqi: Int): AqiData.AqiCategory = when {
            aqi <= 20 -> GOOD
            aqi <= 40 -> FAIR
            aqi <= 60 -> MODERATE
            aqi <= 80 -> POOR
            else -> VERY_POOR
        }
    }
}

fun WeatherCacheEntity.toDomain(isStale: Boolean): WeatherData = WeatherData(
    tempCelsius = tempCelsius,
    conditionCode = conditionCode,
    locationName = locationName,
    fetchedAt = fetchedAt,
    isStale = isStale,
)

fun WeatherData.Companion.unavailable(): WeatherData = WeatherData(
    tempCelsius = 0f,
    conditionCode = -1,
    locationName = "--",
    fetchedAt = 0L,
    isStale = true,
)

fun AqiData.Companion.unavailable(): AqiData = AqiData(
    europeanAqi = -1,
    category = AqiData.AqiCategory.MODERATE,
    fetchedAt = 0L,
    isStale = true,
)
