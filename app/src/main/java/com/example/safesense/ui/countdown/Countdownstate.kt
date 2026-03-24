package com.example.safesense.ui.countdown

import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.IncidentType

sealed class CountdownState {
    object Idle : CountdownState()

    data class CountdownRunning(
        val incidentType: IncidentType,
        val secondsRemaining: Int,
        val confidence: ConfidenceLevel
    ) : CountdownState()

    data class AlertDispatched(
        val contactsNotified: Int,
        val timestamp: Long
    ) : CountdownState()

    object CancelledByUser : CountdownState()
}