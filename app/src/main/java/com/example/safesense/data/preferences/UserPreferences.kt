package com.example.safesense.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object UserPreferences {
    val SENSITIVITY = floatPreferencesKey("sensitivity")
    val SHAKE_TO_ALERT_ENABLED = booleanPreferencesKey("shake_to_alert_enabled")
    val MICROPHONE_DETECTION_ENABLED = booleanPreferencesKey("microphone_detection_enabled")
    val COUNTDOWN_DURATION_SECONDS = intPreferencesKey("countdown_duration_seconds")
    val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    val AUTO_START_ON_REBOOT = booleanPreferencesKey("auto_start_on_reboot")
}