package com.example.safesense.di

import com.example.safesense.data.repository.ContactRepositoryImpl
import com.example.safesense.domain.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// @Binds is more efficient than @Provides for this case —
// it tells Hilt: "whenever someone asks for ContactRepository, give them ContactRepositoryImpl"
// No manual object creation needed — Hilt builds ContactRepositoryImpl automatically
// because it has @Inject on its constructor
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository
}