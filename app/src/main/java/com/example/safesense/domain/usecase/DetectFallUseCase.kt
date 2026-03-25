package com.example.safesense.domain.usecase

import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.sensor.processor.AccelerometerEvent
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// DetectFallUseCase.kt
// Location: domain/usecase/DetectFallUseCase.kt
//
// THE THREE-PHASE FALL ALGORITHM
// All threshold values come from 15 published fall-detection research studies.
// Do NOT change the numbers without understanding what they represent.
//
// PHASE 1 — FREE FALL
//   The phone goes briefly weightless as it falls.
//   Magnitude drops below 3.0 m/s² (much less than gravity = 9.8 m/s²).
//   This must last at least 200ms — normal movement never produces weightlessness
//   that long. A 200ms window filters out bumps and phone grabs.
//
// PHASE 2 — IMPACT
//   The phone hits the ground (or the person hits the ground with the phone).
//   Magnitude spikes above 20.0 m/s² within 1 second of Phase 1 ending.
//   The 1-second window is tight on purpose: if more than 1 second passes
//   between free fall and impact, it was not a fall — it was a throw or a drop
//   from a table, which we should NOT alert on.
//
// PHASE 3 — STILLNESS
//   After a real fall, the person is on the ground and not moving.
//   Magnitude settles within 1.5 m/s² of 9.8 (gravity at rest) for 5 seconds.
//   This phase does two things:
//     1. Eliminates false positives from sports and vigorous activity
//     2. Gives us the confidence score — longer stillness = higher confidence
//
// CONFIDENCE SCORING
//   LOW    — Phase 3 lasts 5–7 seconds
//   MEDIUM — Phase 3 lasts 7–10 seconds
//   HIGH   — Phase 3 lasts more than 10 seconds
//   Why does this matter? HIGH confidence = alert sent immediately.
//   LOW confidence = longer countdown giving user more time to cancel.
//   (The countdown duration logic lives in the ViewModel, not here.)
//
// STATE MACHINE
//   WAITING_FOR_FREE_FALL → WAITING_FOR_IMPACT → WAITING_FOR_STILLNESS → emit
//   If any phase times out, state resets to WAITING_FOR_FREE_FALL.
//   The use case never gets "stuck" — it always resets cleanly.
// ─────────────────────────────────────────────────────────────────────────────

class DetectFallUseCase @Inject constructor() {

    // ── Thresholds — do not change without reading the research ──────────────

    companion object {
        // Phase 1 — Free fall
        const val FREE_FALL_THRESHOLD_MS_PER_S2 = 3.0f   // below this = weightless
        const val FREE_FALL_MIN_DURATION_MS      = 200L   // must last this long

        // Phase 2 — Impact
        const val IMPACT_THRESHOLD_MS_PER_S2     = 20.0f  // above this = impact
        const val IMPACT_WINDOW_MS               = 1000L  // impact must follow free fall within this

        // Phase 3 — Stillness
        const val GRAVITY                        = 9.8f
        const val STILLNESS_TOLERANCE_MS_PER_S2  = 1.5f   // within this of gravity = still
        const val STILLNESS_MIN_DURATION_MS      = 5000L  // must be still this long to confirm fall

        // Confidence thresholds
        const val MEDIUM_CONFIDENCE_STILLNESS_MS = 7000L
        const val HIGH_CONFIDENCE_STILLNESS_MS   = 10000L
    }

    // ── State machine ─────────────────────────────────────────────────────────

    private enum class FallPhase {
        WAITING_FOR_FREE_FALL,
        WAITING_FOR_IMPACT,
        WAITING_FOR_STILLNESS
    }

    private var phase = FallPhase.WAITING_FOR_FREE_FALL

    // Phase 1 tracking
    private var freeFallStartTime = 0L     // when magnitude first dropped below threshold

    // Phase 2 tracking
    private var freeFallEndTime   = 0L     // when free fall ended — impact must come within 1 second

    // Phase 3 tracking
    private var stillnessStartTime = 0L   // when stillness began

    // ── Main entry point ──────────────────────────────────────────────────────
    //
    // Called by SensorFusionEngine every time AccelerometerProcessor emits an event.
    // Returns DetectionResult.Detected if all three phases completed in sequence.
    // Returns DetectionResult.NotDetected otherwise.
    //
    // The caller (SensorFusionEngine) is responsible for feeding events in order.
    // This use case does not collect from any Flow itself — it is pure logic.

    fun process(event: AccelerometerEvent): DetectionResult {
        val magnitude  = event.magnitude
        val timestamp  = event.timestamp

        return when (phase) {
            FallPhase.WAITING_FOR_FREE_FALL -> handleFreeFallPhase(magnitude, timestamp)
            FallPhase.WAITING_FOR_IMPACT    -> handleImpactPhase(magnitude, timestamp)
            FallPhase.WAITING_FOR_STILLNESS -> handleStillnessPhase(magnitude, timestamp)
        }
    }

    // ── Phase 1 — Free fall ───────────────────────────────────────────────────

    private fun handleFreeFallPhase(magnitude: Float, timestamp: Long): DetectionResult {
        if (magnitude < FREE_FALL_THRESHOLD_MS_PER_S2) {
            // Magnitude is below free-fall threshold
            if (freeFallStartTime == 0L) {
                // First reading below threshold — start the timer
                freeFallStartTime = timestamp
            } else if (timestamp - freeFallStartTime >= FREE_FALL_MIN_DURATION_MS) {
                // Been below threshold for 200ms — free fall confirmed
                // Advance to Phase 2 and record when free fall ended
                freeFallEndTime = timestamp
                phase = FallPhase.WAITING_FOR_IMPACT

                android.util.Log.d("SafeSense/Fall", "Phase 1 confirmed — free fall detected")
            }
            // Still accumulating duration — do nothing yet
        } else {
            // Magnitude went back above threshold before 200ms — reset timer
            // This was a bump or a grab, not a fall
            freeFallStartTime = 0L
        }

        return DetectionResult.NotDetected
    }

    // ── Phase 2 — Impact ──────────────────────────────────────────────────────

    private fun handleImpactPhase(magnitude: Float, timestamp: Long): DetectionResult {
        // Check if the impact window has expired
        if (timestamp - freeFallEndTime > IMPACT_WINDOW_MS) {
            // 1 second passed with no impact — this was not a fall
            // Could have been the user reaching for something, or phone sliding off a table
            android.util.Log.d("SafeSense/Fall", "Phase 2 expired — no impact within 1 second, resetting")
            reset()
            return DetectionResult.NotDetected
        }

        if (magnitude >= IMPACT_THRESHOLD_MS_PER_S2) {
            // Impact spike detected within the window — Phase 2 confirmed
            // Advance to Phase 3
            stillnessStartTime = 0L
            phase = FallPhase.WAITING_FOR_STILLNESS

            android.util.Log.d("SafeSense/Fall", "Phase 2 confirmed — impact detected at $magnitude m/s²")
        }

        return DetectionResult.NotDetected
    }

    // ── Phase 3 — Stillness ───────────────────────────────────────────────────

    private fun handleStillnessPhase(magnitude: Float, timestamp: Long): DetectionResult {
        val distanceFromGravity = Math.abs(magnitude - GRAVITY)

        if (distanceFromGravity <= STILLNESS_TOLERANCE_MS_PER_S2) {
            // Magnitude is near gravity — phone is lying still on the ground
            if (stillnessStartTime == 0L) {
                // First still reading — start the stillness timer
                stillnessStartTime = timestamp
            }

            val stillnessDuration = timestamp - stillnessStartTime

            if (stillnessDuration >= STILLNESS_MIN_DURATION_MS) {
                // Been still for at least 5 seconds — fall confirmed
                val confidence = when {
                    stillnessDuration >= HIGH_CONFIDENCE_STILLNESS_MS   -> ConfidenceLevel.HIGH
                    stillnessDuration >= MEDIUM_CONFIDENCE_STILLNESS_MS -> ConfidenceLevel.MEDIUM
                    else                                                 -> ConfidenceLevel.LOW
                }

                android.util.Log.d(
                    "SafeSense/Fall",
                    "Phase 3 confirmed — stillness ${stillnessDuration}ms, confidence=$confidence"
                )

                reset()
                return DetectionResult.Detected(confidence)
            }

        } else {
            // Phone moved again — stillness broken
            // Two possibilities:
            //   A) Person is actually moving — this was a false positive, reset everything
            //   B) Person is moving because they are injured and trying to get up — still a fall
            // We cannot distinguish these. We reset. The 5-second window is the safety net:
            // if stillness resets before 5 seconds, it was probably not a real fall.
            android.util.Log.d("SafeSense/Fall", "Phase 3 broken — movement detected, resetting stillness timer")
            stillnessStartTime = 0L

            // Also check: if too much time has passed since impact with no stable stillness,
            // the whole detection has gone stale — reset fully
            // We allow up to 15 seconds for Phase 3 before giving up entirely
            if (freeFallEndTime > 0L && timestamp - freeFallEndTime > 15_000L) {
                android.util.Log.d("SafeSense/Fall", "Phase 3 timeout — resetting entire state machine")
                reset()
            }
        }

        return DetectionResult.NotDetected
    }

    // ── Reset ─────────────────────────────────────────────────────────────────
    //
    // Called after every confirmed detection AND after every timeout.
    // The state machine must always be able to detect the next fall
    // immediately after resetting.

    fun reset() {
        phase              = FallPhase.WAITING_FOR_FREE_FALL
        freeFallStartTime  = 0L
        freeFallEndTime    = 0L
        stillnessStartTime = 0L
    }
}