package com.yukuza.launcher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yukuza.launcher.data.entity.AppOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOrderDao {
    @Query("SELECT * FROM app_order ORDER BY `order` ASC")
    fun getAll(): Flow<List<AppOrderEntity>>

    @Upsert
    suspend fun upsert(entity: AppOrderEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AppOrderEntity>)

    @Query("DELETE FROM app_order WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
