package com.example.safesense.sensor.processor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// AccelerometerProcessor.kt
// Location: sensor/processor/AccelerometerProcessor.kt
//
// WHAT THIS DOES:
//   Reads raw accelerometer data, calculates vector magnitude (total G-force),
//   and switches between slow (5Hz) and fast (50Hz) sampling based on motion.
//   Emits AccelerometerEvent objects on a SharedFlow for the FusionEngine.
//
// TIERED SAMPLING:
//   SLOW  (SENSOR_DELAY_NORMAL ≈ 5Hz):  Used during idle/calm periods.
//   Saves battery. The phone is just sitting there.
//
//   FAST  (SENSOR_DELAY_FASTEST ≈ 50Hz): Used when unusual motion is detected.
//   Needed to capture the full shape of a fall/impact signature.
//
// HYSTERESIS:
//   When switching from FAST back to SLOW, we don't switch immediately on the
//   first calm reading. We require 3 continuous seconds of calm readings first.
//   This prevents rapid toggling if the readings are bouncing on the threshold.
//
// TIMESTAMP FIX:
//   Uses event.timestamp (sensor hardware clock in nanoseconds) instead of
//   System.currentTimeMillis() (wall clock). This MUST match ProximityProcessor's
//   clock source, or the FusionEngine's 500ms correlation window will reject
//   valid signal pairs as "too far apart in time."
// ─────────────────────────────────────────────────────────────────────────────

class AccelerometerProcessor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _accelerometerEvents = MutableSharedFlow<AccelerometerEvent>(extraBufferCapacity = 64)
    val accelerometerEvents: SharedFlow<AccelerometerEvent> = _accelerometerEvents.asSharedFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var isHighFrequency = false

    // Tracks the moment the sensor first saw a "calm" reading while in
    // high-frequency mode. We only switch back to slow mode after this has
    // been calm for HYSTERESIS_DURATION_MS (3 seconds) continuously.
    // null means we are NOT currently in a calm-down period.
    private var calmSinceTimestamp: Long? = null

    companion object {
        // Use SensorManager constants instead of raw microsecond values.
        //
        // SENSOR_DELAY_NORMAL  ≈ 200ms between readings = ~5Hz
        //   → Good for idle monitoring. Very battery-friendly.
        //
        // SENSOR_DELAY_FASTEST ≈ 20ms between readings = ~50Hz
        //   → Maximum speed the hardware can deliver. Used during potential fall/impact.
        private const val SLOW_SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL
        private const val FAST_SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST

        // How long (in milliseconds) the sensor must stay calm before we
        // allow the switch back to slow mode. Prevents rapid toggling.
        private const val HYSTERESIS_DURATION_MS = 3_000L
    }

    fun start() {
        if (_isActive.value) return
        sensorManager.registerListener(this, sensor, SLOW_SAMPLING_RATE)
        _isActive.value = true
    }

    fun stop() {
        if (!_isActive.value) return
        sensorManager.unregisterListener(this)
        _isActive.value = false
        isHighFrequency = false
        calmSinceTimestamp = null  // Reset hysteresis state on stop
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // ── FIX: Use sensor hardware timestamp, not wall clock ──
        // event.timestamp is in nanoseconds from the sensor hardware clock.
        // Divide by 1_000_000 to get milliseconds.
        // MUST match ProximityProcessor's clock source or the FusionEngine's
        // 500ms correlation window will reject valid signal pairs.
        val timestamp = event.timestamp / 1_000_000L

        val accelEvent = when {
            // Free-fall signature: magnitude drops very low (< 4 m/s²)
            magnitude < 4.0f -> {
                calmSinceTimestamp = null  // This is NOT calm — it is a fall signal
                if (!isHighFrequency) switchToHighFrequency()
                AccelerometerEvent.FallSignature(magnitude, timestamp)
            }

            // Impact / significant movement: magnitude spikes high (> 15 m/s²)
            magnitude > 15.0f -> {
                calmSinceTimestamp = null  // This is NOT calm — it is a movement signal
                if (!isHighFrequency) switchToHighFrequency()
                AccelerometerEvent.SignificantMovement(magnitude, timestamp)
            }

            // Everything else is "normal" range
            else -> {
                // Only consider switching back to slow mode if we are
                // currently in high-frequency mode.
                if (isHighFrequency) {
                    // Start the calm timer the first time we see a normal reading
                    if (calmSinceTimestamp == null) {
                        calmSinceTimestamp = timestamp
                    }

                    // Check if we have been calm for the full 3-second window
                    val calmDuration = timestamp - (calmSinceTimestamp ?: timestamp)
                    if (calmDuration >= HYSTERESIS_DURATION_MS) {
                        switchToNormalFrequency()
                        calmSinceTimestamp = null  // Reset after the switch
                    }
                    // If calm duration has NOT reached 3 seconds yet, we do nothing —
                    // we stay in high-frequency mode and wait.
                }

                AccelerometerEvent.Normal(magnitude, timestamp)
            }
        }

        _accelerometerEvents.tryEmit(accelEvent)
    }

    private fun switchToHighFrequency() {
        if (isHighFrequency) return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, sensor, FAST_SAMPLING_RATE)
        isHighFrequency = true
    }

    private fun switchToNormalFrequency() {
        if (!isHighFrequency) return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, sensor, SLOW_SAMPLING_RATE)
        isHighFrequency = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for SafeSense
    }
}

sealed class AccelerometerEvent {
    abstract val magnitude: Float
    abstract val timestamp: Long

    data class Normal(override val magnitude: Float, override val timestamp: Long)              : AccelerometerEvent()
    data class SignificantMovement(override val magnitude: Float, override val timestamp: Long) : AccelerometerEvent()
    data class FallSignature(override val magnitude: Float, override val timestamp: Long)       : AccelerometerEvent()
}