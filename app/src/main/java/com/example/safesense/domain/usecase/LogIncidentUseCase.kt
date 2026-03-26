package com.example.safesense.domain.usecase

import com.example.safesense.domain.model.AlertStatus
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.repository.IncidentRepository
import javax.inject.Inject

/**
 * LogIncidentUseCase
 *
 * Responsibility: Create a new incident record in the database.
 * This is called by SendEmergencyAlertUseCase before the SMS is dispatched
 * to ensure we have an incident ID for tracking delivery receipts.
 */
class LogIncidentUseCase @Inject constructor(
    private val incidentRepository: IncidentRepository
) {

    /**
     * Inserts a new incident into the database and returns its unique ID.
     *
     * @param incidentType  The type of detection (FALL, COLLISION, etc.)
     * @param latitude      Latitude at time of detection
     * @param longitude     Longitude at time of detection
     * @param confidence    The confidence level of the detection
     * @return              The Long ID of the newly created database row
     */
    suspend operator fun invoke(
        incidentType: IncidentType,
        latitude: Double?,
        longitude: Double?,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH
    ): Long {
        val incident = Incident(
            type = incidentType,
            timestampMillis = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            confidence = confidence,
            alertStatus = AlertStatus.PENDING,
            contactsAlerted = 0,
            totalActiveContacts = 0, // Will be updated by dispatcher
            gpsAttached = latitude != null,
            cancelledAfterSeconds = null,
            accelerometerPeakValue = null
        )

        return incidentRepository.insertIncident(incident)
    }
}
