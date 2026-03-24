package com.example.safesense.di

import android.content.Context
import androidx.room.Room
import com.example.safesense.data.db.SafeSenseDatabase
import com.example.safesense.data.db.dao.ContactDao
import com.example.safesense.data.db.dao.IncidentDao
import com.example.safesense.data.repository.IncidentRepositoryImpl
import com.example.safesense.domain.repository.IncidentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindIncidentRepository(
        impl: IncidentRepositoryImpl
    ): IncidentRepository

    companion object {
        @Provides
        @Singleton
        fun provideSafeSenseDatabase(
            @ApplicationContext context: Context
        ): SafeSenseDatabase {
            return Room.databaseBuilder(
                context,
                SafeSenseDatabase::class.java,
                "safesense_database"
            )
            .fallbackToDestructiveMigration() // Added to handle schema changes without manual migrations
            .build()
        }

        @Provides
        @Singleton
        fun provideContactDao(database: SafeSenseDatabase): ContactDao {
            return database.contactDao()
        }

        @Provides
        @Singleton
        fun provideIncidentDao(database: SafeSenseDatabase): IncidentDao {
            return database.incidentDao()
        }
    }
}
