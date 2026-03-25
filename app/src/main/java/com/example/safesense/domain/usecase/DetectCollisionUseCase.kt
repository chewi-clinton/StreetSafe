package com.example.safesense.domain.usecase

import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.sensor.processor.AccelerometerEvent
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// DetectCollisionUseCase.kt
// Location: domain/usecase/DetectCollisionUseCase.kt
//
// THE COLLISION ALGORITHM — TWO PHASES
//
// A vehicle collision feels very different from a fall:
//   - No free-fall phase (you are inside a vehicle, not falling)
//   - A single enormous G-force spike (the impact)
//   - Then either stillness OR continued motion (you are in a crumpling vehicle)
//
// Unlike fall detection which needs a quiet phase 1, collision detection
// watches for a raw magnitude spike ABOVE 40 m/s² — that is roughly 4 times
// gravity. Normal transport (moto-taxi, bus, potholed road in Yaoundé) peaks
// around 15–25 m/s². A real collision starts at 40 m/s².
//
// PHASE 1 — SPIKE
//   magnitude > COLLISION_THRESHOLD (40.0 m/s²)
//   This is the impact force.
//
// PHASE 2 — POST-IMPACT STILLNESS
//   After the spike, wait for the phone to settle.
//   magnitude ≈ 9.8 ± 2.0 for at least 3 seconds.
//   Why 3 seconds not 5? A collision victim may be conscious and moving
//   within a few seconds. We do not want to miss real collisions by
//   requiring the same long stillness as a fall.
//   The wider tolerance (±2.0 vs ±1.5 for falls) also accounts for the
//   phone settling at an angle inside a crumpled vehicle.
//
// CONFIDENCE
//   The collision use case always returns HIGH confidence when both phases
//   complete. Collision + stillness is a very specific signature.
//   SensorFusionEngine can upgrade to HIGH further if audio also matches.
//
// AUDIO UPGRADE (handled in SensorFusionEngine, not here)
//   Row 3 of the decision matrix: >4g spike + any proximity + loud sound → Collision HIGH
//   This use case just handles the accelerometer side of that row.
//   SensorFusionEngine combines the result with the audio signal.
// ─────────────────────────────────────────────────────────────────────────────

class DetectCollisionUseCase @Inject constructor() {

    companion object {
        // Phase 1 — spike threshold
        // 40 m/s² ≈ 4g. Normal Yaoundé road bumps peak at 15–25 m/s².
        // Real crash impacts start at 40 m/s² and go much higher.
        const val COLLISION_THRESHOLD_MS_PER_S2 = 40.0f

        // Phase 2 — post-impact stillness
        const val GRAVITY                        = 9.8f
        const val STILLNESS_TOLERANCE_MS_PER_S2  = 2.0f   // wider than fall (±2.0 vs ±1.5)
        const val STILLNESS_MIN_DURATION_MS      = 3000L  // shorter than fall (3s vs 5s)

        // How long to wait for stillness after the spike before giving up
        const val POST_IMPACT_TIMEOUT_MS         = 8000L
    }

    private enum class CollisionPhase {
        WAITING_FOR_SPIKE,
        WAITING_FOR_STILLNESS
    }

    private var phase = CollisionPhase.WAITING_FOR_SPIKE
    private var spikeTime = 0L
    private var stillnessStartTime = 0L

    // ── Main entry point ──────────────────────────────────────────────────────
    //
    // Called by SensorFusionEngine on every AccelerometerEvent.
    // Returns DetectionResult.Detected(HIGH) when both phases complete.
    // Returns DetectionResult.NotDetected otherwise.

    fun process(event: AccelerometerEvent): DetectionResult {
        return when (phase) {
            CollisionPhase.WAITING_FOR_SPIKE     -> handleSpikePhase(event.magnitude, event.timestamp)
            CollisionPhase.WAITING_FOR_STILLNESS -> handleStillnessPhase(event.magnitude, event.timestamp)
        }
    }

    // ── Phase 1 — spike ───────────────────────────────────────────────────────

    private fun handleSpikePhase(magnitude: Float, timestamp: Long): DetectionResult {
        if (magnitude >= COLLISION_THRESHOLD_MS_PER_S2) {
            // Impact spike detected — move to phase 2
            spikeTime = timestamp
            stillnessStartTime = 0L
            phase = CollisionPhase.WAITING_FOR_STILLNESS
            android.util.Log.d("SafeSense/Collision", "Phase 1 confirmed — spike at $magnitude m/s²")
        }
        return DetectionResult.NotDetected
    }

    // ── Phase 2 — post-impact stillness ───────────────────────────────────────

    private fun handleStillnessPhase(magnitude: Float, timestamp: Long): DetectionResult {
        // Check if we have waited too long with no stable stillness
        if (timestamp - spikeTime > POST_IMPACT_TIMEOUT_MS) {
            android.util.Log.d("SafeSense/Collision", "Phase 2 timeout — no stillness after spike, resetting")
            reset()
            return DetectionResult.NotDetected
        }

        val distanceFromGravity = Math.abs(magnitude - GRAVITY)

        if (distanceFromGravity <= STILLNESS_TOLERANCE_MS_PER_S2) {
            // Phone is settling near gravity — potential stillness
            if (stillnessStartTime == 0L) {
                stillnessStartTime = timestamp
            }

            val stillnessDuration = timestamp - stillnessStartTime

            if (stillnessDuration >= STILLNESS_MIN_DURATION_MS) {
                // 3 seconds of stillness after a 4g spike = collision confirmed
                android.util.Log.d(
                    "SafeSense/Collision",
                    "Phase 2 confirmed — ${stillnessDuration}ms stillness after spike"
                )
                reset()
                return DetectionResult.Detected(ConfidenceLevel.HIGH)
            }
        } else {
            // Movement broke the stillness — reset stillness timer but stay in phase 2
            // (the vehicle may still be rocking after a crash)
            stillnessStartTime = 0L
        }

        return DetectionResult.NotDetected
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    fun reset() {
        phase              = CollisionPhase.WAITING_FOR_SPIKE
        spikeTime          = 0L
        stillnessStartTime = 0L
    }
}