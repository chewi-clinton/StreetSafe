package com.example.safesense.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// ─────────────────────────────────────────────────────────────────────────────
// UserPreferences.kt
// Location: data/preferences/UserPreferences.kt
//
// STEP 10 CHANGE vs Step 5:
//   Added LAST_ACCELEROMETER_TIMESTAMP key.
//
//   This Long is written by AccelerometerProcessor every time it processes
//   a reading (via SensorForegroundService → DataStore). It stores the
//   Unix millisecond timestamp of the last successful accelerometer reading.
//
//   SensorHeartbeatWorker reads this key every 3 minutes. If the timestamp
//   is more than 5 minutes old (or missing), the Foreground Service has
//   been silently killed by the OS — this is the Tecno/Infinix battery
//   killer problem described in the master prompt.
//   The worker fires a local notification telling the user to tap to restore.
//
// ALL OTHER KEYS ARE UNCHANGED from Step 5.
// ─────────────────────────────────────────────────────────────────────────────

object UserPreferences {
    // ── Step 5: Service lifecycle ─────────────────────────────────────────────
    val IS_MONITORING = booleanPreferencesKey("is_monitoring")

    // ── Step 10: Heartbeat timestamp ──────────────────────────────────────────
    // Written by AccelerometerProcessor (via SensorForegroundService) on every
    // accelerometer reading. Read by SensorHeartbeatWorker to detect silent kills.
    val LAST_ACCELEROMETER_TIMESTAMP = longPreferencesKey("last_accelerometer_timestamp")

    // ── User Identity ─────────────────────────────────────────────────────────
    val USER_NAME                      = stringPreferencesKey("user_name")

    // ── Existing keys — unchanged ─────────────────────────────────────────────
    val SENSITIVITY                    = floatPreferencesKey("sensitivity")
    val SHAKE_TO_ALERT_ENABLED         = booleanPreferencesKey("shake_to_alert_enabled")
    val MICROPHONE_DETECTION_ENABLED   = booleanPreferencesKey("microphone_detection_enabled")
    val COUNTDOWN_DURATION_SECONDS     = intPreferencesKey("countdown_duration_seconds")
    val SELECTED_LANGUAGE              = stringPreferencesKey("selected_language")
    val AUTO_START_ON_REBOOT           = booleanPreferencesKey("auto_start_on_reboot")
}
