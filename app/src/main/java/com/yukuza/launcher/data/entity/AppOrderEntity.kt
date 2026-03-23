package com.yukuza.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_order")
data class AppOrderEntity(
    @PrimaryKey val packageName: String,
    val order: Int,
    @ColumnInfo(name = "isHidden", defaultValue = "0")
    val isHidden: Boolean = false,
)
