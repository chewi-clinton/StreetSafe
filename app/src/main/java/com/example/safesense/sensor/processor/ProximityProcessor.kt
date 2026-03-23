package com.example.safesense.sensor.processor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ProximityProcessor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    companion object {
        const val NEAR_THRESHOLD = 1.0f  // centimeters — anything below this is "near"
    }

    private val _proximityEvents = MutableSharedFlow<ProximityEvent>(extraBufferCapacity = 32)
    val proximityEvents: SharedFlow<ProximityEvent> = _proximityEvents.asSharedFlow()

    private var isRegistered = false
    private var lastState: ProximityState = ProximityState.UNKNOWN

    fun start() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return
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
        val newState = if (distance < NEAR_THRESHOLD) ProximityState.NEAR else ProximityState.FAR

        // Only emit on state change — not on every reading
        if (newState != lastState) {
            lastState = newState
            val proximityEvent = ProximityEvent(
                state = newState,
                timestamp = System.currentTimeMillis()
            )
            _proximityEvents.tryEmit(proximityEvent)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for SafeSense
    }
}

data class ProximityEvent(
    val state: ProximityState,
    val timestamp: Long
)

enum class ProximityState {
    NEAR,
    FAR,
    UNKNOWN
}