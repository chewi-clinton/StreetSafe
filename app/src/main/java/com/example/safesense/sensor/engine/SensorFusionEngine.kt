package com.example.safesense.sensor.engine

import com.example.safesense.sensor.processor.AccelerometerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SensorFusionEngine(
    private val scope: CoroutineScope
) {

    companion object {
        const val CORRELATION_WINDOW_MS = 500L  // signals must arrive within 500ms
        const val HIGH_CONFIDENCE = 0.9f
        const val MEDIUM_CONFIDENCE = 0.6f
    }

    private val _incidents = MutableSharedFlow<DetectedIncident>(extraBufferCapacity = 16)
    val incidents: SharedFlow<DetectedIncident> = _incidents.asSharedFlow()

    private var lastFallSignatureTime: Long = 0L
    private var lastProximityChangeTime: Long = 0L

    // Called by AccelerometerProcessor events
    fun onAccelerometerEvent(event: AccelerometerEvent) {
        when (event) {
            is AccelerometerEvent.FallSignature -> {
                lastFallSignatureTime = event.timestamp
                evaluateIncident(event.timestamp)
            }
            is AccelerometerEvent.SignificantMovement -> {
                // Significant movement alone is not an incident
                // but resets correlation window consideration
            }
            is AccelerometerEvent.Normal -> {
                // No action needed
            }
        }
    }

    // Called by ProximityProcessor (wired in next step)
    fun onProximityChanged(timestamp: Long) {
        lastProximityChangeTime = timestamp
        evaluateIncident(timestamp)
    }

    private fun evaluateIncident(currentTime: Long) {
        val fallRecent = (currentTime - lastFallSignatureTime) < CORRELATION_WINDOW_MS
        val proximityRecent = (currentTime - lastProximityChangeTime) < CORRELATION_WINDOW_MS

        when {
            fallRecent && proximityRecent -> declareIncident(
                type = IncidentType.FALL,
                confidence = HIGH_CONFIDENCE
            )
            fallRecent && !proximityRecent -> declareIncident(
                type = IncidentType.FALL,
                confidence = MEDIUM_CONFIDENCE
            )
            // Proximity alone is not enough to declare an incident
        }
    }

    private fun declareIncident(type: IncidentType, confidence: Float) {
        // Reset timestamps to prevent duplicate incidents
        lastFallSignatureTime = 0L
        lastProximityChangeTime = 0L

        scope.launch {
            _incidents.emit(
                DetectedIncident(
                    type = type,
                    confidenceLevel = confidence,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}

data class DetectedIncident(
    val type: IncidentType,
    val confidenceLevel: Float,
    val timestamp: Long
)

enum class IncidentType {
    FALL,
    COLLISION,
    SHAKE_GESTURE
}