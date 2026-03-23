package com.example.safesense.sensor.processor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

class AccelerometerProcessor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    companion object {
        const val SLOW_SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL   // 5Hz
        const val FAST_SAMPLING_RATE = SensorManager.SENSOR_DELAY_GAME     // 50Hz
        const val MOVEMENT_THRESHOLD = 12.0f  // m/s² — above this we escalate to fast sampling
        const val FALL_THRESHOLD = 25.0f      // m/s² — free fall + impact signature
    }

    private val _accelerometerEvents = MutableSharedFlow<AccelerometerEvent>(extraBufferCapacity = 64)
    val accelerometerEvents: SharedFlow<AccelerometerEvent> = _accelerometerEvents.asSharedFlow()

    private var isRegistered = false
    private var isHighFrequency = false

    fun start() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.registerListener(this, sensor, SLOW_SAMPLING_RATE)
        isRegistered = true
        isHighFrequency = false
    }

    fun stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
            isHighFrequency = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        // Tiered sampling — escalate to high frequency on movement
        if (magnitude > MOVEMENT_THRESHOLD && !isHighFrequency) {
            escalateToHighFrequency()
        } else if (magnitude <= MOVEMENT_THRESHOLD && isHighFrequency) {
            downgradeToLowFrequency()
        }

        val accelerometerEvent = when {
            magnitude > FALL_THRESHOLD -> AccelerometerEvent.FallSignature(magnitude, System.currentTimeMillis())
            magnitude > MOVEMENT_THRESHOLD -> AccelerometerEvent.SignificantMovement(magnitude, System.currentTimeMillis())
            else -> AccelerometerEvent.Normal(magnitude, System.currentTimeMillis())
        }

        _accelerometerEvents.tryEmit(accelerometerEvent)
    }

    private fun escalateToHighFrequency() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, sensor, FAST_SAMPLING_RATE)
        isHighFrequency = true
    }

    private fun downgradeToLowFrequency() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, sensor, SLOW_SAMPLING_RATE)
        isHighFrequency = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for SafeSense
    }
}

sealed class AccelerometerEvent {
    data class Normal(val magnitude: Float, val timestamp: Long) : AccelerometerEvent()
    data class SignificantMovement(val magnitude: Float, val timestamp: Long) : AccelerometerEvent()
    data class FallSignature(val magnitude: Float, val timestamp: Long) : AccelerometerEvent()
}