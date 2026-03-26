package com.example.safesense.domain.usecase

import com.example.safesense.domain.model.AlertResult
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.repository.ContactRepository
import com.example.safesense.sensor.alert.AlertMessageBuilder
import com.example.safesense.sensor.alert.SmsAlertDispatcher
import com.example.safesense.sensor.processor.GPSTracker
import com.example.safesense.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * SendEmergencyAlertUseCase
 *
 * This is the orchestrator. It is the ONLY entry point for sending an alert.
 * Nothing else in the app sends an SMS — everything routes through here.
 */
class SendEmergencyAlertUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val gpsTracker: GPSTracker,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val smsAlertDispatcher: SmsAlertDispatcher,
    private val logIncidentUseCase: LogIncidentUseCase
) {

    /**
     * Executes the full alert pipeline.
     *
     * This is a suspend function — it must be called from a coroutine.
     * CountdownViewModel calls it from viewModelScope.
     *
     * @param incidentType  The type of incident that triggered the alert
     * @return              AlertResult.Success with contactsNotified count,
     *                      or AlertResult.Failure with a reason if something went wrong
     */
    suspend operator fun invoke(incidentType: IncidentType): AlertResult {

        // ── STEP 1: Get active contacts ──────────────────────────────────────
        val activeContacts = contactRepository.getActiveContacts().first()

        if (activeContacts.isEmpty()) {
            return AlertResult.Failure(reason = "No active emergency contacts configured.")
        }

        // ── STEP 2: Get cached GPS location ──────────────────────────────────
        val location = gpsTracker.getLastLocationForAlert()
            ?: return AlertResult.Failure(reason = "No GPS fix available. Location could not be included.")

        // ── STEP 3: Get user preferences ─────────────────────────────────────
        val userName = userPreferencesRepository.userName.first().ifBlank { "SafeSense User" }
        val language = userPreferencesRepository.selectedLanguage.first()

        // ── STEP 4: Build the SMS message ─────────────────────────────────────
        val smsMessage = AlertMessageBuilder.build(
            userName     = userName,
            incidentType = incidentType,
            latitude     = location.latitude,
            longitude    = location.longitude,
            language     = language
        )

        // ── STEP 5: Log the incident to Room BEFORE sending ───────────────────
        val incidentId = logIncidentUseCase.invoke(
            incidentType = incidentType,
            latitude     = location.latitude,
            longitude    = location.longitude
        )

        // ── STEP 6: Dispatch the SMS ──────────────────────────────────────────
        smsAlertDispatcher.dispatch(
            contacts   = activeContacts,
            message    = smsMessage,
            incidentId = incidentId
        )

        // ── STEP 7: Return success ────────────────────────────────────────────
        return AlertResult.Success(
            contactsNotified = activeContacts.size,
            timestamp        = System.currentTimeMillis()
        )
    }
}
