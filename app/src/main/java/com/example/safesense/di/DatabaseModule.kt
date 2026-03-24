package com.example.safesense.di

import android.content.Context
import androidx.room.Room
import com.example.safesense.data.db.SafeSenseDatabase
import com.example.safesense.data.db.dao.ContactDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Lives for the entire app lifetime — database is a singleton
object DatabaseModule {

    @Provides
    @Singleton // Only ONE instance of the database ever exists
    fun provideSafeSenseDatabase(
        @ApplicationContext context: Context
    ): SafeSenseDatabase {
        return Room.databaseBuilder(
            context,
            SafeSenseDatabase::class.java,
            "safesense_database" // The name of the .db file stored on the device
        ).build()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: SafeSenseDatabase): ContactDao {
        return database.contactDao()
    }
}