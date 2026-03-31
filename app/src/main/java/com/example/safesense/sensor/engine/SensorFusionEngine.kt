package com.example.safesense.sensor.fusion

import android.location.Location
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.DetectedIncident
import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.usecase.DetectCollisionUseCase
import com.example.safesense.domain.usecase.DetectFallUseCase
import com.example.safesense.sensor.processor.AccelerometerEvent
import com.example.safesense.sensor.processor.GPSTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SensorFusionEngine(
    private val scope: CoroutineScope,
    private val detectFallUseCase: DetectFallUseCase,
    private val detectCollisionUseCase: DetectCollisionUseCase = DetectCollisionUseCase(),
    private val gpsTracker: GPSTracker
) {

    companion object {
        const val CORRELATION_WINDOW_MS = 500L
        const val GPS_SPEED_UPGRADE_THRESHOLD_M_PER_S = 2.778f
    }

    private val _incidents = MutableSharedFlow<DetectedIncident>(extraBufferCapacity = 1)
    val incidents: SharedFlow<DetectedIncident> = _incidents.asSharedFlow()

    @Volatile private var lastProximityTimestamp    = 0L
    @Volatile private var lastDistressSoundTimestamp = 0L
    private var proximityIsNear = false

    fun onAccelerometerEvent(event: AccelerometerEvent) {
        val fallResult      = detectFallUseCase.process(event)
        val collisionResult = detectCollisionUseCase.process(event)

        when {
            fallResult is DetectionResult.Detected -> {
                handleFallDetected(fallResult.confidence, event.timestamp)
            }
            collisionResult is DetectionResult.Detected -> {
                handleCollisionDetected(collisionResult.confidence, event.timestamp)
            }
        }
    }

    fun onProximityChanged(timestamp: Long, isNear: Boolean) {
        lastProximityTimestamp = timestamp
        proximityIsNear = isNear
        android.util.Log.d("SafeSense/Fusion", "Proximity change: isNear=$isNear at $timestamp")

        if (!isNear) checkForSnatchedPattern(timestamp)
    }

    fun onDistressSoundDetected(timestamp: Long) {
        lastDistressSoundTimestamp = timestamp
        android.util.Log.d("SafeSense/Fusion", "Distress sound recorded at $timestamp")
        checkForSnatchedPattern(timestamp)
    }

    /**
     * Called when a shake gesture is detected.
     * 
     * FIX: We no longer require proximityIsNear = true.
     * The RecognizeShakeGestureUseCase already ensures the shake is intentional.
     * Requiring proximity = NEAR would block alerts when the phone is shaken
     * in the open air (in hand), which is the most common use case for 
     * "Shake to Alert".
     */
    fun onShakeEvent(timestamp: Long) {
        android.util.Log.d("SafeSense/Fusion", "Shake event received, proximityNear=$proximityIsNear")
        
        // We trigger the alert regardless of proximity state.
        // If proximity is NEAR, it's definitely a HIGH confidence hand/pocket shake.
        // If proximity is FAR, it's still a valid shake gesture detection.
        val confidence = if (proximityIsNear) ConfidenceLevel.HIGH else ConfidenceLevel.HIGH
        
        android.util.Log.d("SafeSense/Fusion", "Row 5: Shake detected → Triggering HIGH confidence alert")
        emitIncident(IncidentType.SHAKE, confidence, timestamp)
    }

    private fun handleFallDetected(baseConfidence: ConfidenceLevel, detectionTimestamp: Long) {
        val proximityCorroborated = isWithinWindow(lastProximityTimestamp, detectionTimestamp)
        val finalConfidence = if (proximityCorroborated) ConfidenceLevel.HIGH else baseConfidence
        emitIncident(IncidentType.FALL, finalConfidence, detectionTimestamp)
    }

    private fun handleCollisionDetected(baseConfidence: ConfidenceLevel, detectionTimestamp: Long) {
        val audioCorroborated = isWithinWindow(lastDistressSoundTimestamp, detectionTimestamp)
        val gpsSpeedCorroborated = isGpsSpeedAboveThreshold()

        val finalConfidence = when {
            audioCorroborated || gpsSpeedCorroborated -> ConfidenceLevel.HIGH
            else -> baseConfidence
        }

        emitIncident(IncidentType.COLLISION, finalConfidence, detectionTimestamp)
    }

    private fun checkForSnatchedPattern(currentTimestamp: Long) {
        if (lastProximityTimestamp == 0L || lastDistressSoundTimestamp == 0L) return
        val timeBetweenSignals = Math.abs(lastProximityTimestamp - lastDistressSoundTimestamp)
        if (timeBetweenSignals <= CORRELATION_WINDOW_MS) {
            lastProximityTimestamp    = 0L
            lastDistressSoundTimestamp = 0L
            emitIncident(IncidentType.COLLISION, ConfidenceLevel.MEDIUM, currentTimestamp)
        }
    }

    private fun isWithinWindow(signalTimestamp: Long, detectionTimestamp: Long): Boolean {
        if (signalTimestamp == 0L) return false
        return Math.abs(detectionTimestamp - signalTimestamp) <= CORRELATION_WINDOW_MS
    }

    private fun isGpsSpeedAboveThreshold(): Boolean {
        val location: Location? = gpsTracker.lastLocation.value
        if (location == null) return false
        return location.speed >= GPS_SPEED_UPGRADE_THRESHOLD_M_PER_S
    }

    private fun emitIncident(
        type: IncidentType,
        confidence: ConfidenceLevel,
        timestamp: Long
    ) {
        val incident = DetectedIncident(
            type            = type,
            confidenceLevel = confidence,
            timestamp       = timestamp
        )
        scope.launch {
            _incidents.emit(incident)
        }
    }
}
