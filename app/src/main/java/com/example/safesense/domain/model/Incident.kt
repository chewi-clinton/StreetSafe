package com.example.safesense.domain.model

data class Incident(
    val id: Long = 0,
    val type: IncidentType,
    val timestampMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val confidence: ConfidenceLevel,
    val alertStatus: AlertStatus,
    val contactsAlerted: Int,
    val totalActiveContacts: Int,
    val gpsAttached: Boolean,
    val cancelledAfterSeconds: Int?,
    val accelerometerPeakValue: Float?,
    val userNotes: String? = null
)
