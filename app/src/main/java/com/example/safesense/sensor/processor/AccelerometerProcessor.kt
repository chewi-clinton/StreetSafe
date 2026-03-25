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

    companion object {
        // Sampling rates in microseconds
        // Normal: ~50Hz (20ms) - Good for battery while walking
        // High: ~100Hz (10ms) - Needed for accurate impact detection
        private const val SLOW_SAMPLING_RATE   = 60_000
        private const val FAST_SAMPLING_RATE   = 10_000
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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate magnitude: sqrt(x^2 + y^2 + z^2)
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val timestamp = System.currentTimeMillis()

        val accelEvent = when {
            // Free-fall threshold (usually < 3.0 m/s^2)
            magnitude < 4.0f -> {
                if (!isHighFrequency) switchToHighFrequency()
                AccelerometerEvent.FallSignature(magnitude, timestamp)
            }

            // Significant movement (potential start of a fall or impact)
            magnitude > 15.0f -> {
                if (!isHighFrequency) switchToHighFrequency()
                AccelerometerEvent.SignificantMovement(magnitude, timestamp)
            }

            // Normal movement
            else -> {
                // If we were in high frequency but things calmed down, switch back
                if (isHighFrequency && magnitude > 7.0f && magnitude < 12.0f) {
                    switchToNormalFrequency()
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

    data class Normal(override val magnitude: Float, override val timestamp: Long)             : AccelerometerEvent()
    data class SignificantMovement(override val magnitude: Float, override val timestamp: Long) : AccelerometerEvent()
    data class FallSignature(override val magnitude: Float, override val timestamp: Long)       : AccelerometerEvent()
}
