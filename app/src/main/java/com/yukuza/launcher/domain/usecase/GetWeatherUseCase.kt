package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.WeatherRepository
import com.yukuza.launcher.domain.model.WeatherData
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(private val repo: WeatherRepository) {
    suspend operator fun invoke(lat: Double, lon: Double): WeatherData = repo.getWeather(lat, lon)
}
