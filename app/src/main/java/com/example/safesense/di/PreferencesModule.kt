package com.example.safesense.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// PreferencesModule.kt
// Location: di/PreferencesModule.kt
//
// PURPOSE:
//   Tells Hilt HOW to build a DataStore<Preferences> instance and WHERE to
//   inject it. Any class that asks Hilt for a DataStore<Preferences> —
//   including OnboardingViewModel — will receive THIS exact instance.
//
// WHY SINGLETON?
//   DataStore must be a singleton. If two instances point at the same file,
//   they will corrupt each other's data. The @Singleton annotation + the
//   SingletonComponent ensures Hilt creates it once and reuses it forever.
//
// WHY NOT SharedPreferences?
//   SharedPreferences can block the main thread and cause ANRs if the file
//   is large or read on the UI thread. DataStore is coroutine-native and
//   never blocks.
// ─────────────────────────────────────────────────────────────────────────────

// This extension property creates the DataStore file named "safesense_prefs"
// on the device. It lives at the top level (outside the class) because Kotlin
// extension properties on Context must be at file scope.
//
// The name "safesense_prefs" is what you will see in:
//   /data/data/com.example.safesense/files/datastore/safesense_prefs.preferences_pb
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "safesense_prefs"
)

@Module
@InstallIn(SingletonComponent::class) // Lives as long as the app process lives
object PreferencesModule {

    /**
     * Provides the single DataStore<Preferences> instance to the whole app.
     *
     * @param context The application context — injected automatically by Hilt
     *                via @ApplicationContext. Never use Activity context here
     *                because the DataStore must outlive any single screen.
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        // We call the extension property we defined above.
        // Kotlin's `by preferencesDataStore(...)` delegates handle the
        // single-instance guarantee at the Context level.
        return context.dataStore
    }
}