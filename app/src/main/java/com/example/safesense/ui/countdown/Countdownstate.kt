package com.example.safesense.ui.countdown

import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.model.ConfidenceLevel

// ─────────────────────────────────────────────────────────────────────────────
// CountdownState.kt
// Location: ui/countdown/CountdownState.kt
//
// MVI State for the countdown overlay screen.
//
// These states are MUTUALLY EXCLUSIVE. The UI can only ever be in ONE state
// at a time. This is enforced by the architecture — the ViewModel exposes
// a single StateFlow<CountdownState>, and the Compose UI renders based
// on a `when` block that covers all cases.
//
// WHY Dispatching AND Error?
//   The original 4-state model didn't account for the async SMS dispatch.
//   Without these states:
//   - After countdown hits 0, the screen would jump straight to "Alert sent"
//     even though SMS sending takes 1-3 seconds on MTN/Orange networks.
//   - If SMS fails, there would be no way to show the error to the user.
//
//   Dispatching: Shows "Sending alert..." while SmsAlertDispatcher works.
//   Error: Shows what went wrong if the SMS fails to send.
//
//   These are still mutually exclusive with all other states — the MVI
//   guarantee is preserved.
// ─────────────────────────────────────────────────────────────────────────────

sealed class CountdownState {

    // No countdown active — overlay is not visible
    object Idle : CountdownState()

    // Countdown is ticking — this is the critical state
    data class CountdownRunning(
        val incidentType: IncidentType,
        val secondsRemaining: Int,
        val confidence: ConfidenceLevel
    ) : CountdownState()

    // SMS is being sent — shows "Sending alert..." to the user
    data class Dispatching(
        val incidentType: IncidentType
    ) : CountdownState()

    // SMS sent successfully — shows how many contacts were notified
    data class AlertDispatched(
        val contactsNotified: Int,
        val timestamp: Long
    ) : CountdownState()

    // SMS failed to send — shows error reason to the user
    data class Error(
        val reason: String
    ) : CountdownState()

    // User tapped the cancel button during countdown
    object CancelledByUser : CountdownState()
}