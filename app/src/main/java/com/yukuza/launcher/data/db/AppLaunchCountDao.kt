package com.yukuza.launcher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yukuza.launcher.data.entity.AppLaunchCountEntity

@Dao
interface AppLaunchCountDao {
    @Query("SELECT * FROM app_launch_count")
    suspend fun getAll(): List<AppLaunchCountEntity>

    @Query("SELECT count FROM app_launch_count WHERE packageName = :packageName")
    suspend fun getCount(packageName: String): Int?

    @Upsert
    suspend fun upsert(entity: AppLaunchCountEntity)
}
