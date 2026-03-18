package com.yukuza.launcher.data.repository

import com.yukuza.launcher.data.db.WeatherCacheDao
import com.yukuza.launcher.data.entity.WeatherCacheEntity
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
    private val api: OpenMeteoApi,
    private val dao: WeatherCacheDao,
) {
    suspend fun getWeather(lat: Double, lon: Double): WeatherData {
        return try {
            val weather = api.getForecast(lat, lon)
            val aqi = api.getAirQuality(lat, lon)
            val entity = WeatherCacheEntity(
                tempCelsius = weather.current.tempCelsius,
                conditionCode = weather.current.weatherCode,
                locationName = "Current Location",
                europeanAqi = aqi.current.europeanAqi,
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
            val aqi = api.getAirQuality(lat, lon)
            AqiData(
                europeanAqi = aqi.current.europeanAqi,
                category = aqiToCategory(aqi.current.europeanAqi),
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
        fun wmoCodeToDescription(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            in 51..55 -> "Drizzle"
            in 61..65 -> "Rain"
            in 71..75 -> "Snow"
            in 95..99 -> "Thunderstorm"
            else -> "Unknown"
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

// Extension functions for mapping
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
