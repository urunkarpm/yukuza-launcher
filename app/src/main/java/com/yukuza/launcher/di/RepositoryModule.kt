package com.yukuza.launcher.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Repository bindings will be added in Tasks 5, 6, and 7
// after repository interfaces and implementations are created.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
