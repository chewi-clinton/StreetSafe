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

/**
 * AccelerometerProcessor handles the raw data from the phone's accelerometer.
 * It calculates the vector magnitude (total G-force) and decides when to
 * switch between low-frequency and high-frequency sampling.
 */
class AccelerometerProcessor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _accelerometerEvents = MutableSharedFlow<AccelerometerEvent>(extraBufferCapacity = 64)
    val accelerometerEvents: SharedFlow<AccelerometerEvent> = _accelerometerEvents.asSharedFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var isHighFrequency = false

    // FIX 3: Tracks the moment the sensor first saw a "calm" reading while in
    // high-frequency mode. We only switch back to slow mode after this has
    // been calm for HYSTERESIS_DURATION_MS (3 seconds) continuously.
    // null means we are NOT currently in a calm-down period.
    private var calmSinceTimestamp: Long? = null

    companion object {
        // FIX 1 & 2: Use SensorManager constants instead of raw microsecond values.
        //
        // SENSOR_DELAY_NORMAL  ≈ 200ms between readings = ~5Hz
        //   → Good for idle monitoring. Very battery-friendly.
        //   → The OLD value (60_000 µs = 60ms ≈ 17Hz) was much faster than needed
        //     and wasted battery during normal carry.
        //
        // SENSOR_DELAY_FASTEST ≈ 20ms between readings = ~50Hz
        //   → Maximum speed the hardware can deliver. Used during potential fall/impact.
        //   → The OLD value (10_000 µs = 10ms = 100Hz) asked for more than the
        //     hardware can actually deliver and is not a standard constant.
        private const val SLOW_SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL
        private const val FAST_SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST

        // FIX 3: How long (in milliseconds) the sensor must stay calm before we
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
        val timestamp = System.currentTimeMillis()

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
                // FIX 3: Only consider switching back to slow mode if we are
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