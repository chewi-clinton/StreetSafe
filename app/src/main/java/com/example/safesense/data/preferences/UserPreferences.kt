package com.example.safesense.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object UserPreferences {
    val IS_MONITORING = booleanPreferencesKey("is_monitoring")
    val LAST_ACCELEROMETER_TIMESTAMP = longPreferencesKey("last_accelerometer_timestamp")
    val USER_NAME = stringPreferencesKey("user_name")
    val SENSITIVITY = floatPreferencesKey("sensitivity")
    val SHAKE_TO_ALERT_ENABLED = booleanPreferencesKey("shake_to_alert_enabled")
    val MICROPHONE_DETECTION_ENABLED = booleanPreferencesKey("microphone_detection_enabled")
    val COUNTDOWN_DURATION_SECONDS = intPreferencesKey("countdown_duration_seconds")
    val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    val AUTO_START_ON_REBOOT = booleanPreferencesKey("auto_start_on_reboot")
    val SHOW_FALSE_POSITIVE_NUDGE = booleanPreferencesKey("show_false_positive_nudge")
    
    // Onboarding state
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

    // Audit Fix: Customizable shake thresholds
    val SHAKE_COUNT = intPreferencesKey("shake_count")
    val SHAKE_WINDOW_MS = longPreferencesKey("shake_window_ms")
}
