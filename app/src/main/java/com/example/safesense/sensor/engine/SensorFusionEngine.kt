package com.example.safesense.sensor.engine

import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.domain.usecase.DetectFallUseCase
import com.example.safesense.sensor.processor.AccelerometerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SensorFusionEngine.kt
// Location: sensor/engine/SensorFusionEngine.kt
//
// STEP 7 CHANGE vs Step 6:
//   Before: onAccelerometerEvent() checked if a FallSignature event arrived
//   recently and declared an incident immediately. This was a placeholder —
//   it would have produced massive false positives on any bumpy road.
//
//   Now: every AccelerometerEvent is fed into DetectFallUseCase.process().
//   The use case runs the full three-phase algorithm (free fall → impact →
//   stillness) and only returns Detected when all three phases complete in
//   sequence. The confidence level from the use case flows into the incident.
//
//   The proximity correlation window is preserved — it upgrades confidence
//   when proximity also changed near the time of the fall. This is the
//   SensorFusionEngine's job: correlate signals from multiple sensors.
//
// WHY DetectFallUseCase IS INJECTED, NOT CONSTRUCTED HERE:
//   SensorFusionEngine lives in the sensor layer. DetectFallUseCase lives in
//   the domain layer. The domain layer never imports from the sensor layer.
//   We pass the use case in through the constructor so that in tests we can
//   replace it with a fake — without this, fall detection is untestable.
// ─────────────────────────────────────────────────────────────────────────────

class SensorFusionEngine @Inject constructor(
    private val scope: CoroutineScope,
    private val detectFallUseCase: DetectFallUseCase
) {

    companion object {
        // How close in time two sensor signals must be to count as one event.
        // 500ms: if proximity changed within 500ms of a confirmed fall,
        // the phone likely hit a surface face-down — higher confidence.
        const val CORRELATION_WINDOW_MS = 500L
    }

    private val _incidents = MutableSharedFlow<DetectedIncident>(extraBufferCapacity = 16)
    val incidents: SharedFlow<DetectedIncident> = _incidents.asSharedFlow()

    // Tracks when proximity last changed so we can correlate it with falls.
    // Proximity alone never triggers an incident — it only modifies confidence.
    private var lastProximityChangeTime: Long = 0L

    // ── Accelerometer ─────────────────────────────────────────────────────────
    //
    // Every event — Normal, SignificantMovement, and FallSignature — is passed
    // to DetectFallUseCase. The use case needs ALL of them because:
    //   - Normal events are how it measures stillness in Phase 3
    //   - SignificantMovement events are how it detects the impact in Phase 2
    //   - FallSignature events are how it detects free fall in Phase 1
    // Filtering here would break the state machine inside the use case.

    fun onAccelerometerEvent(event: AccelerometerEvent) {
        val result = detectFallUseCase.process(event)

        if (result is DetectionResult.Detected) {
            // The three-phase algorithm confirmed a fall.
            // Now check if proximity also changed recently.
            // If it did, we upgrade to HIGH confidence regardless of what the
            // use case returned — two independent sensors agreeing is strong signal.
            val proximityCorroborates = (event.timestamp - lastProximityChangeTime) < CORRELATION_WINDOW_MS

            val finalConfidence = when {
                proximityCorroborates                        -> ConfidenceLevel.HIGH
                result.confidence == ConfidenceLevel.HIGH   -> ConfidenceLevel.HIGH
                result.confidence == ConfidenceLevel.MEDIUM -> ConfidenceLevel.MEDIUM
                else                                         -> ConfidenceLevel.LOW
            }

            declareIncident(
                type       = IncidentType.FALL,
                confidence = finalConfidence,
                timestamp  = event.timestamp
            )
        }
    }

    // ── Proximity ─────────────────────────────────────────────────────────────
    //
    // ProximityProcessor calls this when the sensor state changes.
    // We only record the timestamp — the correlation check happens in
    // onAccelerometerEvent() when a fall is confirmed.

    fun onProximityChanged(timestamp: Long) {
        lastProximityChangeTime = timestamp
    }

    // ── Declare ───────────────────────────────────────────────────────────────

    private fun declareIncident(
        type: IncidentType,
        confidence: ConfidenceLevel,
        timestamp: Long
    ) {
        scope.launch {
            _incidents.emit(
                DetectedIncident(
                    type            = type,
                    confidenceLevel = confidence,
                    timestamp       = timestamp
                )
            )
        }
    }
}

// ── Supporting types ──────────────────────────────────────────────────────────
//
// DetectedIncident uses ConfidenceLevel (the domain enum) instead of a raw
// Float. This makes the confidence value meaningful and prevents bugs where
// 0.9f and 0.91f would be treated differently by downstream logic.

data class DetectedIncident(
    val type: IncidentType,
    val confidenceLevel: ConfidenceLevel,
    val timestamp: Long
)

enum class IncidentType {
    FALL,
    COLLISION,
    SHAKE_GESTURE
}