package com.yukuza.launcher.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_color_cache")
data class AppColorCacheEntity(
    @PrimaryKey val packageName: String,
    val dominantColor: Int,
    val extractedAt: Long,
)
