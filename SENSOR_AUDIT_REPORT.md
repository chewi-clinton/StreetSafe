# SafeSense Sensor Pipeline Audit Report
**Date:** March 2026
**Status:** **FAIL** (4 Critical Issues, 4 Bugs)

## Executive Summary
The sensor pipeline for SafeSense has been audited against the offline-first, battery-efficient, and device-agnostic requirements for deployment on Tecno/Infinix hardware in Cameroon. While the core fall detection logic (Block B) and the fusion engine correlation (Block H) are solid, several critical architectural failures in the GPS, Proximity, and Audio components will lead to silent failures and false alerts in production.

---

## Block-by-Block Status

| Block | Component | Status | Critical Issues |
|-------|-----------|--------|-----------------|
| A | AccelerometerProcessor | **PASS** | 0 |
| B | DetectFallUseCase | **PASS** | 0 |
| C | DetectCollisionUseCase | **PARTIAL** | 0 |
| D | RecognizeShakeGestureUseCase | **FAIL** | 0 |
| E | ProximityProcessor | **FAIL** | 2 |
| F | AudioMonitor | **FAIL** | 1 |
| G | GPSTracker | **FAIL** | 1 |
| H | SensorFusionEngine | **PASS** | 0 |
| I | SensorForegroundService | **PASS** | 0 |
| J | CountdownViewModel | **FAIL** | 0 |
| K | SensorHeartbeatWorker | **PASS** | 0 |

---

## Critical Issues (Priority Order)

### 1. [CRITICAL] GPS Failure in Airplane Mode (Block G)
**Component:** `GPSTracker.kt`
**Issue:** Missing `setForceAndroidLocationManager(true)` in the location request.
**Impact:** On devices without Google Play Services or in Airplane Mode, the FusedLocationProvider will fail to retrieve a fix. **Emergency alerts will be sent without coordinates.**

### 2. [CRITICAL] Binary Sensor Incompatibility (Block E)
**Component:** `ProximityProcessor.kt`
**Issue:** Uses a hardcoded `NEAR_THRESHOLD = 1.0f`.
**Impact:** Most modern Android proximity sensors are binary (reporting 0.0 or 5.0). Some report their `maximumRange` as the "FAR" value. Using `1.0f` is fragile. The spec mandates using `sensor.maximumRange`.

### 3. [CRITICAL] Timing Drift in Fusion Engine (Block E)
**Component:** `ProximityProcessor.kt`
**Issue:** Uses `System.currentTimeMillis()` for events instead of `event.timestamp`.
**Impact:** The SensorFusionEngine relies on a tight 500ms window. System time and sensor clock time can drift, causing valid signals to be ignored by the judge, leading to **missed detections**.

### 4. [CRITICAL] False Alert Risk - Transient Noise (Block F)
**Component:** `AudioMonitor.kt`
**Issue:** Missing the 2-second sustain check for distress sounds.
**Impact:** Any momentary loud noise (door slam, car horn, shouting) will trigger a `DistressSound` event. If this coincides with a bump (common in moto-taxis), a **false emergency alert will be dispatched.**

---

## Technical Bugs & Improvements

### Block D — RecognizeShakeGestureUseCase
*   **[BUG]:** Hardcoded thresholds. Shake count and time window must be read from `UserPreferences`.
*   **[BUG]:** `runBlocking` in the sensor path. Calling `runBlocking` at 50Hz to check settings will cause UI stuttering and battery drain.

### Block F — AudioMonitor
*   **[BUG]:** Incorrect Threshold. Uses 85dB instead of the 90dB specified.

### Block J — CountdownViewModel
*   **[BUG]:** Hardcoded Duration. The 10s/15s countdown logic ignores the user's configured settings in DataStore.

### Block C — DetectCollisionUseCase
*   **[WARNING]:** Missing GPS speed corroboration. While not critical, the spec suggested using speed > 10km/h to upgrade to HIGH confidence.

---

## Verified Successes (The "Good" News)
*   **Block K (Heartbeat):** The WorkManager heartbeat is correctly implemented with `@HiltWorker` and correctly detects silent service kills.
*   **Block B (Fall Logic):** The three-phase fall algorithm is mathematically sound and follows research standards.
*   **Block H (Fusion Engine):** The correlation window logic is correctly implemented and correctly handles the "Snatched" pattern.
*   **Block A (Battery):** Tiered sampling is correctly implemented to save battery during idle monitoring.
