package com.example.safesense.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
// abstract class (not object) because repository binding uses @Binds
// which requires an abstract function — we'll add those in Phase 1 Step 2