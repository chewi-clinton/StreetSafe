package com.example.safesense.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val userName: Flow<String> = dataStore.data.map { prefs ->
        prefs[UserPreferences.USER_NAME] ?: ""
    }

    val sensitivity: Flow<Float> = dataStore.data.map { prefs ->
        prefs[UserPreferences.SENSITIVITY] ?: 0.5f
    }

    val shakeToAlertEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UserPreferences.SHAKE_TO_ALERT_ENABLED] ?: true
    }

    val microphoneDetectionEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UserPreferences.MICROPHONE_DETECTION_ENABLED] ?: false
    }

    val countdownDurationSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[UserPreferences.COUNTDOWN_DURATION_SECONDS] ?: 15
    }

    val selectedLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[UserPreferences.SELECTED_LANGUAGE] ?: "FR"
    }

    val autoStartOnReboot: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UserPreferences.AUTO_START_ON_REBOOT] ?: true
    }

    val showFalsePositiveNudge: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UserPreferences.SHOW_FALSE_POSITIVE_NUDGE] ?: false
    }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UserPreferences.ONBOARDING_COMPLETE] ?: false
    }

    // Audit Fix: Expose shake thresholds
    val shakeCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[UserPreferences.SHAKE_COUNT] ?: 3
    }

    val shakeWindowMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[UserPreferences.SHAKE_WINDOW_MS] ?: 2000L
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.USER_NAME] = name
        }
    }

    suspend fun setSensitivity(value: Float) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SENSITIVITY] = value
        }
    }

    suspend fun setShakeToAlertEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SHAKE_TO_ALERT_ENABLED] = enabled
        }
    }

    suspend fun setMicrophoneDetectionEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.MICROPHONE_DETECTION_ENABLED] = enabled
        }
    }

    suspend fun setCountdownDurationSeconds(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.COUNTDOWN_DURATION_SECONDS] = seconds
        }
    }

    suspend fun setSelectedLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SELECTED_LANGUAGE] = language
        }
    }

    suspend fun setAutoStartOnReboot(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.AUTO_START_ON_REBOOT] = enabled
        }
    }

    suspend fun setShowFalsePositiveNudge(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SHOW_FALSE_POSITIVE_NUDGE] = show
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setShakeCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SHAKE_COUNT] = count
        }
    }

    suspend fun setShakeWindowMs(windowMs: Long) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SHAKE_WINDOW_MS] = windowMs
        }
    }
}
