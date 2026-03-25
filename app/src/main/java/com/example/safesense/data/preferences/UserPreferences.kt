package com.example.safesense.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// ─────────────────────────────────────────────────────────────────────────────
// UserPreferences.kt
// Location: data/preferences/UserPreferences.kt
//
// STEP 5 CHANGE:
//   Added IS_MONITORING key.
//
//   This boolean is written by SensorForegroundService:
//     true  → written when the service starts
//     false → written when the service stops
//
//   BootReceiver reads it on device reboot. If true, it restarts
//   SensorForegroundService automatically so the user stays protected
//   after a reboot without having to open the app.
//
//   HomeViewModel also reads it on launch to restore the correct
//   monitoring state in the UI (button shows Stop if service was running).
// ─────────────────────────────────────────────────────────────────────────────

object UserPreferences {
    // ── Step 5: Service lifecycle ─────────────────────────────────────────────
    val IS_MONITORING = booleanPreferencesKey("is_monitoring")

    // ── Existing keys — unchanged ─────────────────────────────────────────────
    val SENSITIVITY                    = floatPreferencesKey("sensitivity")
    val SHAKE_TO_ALERT_ENABLED         = booleanPreferencesKey("shake_to_alert_enabled")
    val MICROPHONE_DETECTION_ENABLED   = booleanPreferencesKey("microphone_detection_enabled")
    val COUNTDOWN_DURATION_SECONDS     = intPreferencesKey("countdown_duration_seconds")
    val SELECTED_LANGUAGE              = stringPreferencesKey("selected_language")
    val AUTO_START_ON_REBOOT           = booleanPreferencesKey("auto_start_on_reboot")
}