package com.yukuza.launcher.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class WeatherData(
    val tempCelsius: Float,
    val conditionCode: Int,       // WMO weather interpretation code
    val locationName: String,
    val fetchedAt: Long,
    val isStale: Boolean = false,
) {
    companion object
}
