package com.example.safesense.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// @Module marks this as a Hilt module — a class that tells Hilt how to create dependencies
// @InstallIn(SingletonComponent::class) means these dependencies live as long as the app does
// We will fill in the actual @Provides functions in Phase 1 Step 2 when we build Room

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule