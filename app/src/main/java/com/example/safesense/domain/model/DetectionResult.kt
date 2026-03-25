package com.example.safesense.domain.model

// ─────────────────────────────────────────────────────────────────────────────
// DetectionResult.kt
// Location: domain/model/DetectionResult.kt
// ─────────────────────────────────────────────────────────────────────────────

sealed class DetectionResult {
    data class Detected(val confidence: ConfidenceLevel) : DetectionResult()
    object NotDetected : DetectionResult()
}