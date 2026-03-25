package com.example.safesense.ui.countdown

import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.IncidentType

// ─────────────────────────────────────────────────────────────────────────────
// CountdownState.kt
// Location: ui/countdown/CountdownState.kt
//
// These four states are MUTUALLY EXCLUSIVE. The UI can only ever be in
// one at a time. This is enforced by the sealed class — the compiler will
// not let you create a fifth state. This is the MVI pattern.
//
// The master prompt calls this "sacred" — do not add states, do not merge
// states, do not add nullable fields to simulate additional states.
//
// If you feel like you need a fifth state, you need a new ViewModel or
// a new screen — not a change here.
// ─────────────────────────────────────────────────────────────────────────────

sealed class CountdownState {

    // The app is monitoring but no incident has been detected.
    // This is the default state when the service is running.
    object Idle : CountdownState()

    // An incident was detected and the countdown is ticking.
    // The user has secondsRemaining seconds to tap "I'm Safe" before
    // the alert is dispatched.
    data class CountdownRunning(
        val incidentType: IncidentType,
        val secondsRemaining: Int,
        val confidence: ConfidenceLevel
    ) : CountdownState()

    // The countdown reached zero and the SMS alert was dispatched.
    // We store how many contacts were notified and when.
    data class AlertDispatched(
        val contactsNotified: Int,
        val timestamp: Long
    ) : CountdownState()

    // The user tapped "I'm Safe" during the countdown.
    // No alert was sent.
    object CancelledByUser : CountdownState()
}