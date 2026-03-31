package com.example.safesense.sensor.processor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// ─────────────────────────────────────────────────────────────────────────────
// ProximityProcessor.kt
// Location: sensor/processor/ProximityProcessor.kt
//
// WHAT THIS DOES:
//   Monitors the proximity sensor for NEAR/FAR state changes. Emits a
//   ProximityEvent only when the state changes (not on every reading).
//   The FusionEngine uses NEAR→FAR transitions to detect "snatched" events
//   and to upgrade fall confidence from MEDIUM to HIGH.
//
// AUDIT FIX #2 — Binary Sensor Incompatibility:
//   OLD: Used hardcoded NEAR_THRESHOLD = 1.0f
//   NEW: Uses sensor.maximumRange read at runtime
//   WHY: Most Android proximity sensors are binary. They report 0.0 (NEAR)
//   or their maximumRange (FAR, often 5.0 or 8.0). A hardcoded threshold
//   breaks on devices where the FAR value differs from 1.0. By reading
//   the actual maximumRange from the hardware, we correctly handle ALL devices:
//     - Device reporting 0.0 / 5.0 with maxRange=5.0 → 0.0 < 5.0 = NEAR ✓
//     - Device reporting 0.0 / 8.0 with maxRange=8.0 → 0.0 < 8.0 = NEAR ✓
//     - Device reporting 0.0 / 5.0 with maxRange=5.0 → 5.0 < 5.0 = false = FAR ✓
//
// AUDIT FIX #3 — Timing Drift in Fusion Engine:
//   OLD: Used System.currentTimeMillis() for event timestamps
//   NEW: Uses event.timestamp / 1_000_000L
//   WHY: The FusionEngine's 500ms correlation window compares timestamps across
//   sensors. System.currentTimeMillis() is the wall clock. event.timestamp is
//   the sensor hardware clock. These can drift apart, causing valid signal pairs
//   to be rejected as "too far apart." All sensor processors MUST use the same
//   clock source (sensor hardware) for the fusion window to work correctly.
// ─────────────────────────────────────────────────────────────────────────────

class ProximityProcessor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    companion object {
        private const val TAG = "ProximityProcessor"
    }

    private val _proximityEvents = MutableSharedFlow<ProximityEvent>(extraBufferCapacity = 32)
    val proximityEvents: SharedFlow<ProximityEvent> = _proximityEvents.asSharedFlow()

    private var isRegistered = false
    private var lastState: ProximityState = ProximityState.UNKNOWN

    // ── FIX #2: Dynamic threshold from hardware instead of hardcoded 1.0f ──
    // Binary sensors report 0.0 for NEAR and maximumRange for FAR.
    // By using maximumRange as the threshold:
    //   distance < maximumRange  →  NEAR  (0.0 is always less than maxRange)
    //   distance >= maximumRange →  FAR   (sensor reports its maxRange value)
    // Initialized to MAX_VALUE as a safe fallback if sensor is not found.
    private var farThreshold: Float = Float.MAX_VALUE

    fun start() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: run {
            Log.w(TAG, "No proximity sensor available on this device")
            return
        }

        // ── Read the actual maximumRange from the hardware ──
        farThreshold = sensor.maximumRange
        Log.d(TAG, "Proximity sensor maxRange=$farThreshold — using as FAR threshold")

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        isRegistered = true
    }

    fun stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
            lastState = ProximityState.UNKNOWN
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val distance = event.values[0]

        // ── FIX #2: Compare against hardware maximumRange ──
        val newState = if (distance < farThreshold) ProximityState.NEAR else ProximityState.FAR

        // Only emit on state change — not on every reading
        if (newState != lastState) {
            lastState = newState

            // ── FIX #3: Use sensor hardware timestamp, not wall clock ──
            // event.timestamp is in nanoseconds. Divide by 1_000_000 to get milliseconds.
            // MUST match AccelerometerProcessor's clock source.
            val timestampMs = event.timestamp / 1_000_000L

            val proximityEvent = ProximityEvent(
                state = newState,
                timestamp = timestampMs
            )
            _proximityEvents.tryEmit(proximityEvent)
            Log.d(TAG, "Proximity state change: $newState at ${timestampMs}ms")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for SafeSense
    }
}

data class ProximityEvent(
    val state: ProximityState,
    val timestamp: Long  // Milliseconds from sensor hardware clock
)

enum class ProximityState {
    NEAR,
    FAR,
    UNKNOWN
}