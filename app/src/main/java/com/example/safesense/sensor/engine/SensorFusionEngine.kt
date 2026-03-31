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

// ─────────────────────────────────────────────────────────────────────────────
// SensorFusionEngine.kt
// Location: sensor/fusion/SensorFusionEngine.kt
//
// THIS IS THE ONLY COMPONENT THAT CAN DECLARE AN INCIDENT.
//
// Individual sensor processors (AccelerometerProcessor, ProximityProcessor,
// AudioMonitor) are "witnesses" — they report raw readings. This class is
// the "judge" — it decides whether those readings together mean an emergency.
//
// HOW IT WORKS:
//   1. SensorForegroundService feeds events in by calling onAccelerometerEvent(),
//      onProximityChanged(), and onDistressSoundDetected() directly.
//   2. This engine records the timestamp of each signal.
//   3. When the accelerometer use case says "detection!", the engine checks
//      whether other signals arrived within the 500ms correlation window.
//   4. The combination of signals determines which row of the decision matrix
//      fires, and at what confidence level.
//   5. A DetectedIncident is emitted on the `incidents` SharedFlow.
//   6. CountdownViewModel subscribes to `incidents` and starts the countdown.
//
// THE 500ms CORRELATION WINDOW:
//   If proximity changes at t=0 and the accelerometer spike arrives at t=600ms,
//   we do NOT combine them — 600ms is outside the window.
//   Why 500ms? Research on fall and collision biomechanics shows that all
//   body sensor signals from a single event arrive within ~300ms. We use 500ms
//   to account for sensor polling jitter on low-end Android hardware.
//
// THE FIVE DECISION MATRIX ROWS:
//
//   Row 1: Accelerometer fall algorithm fires
//          No proximity change within 500ms, no audio within 500ms
//          → Fall, MEDIUM confidence
//
//   Row 2: Accelerometer fall algorithm fires
//          Proximity changed NEAR→FAR within 500ms (phone left body at impact)
//          → Fall, HIGH confidence
//
//   Row 3: Collision algorithm fires (>4g spike + stillness)
//          → Collision, MEDIUM confidence (base)
//          Upgrade to HIGH if EITHER:
//            - GPS speed > 10 km/h (vehicle was moving — not a dropped phone)
//            - Distress sound within 500ms (crash sound detected)
//
//   Row 4: No accelerometer event
//          Proximity changed NEAR→FAR, and audio loud within 500ms
//          → Snatched, MEDIUM confidence
//
//   Row 5: Shake gesture (≥3 shakes in 2s) from RecognizeShakeGestureUseCase
//          Proximity is NEAR (phone in hand)
//          → Shake Alert, HIGH confidence
//
// AUDIT FIX — COLLISION CONFIDENCE:
//   OLD: Collision always returned HIGH regardless of corroboration.
//   PROBLEM: A phone dropped on a hard surface inside a stationary vehicle
//       could produce a >4g spike + stillness. Without GPS speed or audio
//       corroboration, this would be a false HIGH-confidence collision alert.
//   FIX: DetectCollisionUseCase now returns MEDIUM. This engine upgrades to
//       HIGH only when GPS speed > 10 km/h or audio corroborates.
//       MEDIUM confidence STILL triggers the countdown — user is protected.
//
// AUDIT FIX — GPS SPEED CORROBORATION:
//   OLD: GPS speed was not checked for collision confidence.
//   FIX: GPSTracker is now injected. When collision is detected at MEDIUM,
//       we check lastLocation.speed. If > 10 km/h (2.78 m/s), we upgrade
//       to HIGH because the phone was in a moving vehicle.
//
// ─────────────────────────────────────────────────────────────────────────────

class SensorFusionEngine(
    private val scope: CoroutineScope,
    private val detectFallUseCase: DetectFallUseCase,
    private val detectCollisionUseCase: DetectCollisionUseCase = DetectCollisionUseCase(),
    private val gpsTracker: GPSTracker  // NEW: for GPS speed corroboration on collisions
) {

    companion object {
        // The 500ms correlation window — signals outside this are never combined
        const val CORRELATION_WINDOW_MS = 500L

        // GPS speed threshold for upgrading collision confidence to HIGH
        // 10 km/h = 2.778 m/s (Android Location.speed is in m/s)
        // Below this, the phone could be stationary — a dropped phone, not a crash.
        // Above this, the phone was in a moving vehicle — consistent with collision.
        const val GPS_SPEED_UPGRADE_THRESHOLD_M_PER_S = 2.778f
    }

    // ── Output: the incidents SharedFlow ─────────────────────────────────────
    //
    // CountdownViewModel subscribes to this.
    // Every emission starts a countdown.
    // extraBufferCapacity = 1: if CountdownViewModel hasn't collected the last
    // incident yet, we hold one in the buffer. We never drop an incident.

    private val _incidents = MutableSharedFlow<DetectedIncident>(extraBufferCapacity = 1)
    val incidents: SharedFlow<DetectedIncident> = _incidents.asSharedFlow()

    // ── Correlated signal timestamps ──────────────────────────────────────────
    //
    // Each time a corroborating signal arrives, we record its timestamp.
    // When a use case fires, we check whether these timestamps are within
    // CORRELATION_WINDOW_MS of the detection timestamp.

    @Volatile private var lastProximityTimestamp    = 0L  // last NEAR→FAR transition
    @Volatile private var lastDistressSoundTimestamp = 0L  // last audio spike above threshold

    // ── Proximity state for shake gesture (Row 5) ────────────────────────────
    // The RecognizeShakeGestureUseCase handles the actual shake detection.
    // We just need to know if the phone is NEAR (in hand/pocket) to validate
    // the gesture. Proximity NEAR means the phone is against the body.
    private var proximityIsNear = false

    // ── Signal entry points — called by SensorForegroundService ──────────────

    /**
     * Called for every accelerometer reading.
     * We pass it through both use cases.
     * If either fires a detection, we run the decision matrix.
     */
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

    /**
     * Called by SensorForegroundService when ProximityProcessor emits any event.
     * We only care about NEAR→FAR transitions — that means the phone left
     * the user's body (fell out of pocket, got snatched, etc.).
     * The ProximityProcessor already filters to state-change events only,
     * so every call here is a real state change.
     * We record the timestamp for correlation.
     */
    fun onProximityChanged(timestamp: Long, isNear: Boolean) {
        lastProximityTimestamp = timestamp
        proximityIsNear = isNear
        android.util.Log.d("SafeSense/Fusion", "Proximity change: isNear=$isNear at $timestamp")

        // Check row 4: snatched pattern — proximity NEAR→FAR + audio within the window
        if (!isNear) checkForSnatchedPattern(timestamp)
    }

    /**
     * Called by SensorForegroundService when AudioMonitor emits a DistressSound.
     * We record the timestamp for correlation with accelerometer events.
     */
    fun onDistressSoundDetected(timestamp: Long) {
        lastDistressSoundTimestamp = timestamp
        android.util.Log.d("SafeSense/Fusion", "Distress sound recorded at $timestamp")

        // Check row 4: snatched pattern — audio + proximity within the window
        checkForSnatchedPattern(timestamp)
    }

    /**
     * Called by SensorForegroundService when RecognizeShakeGestureUseCase
     * detects a FULL shake gesture (configured shakes within configured window).
     *
     * The use case handles all the shake detection logic (jerk calculation,
     * counting, timing). We only validate that the phone is in hand (NEAR)
     * to prevent false triggers from a phone bouncing in a bag or vehicle.
     */
    fun onShakeEvent(timestamp: Long) {
        android.util.Log.d("SafeSense/Fusion", "Shake event received, proximityNear=$proximityIsNear")

        if (proximityIsNear) {
            android.util.Log.d("SafeSense/Fusion", "Row 5: Shake + phone NEAR → SHAKE HIGH")
            emitIncident(IncidentType.SHAKE, ConfidenceLevel.HIGH, timestamp)
        } else {
            android.util.Log.d("SafeSense/Fusion", "Shake ignored — phone is not NEAR (not in hand/pocket)")
        }
    }

    // ── Decision matrix rows ──────────────────────────────────────────────────

    /**
     * Rows 1 and 2: Fall detection
     *
     * Row 1: Fall alone                → MEDIUM confidence (from use case)
     * Row 2: Fall + proximity change   → upgrade to HIGH confidence
     *
     * The use case already determined the base confidence from stillness duration.
     * Proximity corroboration overrides that to HIGH, because the physical
     * signature (phone left body + fall phases) is very specific.
     */
    private fun handleFallDetected(baseConfidence: ConfidenceLevel, detectionTimestamp: Long) {
        val proximityCorroborated = isWithinWindow(lastProximityTimestamp, detectionTimestamp)

        val finalConfidence = if (proximityCorroborated) {
            // Row 2: fall + phone left body = HIGH confidence
            android.util.Log.d(
                "SafeSense/Fusion",
                "Row 2: Fall + proximity corroborated → HIGH"
            )
            ConfidenceLevel.HIGH
        } else {
            // Row 1: fall alone — use the confidence from the use case
            android.util.Log.d(
                "SafeSense/Fusion",
                "Row 1: Fall alone → $baseConfidence"
            )
            baseConfidence
        }

        emitIncident(IncidentType.FALL, finalConfidence, detectionTimestamp)
    }

    /**
     * Row 3: Collision detection
     *
     * DetectCollisionUseCase returns MEDIUM confidence (accelerometer only).
     * This engine upgrades to HIGH when EITHER corroboration is available:
     *
     *   - GPS speed > 10 km/h: The phone was in a moving vehicle.
     *     A stationary phone dropping on a hard surface can produce >4g,
     *     but a moving vehicle confirms this is a real collision scenario.
     *
     *   - Distress sound within 500ms: Metal-on-metal crash sound,
     *     glass breaking, etc. Corroborates that the impact was violent.
     *
     * WHY MEDIUM AS BASE?
     *   Without corroboration, a >4g spike + stillness could be:
     *   - Phone dropped from a height onto a hard floor
     *   - Phone slammed down on a table
     *   - Phone in a bag that was thrown down
     *   These are NOT collisions but produce similar accelerometer signatures.
     *   MEDIUM confidence still triggers the countdown — user is protected.
     *   HIGH confidence would be inappropriate without corroboration.
     *
     * WHY CHECK GPS SPEED INSTEAD OF JUST GPS FIX?
     *   We don't need coordinates here — we need SPEED. The Location object
     *   includes speed in m/s. If no GPS fix is available, speed is 0.0,
     *   and we don't upgrade (correct behavior — can't confirm vehicle motion).
     */
    private fun handleCollisionDetected(baseConfidence: ConfidenceLevel, detectionTimestamp: Long) {
        val audioCorroborated = isWithinWindow(lastDistressSoundTimestamp, detectionTimestamp)
        val gpsSpeedCorroborated = isGpsSpeedAboveThreshold()

        val finalConfidence = when {
            audioCorroborated && gpsSpeedCorroborated -> {
                // Both corroborations — strongest signal
                android.util.Log.d(
                    "SafeSense/Fusion",
                    "Row 3a: Collision + audio + GPS speed > 10km/h → HIGH"
                )
                ConfidenceLevel.HIGH
            }
            audioCorroborated -> {
                // Audio only — crash sound heard
                android.util.Log.d(
                    "SafeSense/Fusion",
                    "Row 3b: Collision + audio → HIGH"
                )
                ConfidenceLevel.HIGH
            }
            gpsSpeedCorroborated -> {
                // GPS speed only — vehicle was moving
                android.util.Log.d(
                    "SafeSense/Fusion",
                    "Row 3c: Collision + GPS speed > 10km/h → HIGH"
                )
                ConfidenceLevel.HIGH
            }
            else -> {
                // No corroboration — keep the base confidence from use case
                android.util.Log.d(
                    "SafeSense/Fusion",
                    "Row 3d: Collision alone → $baseConfidence"
                )
                baseConfidence
            }
        }

        emitIncident(IncidentType.COLLISION, finalConfidence, detectionTimestamp)
    }

    /**
     * Row 4: Snatched detection
     *
     * This fires when BOTH proximity AND audio arrive within 500ms of each other
     * but with NO accelerometer fall or collision detection.
     * We check this every time either signal arrives, using the other signal's
     * stored timestamp.
     *
     * What "snatched" means physically:
     *   - Phone leaves body (proximity: NEAR→FAR)
     *   - Loud sound (scream, yell, struggle) at the same time
     *   - But no fall (person stayed upright during mugging)
     */
    private fun checkForSnatchedPattern(currentTimestamp: Long) {
        // Need both signals to have timestamps that are within the window of each other
        if (lastProximityTimestamp == 0L || lastDistressSoundTimestamp == 0L) return

        val timeBetweenSignals = Math.abs(lastProximityTimestamp - lastDistressSoundTimestamp)

        if (timeBetweenSignals <= CORRELATION_WINDOW_MS) {
            // Both signals within 500ms of each other = snatched pattern
            android.util.Log.d(
                "SafeSense/Fusion",
                "Row 4: Proximity + audio within ${timeBetweenSignals}ms → SNATCHED MEDIUM"
            )

            // Clear both timestamps so we do not fire this twice for the same event
            lastProximityTimestamp    = 0L
            lastDistressSoundTimestamp = 0L

            emitIncident(IncidentType.COLLISION, ConfidenceLevel.MEDIUM, currentTimestamp)
            // Note: using COLLISION as a proxy for SNATCHED because IncidentType does not
            // have a SNATCHED value yet. When IncidentType is expanded,
            // change this to IncidentType.SNATCHED.
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the corroborating signal's timestamp is within
     * CORRELATION_WINDOW_MS of the detection timestamp.
     *
     * We use absolute difference because sometimes the corroborating signal
     * arrives a few milliseconds BEFORE the use case fires (e.g. proximity
     * changes at impact, accelerometer stillness fires 50ms later).
     */
    private fun isWithinWindow(signalTimestamp: Long, detectionTimestamp: Long): Boolean {
        if (signalTimestamp == 0L) return false
        return Math.abs(detectionTimestamp - signalTimestamp) <= CORRELATION_WINDOW_MS
    }

    /**
     * Checks whether the GPS reports a speed above the collision threshold.
     *
     * Android's Location.speed is in meters per second.
     * We convert the threshold: 10 km/h = 10 / 3.6 = 2.778 m/s.
     *
     * Returns false if:
     *   - No GPS fix is available (lastLocation is null)
     *   - GPS speed is below threshold (phone may be stationary)
     *   - GPS speed is negative (invalid reading from GPS hardware)
     *
     * This is a synchronous read from the in-memory StateFlow cache.
     * It never blocks, never makes a GPS request, never accesses DataStore.
     */
    private fun isGpsSpeedAboveThreshold(): Boolean {
        val location: Location? = gpsTracker.lastLocation.value
        if (location == null) {
            android.util.Log.d("SafeSense/Fusion", "GPS speed check: no location fix available")
            return false
        }

        val speedMps = location.speed
        val speedKmh = speedMps * 3.6f
        val isAboveThreshold = speedMps >= GPS_SPEED_UPGRADE_THRESHOLD_M_PER_S

        android.util.Log.d(
            "SafeSense/Fusion",
            "GPS speed check: ${String.format("%.1f", speedKmh)} km/h " +
                    "(threshold 10.0 km/h) → ${if (isAboveThreshold) "ABOVE" else "BELOW"}"
        )

        return isAboveThreshold
    }

    /**
     * Emits a DetectedIncident on the incidents SharedFlow.
     * This is the only place in the entire app that creates a DetectedIncident.
     * Every call here WILL start a countdown in CountdownViewModel.
     */
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
            android.util.Log.d(
                "SafeSense/Fusion",
                "Incident emitted: type=$type confidence=$confidence"
            )
        }
    }
}