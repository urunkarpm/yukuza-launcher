package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.WeatherRepository
import com.yukuza.launcher.domain.model.AqiData
import javax.inject.Inject

class GetAqiUseCase @Inject constructor(private val repo: WeatherRepository) {
    suspend operator fun invoke(lat: Double, lon: Double): AqiData = repo.getAqi(lat, lon)
}
