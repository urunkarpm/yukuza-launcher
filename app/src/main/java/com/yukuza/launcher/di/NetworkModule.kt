package com.yukuza.launcher.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yukuza.launcher.data.remote.AirQualityApi
import com.yukuza.launcher.data.remote.GeocodingApi
import com.yukuza.launcher.data.remote.GithubReleasesApi
import com.yukuza.launcher.data.remote.OpenMeteoApi
import com.yukuza.launcher.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun moshi() = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private fun converterFactory() = MoshiConverterFactory.create(moshi())

    @Provides
    @Singleton
    fun provideOpenMeteoApi(): OpenMeteoApi =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(converterFactory())
            .build()
            .create(OpenMeteoApi::class.java)

    @Provides
    @Singleton
    fun provideAirQualityApi(): AirQualityApi =
        Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .addConverterFactory(converterFactory())
            .build()
            .create(AirQualityApi::class.java)

    @Provides
    @Singleton
    fun provideGeocodingApi(): GeocodingApi =
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .addConverterFactory(converterFactory())
            .build()
            .create(GeocodingApi::class.java)

    @Provides
    @Named("appVersion")
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    fun provideGithubReleasesApi(): GithubReleasesApi =
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(converterFactory())
            .build()
            .create(GithubReleasesApi::class.java)
}
