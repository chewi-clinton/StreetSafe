package com.example.safesense.domain.model

// ─────────────────────────────────────────────────────────────────────────────
// DetectedIncident.kt
// Location: domain/model/DetectedIncident.kt
//
// This is the object SensorFusionEngine emits on its `incidents` SharedFlow
// when the decision matrix fires. CountdownViewModel subscribes to this flow
// and transitions from Idle → CountdownRunning when it arrives.
//
// It is NOT the same as the Room `Incident` entity. That entity is written
// to the database in Step 11 (alert dispatch). This is just the in-memory
// signal that says "something happened, start the countdown."
// ─────────────────────────────────────────────────────────────────────────────

data class DetectedIncident(
    val type: IncidentType,
    val confidenceLevel: ConfidenceLevel,
    val timestamp: Long
)