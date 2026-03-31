package com.example.safesense.domain.usecase

import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.sensor.processor.AccelerometerEvent
import com.example.safesense.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RecognizeShakeGestureUseCase.kt
// Location: domain/usecase/RecognizeShakeGestureUseCase.kt
//
// WHAT THIS DETECTS:
//   A configured number of rapid shakes within a configured time window.
//   Default: 3 shakes within 2 seconds.
//   A single "shake" is when the jerk (rate of acceleration change) exceeds
//   30 m/s³. Jerk = how quickly acceleration is changing, not acceleration
//   itself.
//
// WHY JERK INSTEAD OF RAW MAGNITUDE?
//   Raw magnitude catches every bump on a moto-taxi, every step while walking.
//   Jerk only spikes on sudden direction changes — which is exactly what
//   an intentional shake looks like. Walking and vehicle vibration produce
//   continuous low-jerk movement. A deliberate shake produces 3–5 sharp
//   high-jerk spikes in under 2 seconds. This distinction is what keeps
//   false positives near zero on Cameroonian roads.
//
// THRESHOLDS:
//   JERK_THRESHOLD   = 30 m/s³  — below this is normal movement (constant)
//   SHAKE_WINDOW_MS  = from UserPreferences (default 2000ms)
//   MIN_SHAKE_COUNT  = from UserPreferences (default 3)
//   DEBOUNCE_MS      = 300ms    — minimum gap between counted shakes
//                                 prevents one long shake counting as many
//
// AUDIT FIX — REMOVED runBlocking:
//   OLD: Called runBlocking { userPreferencesRepository.shakeToAlertEnabled.first() }
//       inside process(), which runs at up to 50Hz during active sampling.
//   PROBLEM: runBlocking blocks the calling thread. At 50Hz, that's 50 blocked
//       calls per second. This causes UI stuttering and wastes battery.
//   FIX: Added loadPreferences() suspend function. Caller must call it ONCE
//       when monitoring starts. process() reads from cached values only.
//
// AUDIT FIX — REMOVED HARDCODED THRESHOLDS:
//   OLD: const val SHAKE_WINDOW_MS = 2000L and const val MIN_SHAKE_COUNT = 3
//   PROBLEM: User can configure these in Settings (2/3/4 shakes, 1.5s/2s/2.5s window)
//       but the use case ignored the settings.
//   FIX: Read shakeCount and shakeWindowMs from UserPreferences in loadPreferences().
// ─────────────────────────────────────────────────────────────────────────────

class RecognizeShakeGestureUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    companion object {
        // Jerk threshold stays constant — not configurable in Settings
        const val JERK_THRESHOLD_MS_PER_S3 = 30.0f  // m/s³ — above this = shake
        const val DEBOUNCE_MS              = 300L    // minimum gap between shakes

        // Defaults used BEFORE loadPreferences() is called.
        // These are safe fallbacks — the app works correctly with these values.
        private const val DEFAULT_SHAKE_COUNT    = 3
        private const val DEFAULT_SHAKE_WINDOW_MS = 2000L
    }

    // ── Cached preference values — loaded ONCE by loadPreferences() ──────────
    // These are read from DataStore asynchronously and cached.
    // process() reads ONLY from these cached values — never calls runBlocking.
    private var isShakeEnabled    = true
    private var requiredShakeCount = DEFAULT_SHAKE_COUNT
    private var shakeWindowMs     = DEFAULT_SHAKE_WINDOW_MS

    // Previous event — needed to calculate jerk (change between two readings)
    private var previousMagnitude  = 0.0f
    private var previousTimestamp  = 0L

    // Shake counting
    private var shakeCount         = 0
    private var firstShakeTime     = 0L
    private var lastShakeTime      = 0L

    // ── Load preferences (call ONCE when monitoring starts) ──────────────────
    //
    // This replaces the old runBlocking call. The caller (SensorMonitoringService)
    // must call this once in a coroutine when monitoring starts. After this call,
    // process() never blocks — it reads from cached values only.
    //
    // WHY NOT COLLECT AS A FLOW?
    //   We don't need real-time updates to shake settings while monitoring.
    //   If the user changes settings, they take effect next time monitoring starts.
    //   This avoids a permanent coroutine watching DataStore for changes.

    suspend fun loadPreferences() {
        try {
            userPreferencesRepository.shakeToAlertEnabled.first().let {
                isShakeEnabled = it
            }
            userPreferencesRepository.shakeCount.first().let {
                requiredShakeCount = it
            }
            userPreferencesRepository.shakeWindowMs.first().let {
                shakeWindowMs = it
            }
            android.util.Log.d(
                "SafeSense/Shake",
                "Preferences loaded: enabled=$isShakeEnabled, count=$requiredShakeCount, window=${shakeWindowMs}ms"
            )
        } catch (e: Exception) {
            // If DataStore fails, keep the defaults — the use case still works
            android.util.Log.w("SafeSense/Shake", "Failed to load preferences, using defaults: ${e.message}")
        }
    }

    // ── Main entry point ──────────────────────────────────────────────────────
    //
    // Called by SensorForegroundService with every AccelerometerEvent.
    // Returns DetectionResult.Detected when configured number of shakes
    // happen within the configured time window.
    // Returns DetectionResult.NotDetected otherwise.
    //
    // IMPORTANT: This function NEVER blocks. It reads from cached values only.
    // If loadPreferences() has not been called, it uses safe defaults.

    fun process(event: AccelerometerEvent): DetectionResult {
        // ── FIX: Read from cached value instead of runBlocking ──
        if (!isShakeEnabled) {
            return DetectionResult.NotDetected
        }

        val magnitude = event.magnitude
        val timestamp = event.timestamp

        // We need at least two readings to calculate jerk
        if (previousTimestamp == 0L) {
            previousMagnitude = magnitude
            previousTimestamp = timestamp
            return DetectionResult.NotDetected
        }

        val timeDeltaSeconds = (timestamp - previousTimestamp) / 1000.0f

        // Guard against division by zero or nearly-zero time deltas
        // (can happen if two events arrive in the same millisecond)
        if (timeDeltaSeconds < 0.001f) {
            return DetectionResult.NotDetected
        }

        // Jerk = change in acceleration / change in time
        val jerk = Math.abs(magnitude - previousMagnitude) / timeDeltaSeconds

        previousMagnitude = magnitude
        previousTimestamp = timestamp

        if (jerk >= JERK_THRESHOLD_MS_PER_S3) {
            return recordShake(timestamp)
        }

        return DetectionResult.NotDetected
    }

    // ── Shake counting ────────────────────────────────────────────────────────

    private fun recordShake(timestamp: Long): DetectionResult {
        // Debounce: ignore if a shake was recorded too recently
        // Without this, one violent shake registers as 5+ shakes
        if (timestamp - lastShakeTime < DEBOUNCE_MS) {
            return DetectionResult.NotDetected
        }

        lastShakeTime = timestamp

        if (shakeCount == 0) {
            // First shake — start the window timer
            firstShakeTime = timestamp
            shakeCount = 1
        } else if (timestamp - firstShakeTime <= shakeWindowMs) {
            // ── FIX: Use cached shakeWindowMs instead of hardcoded constant ──
            // Within the window — count it
            shakeCount++

            // ── FIX: Use cached requiredShakeCount instead of hardcoded constant ──
            if (shakeCount >= requiredShakeCount) {
                // Gesture confirmed — reset and report
                android.util.Log.d(
                    "SafeSense/Shake",
                    "Shake gesture confirmed — $shakeCount shakes in ${timestamp - firstShakeTime}ms"
                )
                reset()
                return DetectionResult.Detected(ConfidenceLevel.HIGH)
            }
        } else {
            // Window expired before reaching required count — start fresh
            firstShakeTime = timestamp
            shakeCount = 1
        }

        return DetectionResult.NotDetected
    }

    fun reset() {
        shakeCount        = 0
        firstShakeTime    = 0L
        lastShakeTime     = 0L
        previousMagnitude = 0.0f
        previousTimestamp = 0L
    }
}