package com.example.safesense.domain.usecase

import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.sensor.processor.AccelerometerEvent
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RecognizeShakeGestureUseCase.kt
// Location: domain/usecase/RecognizeShakeGestureUseCase.kt
//
// WHAT THIS DETECTS:
//   3 or more rapid shakes within 2 seconds.
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
//   JERK_THRESHOLD   = 30 m/s³  — below this is normal movement
//   SHAKE_WINDOW_MS  = 2000ms   — all 3 shakes must happen within 2 seconds
//   MIN_SHAKE_COUNT  = 3        — fewer than 3 = not intentional
//   DEBOUNCE_MS      = 300ms    — minimum gap between counted shakes
//                                 prevents one long shake counting as many
// ─────────────────────────────────────────────────────────────────────────────

class RecognizeShakeGestureUseCase @Inject constructor() {

    companion object {
        const val JERK_THRESHOLD_MS_PER_S3 = 30.0f  // m/s³ — above this = shake
        const val SHAKE_WINDOW_MS          = 2000L   // all shakes must fit in this window
        const val MIN_SHAKE_COUNT          = 3       // need at least this many
        const val DEBOUNCE_MS              = 300L    // minimum gap between shakes
    }

    // Previous event — needed to calculate jerk (change between two readings)
    private var previousMagnitude  = 0.0f
    private var previousTimestamp  = 0L

    // Shake counting
    private var shakeCount         = 0
    private var firstShakeTime     = 0L
    private var lastShakeTime      = 0L

    // ── Main entry point ──────────────────────────────────────────────────────
    //
    // Called by SensorFusionEngine with every AccelerometerEvent.
    // Returns DetectionResult.Detected when 3 shakes happen within 2 seconds.
    // Returns DetectionResult.NotDetected otherwise.

    fun process(event: AccelerometerEvent): DetectionResult {
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
        } else if (timestamp - firstShakeTime <= SHAKE_WINDOW_MS) {
            // Within the window — count it
            shakeCount++

            if (shakeCount >= MIN_SHAKE_COUNT) {
                // Gesture confirmed — reset and report
                android.util.Log.d("SafeSense/Shake", "Shake gesture confirmed — $shakeCount shakes in ${timestamp - firstShakeTime}ms")
                reset()
                return DetectionResult.Detected(ConfidenceLevel.HIGH)
            }
        } else {
            // Window expired before reaching 3 — start fresh from this shake
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