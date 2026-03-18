package com.yukuza.launcher.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val id: Int = 1,
    val tempCelsius: Float,
    val conditionCode: Int,
    val locationName: String,
    val europeanAqi: Int,
    val fetchedAt: Long,
)
