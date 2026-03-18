package com.yukuza.launcher.di

import android.content.Context
import androidx.room.Room
import com.yukuza.launcher.data.db.LauncherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): LauncherDatabase =
        Room.databaseBuilder(ctx, LauncherDatabase::class.java, "launcher.db")
            .addMigrations(LauncherDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideAppOrderDao(db: LauncherDatabase) = db.appOrderDao()

    @Provides
    fun provideAppColorCacheDao(db: LauncherDatabase) = db.appColorCacheDao()

    @Provides
    fun provideWeatherCacheDao(db: LauncherDatabase) = db.weatherCacheDao()

    @Provides
    fun provideAppLaunchCountDao(db: LauncherDatabase) = db.appLaunchCountDao()
}
