package com.yukuza.launcher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yukuza.launcher.data.entity.AppColorCacheEntity

@Dao
interface AppColorCacheDao {
    @Query("SELECT * FROM app_color_cache WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppColorCacheEntity?

    @Upsert
    suspend fun upsert(entity: AppColorCacheEntity)
}
