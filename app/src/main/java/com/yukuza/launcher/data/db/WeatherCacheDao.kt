package com.yukuza.launcher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yukuza.launcher.data.entity.WeatherCacheEntity

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE id = 1")
    suspend fun get(): WeatherCacheEntity?

    @Upsert
    suspend fun upsert(entity: WeatherCacheEntity)
}
