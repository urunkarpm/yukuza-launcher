package com.yukuza.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yukuza.launcher.data.entity.AppColorCacheEntity
import com.yukuza.launcher.data.entity.AppLaunchCountEntity
import com.yukuza.launcher.data.entity.AppOrderEntity
import com.yukuza.launcher.data.entity.WeatherCacheEntity

@Database(
    entities = [
        AppOrderEntity::class,
        AppColorCacheEntity::class,
        WeatherCacheEntity::class,
        AppLaunchCountEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appOrderDao(): AppOrderDao
    abstract fun appColorCacheDao(): AppColorCacheDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun appLaunchCountDao(): AppLaunchCountDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_launch_count` " +
                        "(`packageName` TEXT NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`packageName`))"
                )
            }
        }
    }
}
