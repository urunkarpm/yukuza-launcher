package com.yukuza.launcher.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_launch_count")
data class AppLaunchCountEntity(
    @PrimaryKey val packageName: String,
    val count: Int = 0,
)
