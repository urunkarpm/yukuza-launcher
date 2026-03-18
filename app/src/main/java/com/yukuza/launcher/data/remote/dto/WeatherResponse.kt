package com.yukuza.launcher.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current") val current: CurrentWeather,
) {
    @JsonClass(generateAdapter = true)
    data class CurrentWeather(
        @Json(name = "temperature_2m") val tempCelsius: Float,
        @Json(name = "weather_code") val weatherCode: Int,
    )
}
