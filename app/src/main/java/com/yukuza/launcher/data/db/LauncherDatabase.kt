package com.yukuza.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yukuza.launcher.data.entity.AppColorCacheEntity
import com.yukuza.launcher.data.entity.AppOrderEntity
import com.yukuza.launcher.data.entity.WeatherCacheEntity

@Database(
    entities = [AppOrderEntity::class, AppColorCacheEntity::class, WeatherCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appOrderDao(): AppOrderDao
    abstract fun appColorCacheDao(): AppColorCacheDao
    abstract fun weatherCacheDao(): WeatherCacheDao
}
