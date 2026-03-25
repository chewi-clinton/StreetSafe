package com.example.safesense.presentation.countdown

import com.example.safesense.domain.model.IncidentType

sealed class CountdownState {

    object Idle : CountdownState()

    data class CountdownRunning(
        val type: IncidentType,
        val secondsRemaining: Int,
        val confidenceLevel: Float
    ) : CountdownState()

    data class AlertDispatched(
        val contactsNotified: Int,
        val timestamp: Long
    ) : CountdownState()

    object CancelledByUser : CountdownState()
}
