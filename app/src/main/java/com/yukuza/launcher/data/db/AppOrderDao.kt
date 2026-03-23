package com.yukuza.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    // NOT used by reorder() or hide/unhide — kept for any legacy callers only
    @Upsert
    suspend fun upsertAll(entities: List<AppOrderEntity>)

    // Inserts a row only if it doesn't already exist — preserves existing order and isHidden
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: AppOrderEntity)

    // Updates isHidden without touching order
    @Query("UPDATE app_order SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, isHidden: Boolean)

    // Updates order without touching isHidden
    @Query("UPDATE app_order SET `order` = :order WHERE packageName = :packageName")
    suspend fun updateOrder(packageName: String, order: Int)

    @Query("DELETE FROM app_order WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT packageName FROM app_order")
    suspend fun getAllPackageNames(): List<String>
}
