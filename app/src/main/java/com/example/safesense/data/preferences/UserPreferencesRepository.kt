package com.example.safesense.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safesense_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val sensitivity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[UserPreferences.SENSITIVITY] ?: 0.5f
    }

    val shakeToAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[UserPreferences.SHAKE_TO_ALERT_ENABLED] ?: true
    }

    val microphoneDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[UserPreferences.MICROPHONE_DETECTION_ENABLED] ?: false
    }

    val countdownDurationSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[UserPreferences.COUNTDOWN_DURATION_SECONDS] ?: 15
    }

    val selectedLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[UserPreferences.SELECTED_LANGUAGE] ?: "FR"
    }

    val autoStartOnReboot: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[UserPreferences.AUTO_START_ON_REBOOT] ?: true
    }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[UserPreferences.SENSITIVITY] = value
        }
    }

    suspend fun setShakeToAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[UserPreferences.SHAKE_TO_ALERT_ENABLED] = enabled
        }
    }

    suspend fun setMicrophoneDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[UserPreferences.MICROPHONE_DETECTION_ENABLED] = enabled
        }
    }

    suspend fun setCountdownDurationSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[UserPreferences.COUNTDOWN_DURATION_SECONDS] = seconds
        }
    }

    suspend fun setSelectedLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[UserPreferences.SELECTED_LANGUAGE] = language
        }
    }

    suspend fun setAutoStartOnReboot(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[UserPreferences.AUTO_START_ON_REBOOT] = enabled
        }
    }
}