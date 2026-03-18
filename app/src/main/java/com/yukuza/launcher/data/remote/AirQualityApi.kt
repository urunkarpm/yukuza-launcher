package com.yukuza.launcher.data.remote

import com.yukuza.launcher.data.remote.dto.AqiResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Base URL: https://air-quality-api.open-meteo.com/
interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "european_aqi",
    ): AqiResponse
}
