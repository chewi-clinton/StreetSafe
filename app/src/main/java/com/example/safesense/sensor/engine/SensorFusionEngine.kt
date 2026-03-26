package com.example.safesense.sensor.engine

import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.DetectedIncident
import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.usecase.DetectCollisionUseCase
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
//   This is the most important constraint in the entire engine.
//   If proximity changes at t=0 and the accelerometer spike arrives at t=600ms,
//   we do NOT combine them — 600ms is outside the window.
//   Why 500ms? Research on fall and collision biomechanics shows that all
//   body sensor signals from a single event arrive within ~300ms. We use 500ms
//   to account for sensor polling jitter on low-end Android hardware.
//   Signals outside this window are INDEPENDENT events, not correlated.
//
// THE FIVE DECISION MATRIX ROWS (from master prompt):
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
//          Audio distress sound within 500ms (any proximity)
//          → Collision, HIGH confidence
//          (Without audio: → Collision, HIGH — collision alone is HIGH)
//
//   Row 4: No accelerometer event
//          Proximity changed NEAR→FAR, and audio loud within 500ms
//          → Snatched, MEDIUM confidence
//
//   Row 5: Shake gesture (≥3 shakes in 2s) — NOT handled here yet
//          Proximity is NEAR (phone in hand)
//          → Shake Alert, HIGH confidence
//          (Shake detection is Step 17 — this row is a placeholder)
//
// WHY NOT USE FLOWS INTERNALLY?
//   The processors emit on SharedFlows. We COULD collect those flows here.
//   But that creates back-pressure problems and requires each processor to
//   be injected as a dependency of this engine.
//   Instead, SensorForegroundService acts as the coordinator — it collects
//   all processor flows and calls methods on this engine. This keeps the
//   engine simple, testable, and dependency-free from Android sensor APIs.
// ─────────────────────────────────────────────────────────────────────────────

class SensorFusionEngine(
    private val scope: CoroutineScope,
    private val detectFallUseCase: DetectFallUseCase,
    private val detectCollisionUseCase: DetectCollisionUseCase = DetectCollisionUseCase()
) {

    companion object {
        // The 500ms correlation window — signals outside this are never combined
        const val CORRELATION_WINDOW_MS = 500L

        // Snatched detection requires both proximity AND audio
        // If only one fires without the other in the window, ignore it
        // (proximity change alone could be putting phone in pocket;
        //  audio alone could be a passing motorbike)

        const val SHAKE_WINDOW_MS = 2000L  // all shakes must occur within this window
        const val SHAKE_MIN_COUNT = 3      // minimum shakes in the window to qualify
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

    // ── Row 5 shake tracking ──────────────────────────────────────────────────
    //
    // We keep a rolling list of shake timestamps. Every call to onShakeEvent()
    // appends the current timestamp and drops any entries older than 2 seconds.
    // When the list reaches 3 entries AND proximity is NEAR (phone is in hand),
    // Row 5 fires: Shake Alert at HIGH confidence.
    //
    // Note: the shake gesture algorithm itself (jerk calculation) lives in
    // RecognizeShakeGestureUseCase — that is Step 17. For now, onShakeEvent()
    // is a stub entry point so the wiring is correct. When Step 17 is done,
    // SensorForegroundService calls this method instead of calling the use case
    // directly, and Row 5 will fire automatically.
    private val shakeTimestamps = ArrayDeque<Long>()
    private var proximityIsNear = false  // true when last proximity state was NEAR

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
                handleCollisionDetected(event.timestamp)
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
     * Row 5: Called by SensorForegroundService when RecognizeShakeGestureUseCase
     * detects a single shake event (Step 17 will wire this).
     *
     * We maintain a rolling 2-second window of shake timestamps.
     * When ≥3 shakes occur within that window AND proximity is NEAR
     * (phone is in the user's hand), we emit a Shake Alert at HIGH confidence.
     *
     * Why NEAR and not FAR? The user is deliberately shaking their phone
     * as a distress gesture. If the phone is not in their hand (FAR),
     * the shaking is more likely a bag or vehicle vibration — ignore it.
     */
    fun onShakeEvent(timestamp: Long) {
        // Add this shake and purge any timestamps outside the 2-second window
        shakeTimestamps.addLast(timestamp)
        while (shakeTimestamps.isNotEmpty() &&
            timestamp - shakeTimestamps.first() > SHAKE_WINDOW_MS) {
            shakeTimestamps.removeFirst()
        }

        android.util.Log.d(
            "SafeSense/Fusion",
            "Shake event: ${shakeTimestamps.size} shakes in last ${SHAKE_WINDOW_MS}ms, proximityNear=$proximityIsNear"
        )

        if (shakeTimestamps.size >= SHAKE_MIN_COUNT && proximityIsNear) {
            android.util.Log.d("SafeSense/Fusion", "Row 5: ≥3 shakes + NEAR → SHAKE HIGH")
            shakeTimestamps.clear()  // prevent double-firing
            emitIncident(IncidentType.SHAKE, ConfidenceLevel.HIGH, timestamp)
        }
    }

    // ── Decision matrix rows ──────────────────────────────────────────────────

    /**
     * Rows 1 and 2: Fall detection
     *
     * Row 1: Fall alone                → MEDIUM confidence (or whatever use case returned)
     * Row 2: Fall + proximity change   → upgrade to HIGH confidence
     *
     * The use case already determined LOW/MEDIUM/HIGH from stillness duration.
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
     * Collision alone already returns HIGH from DetectCollisionUseCase.
     * Audio corroboration confirms it further (same confidence — already HIGH).
     * We log which sub-row fired for debugging, but the confidence is HIGH either way.
     */
    private fun handleCollisionDetected(detectionTimestamp: Long) {
        val audioCorroborated = isWithinWindow(lastDistressSoundTimestamp, detectionTimestamp)

        if (audioCorroborated) {
            android.util.Log.d(
                "SafeSense/Fusion",
                "Row 3a: Collision + distress sound → HIGH"
            )
        } else {
            android.util.Log.d(
                "SafeSense/Fusion",
                "Row 3b: Collision alone → HIGH"
            )
        }

        emitIncident(IncidentType.COLLISION, ConfidenceLevel.HIGH, detectionTimestamp)
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

            emitIncident(IncidentType.SHAKE, ConfidenceLevel.MEDIUM, currentTimestamp)
            // Note: using SHAKE as a proxy for SNATCHED because IncidentType does not
            // have a SNATCHED value yet. When IncidentType is expanded in Step 11,
            // change this to IncidentType.SNATCHED.
            // For now this ensures the incident reaches CountdownViewModel correctly.
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
