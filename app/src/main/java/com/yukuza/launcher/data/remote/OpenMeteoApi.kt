package com.yukuza.launcher.data.remote

import com.yukuza.launcher.data.remote.dto.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Base URL: https://api.open-meteo.com/
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
    ): WeatherResponse
}
