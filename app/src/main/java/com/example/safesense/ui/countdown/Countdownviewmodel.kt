package com.example.safesense.ui.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.data.preferences.UserPreferencesRepository
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.repository.ContactRepository
import com.example.safesense.sensor.SensorForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// CountdownViewModel.kt
// Location: ui/countdown/CountdownViewModel.kt
//
// STEP 9 TASK 2 CHANGES vs previous version:
//
//   1. Observes SensorForegroundService.incidents SharedFlow in init{}.
//      When an incident arrives, it calls startCountdown() automatically.
//      This is the final wire from sensor detection to the UI countdown.
//
//   2. startCountdown() is now guarded against being called while already
//      in a non-Idle state. If an incident arrives during an active countdown
//      (e.g. two falls in quick succession), the first countdown is NOT
//      interrupted. The second incident is silently ignored. This is correct
//      behaviour — one active emergency at a time.
//
//   3. The function signature for startCountdown() is unchanged from the
//      previous version, so any existing test code that calls it directly
//      (e.g. from SettingsScreen's test ShakeDetector) still compiles.
//      You should delete the test ShakeDetector from SettingsScreen now.
//
// EVERYTHING ELSE IS UNCHANGED.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CountdownViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val state: StateFlow<CountdownState> = _state.asStateFlow()

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private var timerJob: Job? = null

    init {
        // ── Step 9 Task 2: Subscribe to the incident stream ───────────────────
        //
        // SensorForegroundService.incidents is a companion object SharedFlow.
        // Every DetectedIncident emitted here means SensorFusionEngine
        // has declared a real emergency. We respond by starting the countdown.
        //
        // Why launchIn(viewModelScope) instead of launch + collect?
        // launchIn is idiomatic for fire-and-forget Flow subscriptions in a
        // ViewModel. It cancels automatically when the ViewModel is cleared.
        //
        // Why not inject SensorFusionEngine directly?
        // SensorFusionEngine lives in the sensor layer (Layer 4). ViewModels
        // live in the presentation layer (Layer 1). Clean Architecture says
        // Layer 1 cannot import Layer 4. We bridge through the companion object
        // SharedFlow on SensorForegroundService, which is the same pattern
        // already used for accelerometerActive in HomeViewModel.
        SensorForegroundService.incidents
            .onEach { incident ->
                android.util.Log.d(
                    "SafeSense/CountdownVM",
                    "Incident received: ${incident.type} ${incident.confidenceLevel}"
                )
                startCountdown(incident.type, incident.confidenceLevel)
            }
            .launchIn(viewModelScope)
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Transitions from Idle → CountdownRunning.
     *
     * Guards: if already in CountdownRunning, AlertDispatched, or CancelledByUser,
     * this call is ignored. The state machine does not allow two countdowns at once.
     *
     * Called from:
     *   - init{} above (automatic, via SensorForegroundService.incidents)
     *   - Any test code or manual trigger (e.g. Panic Button in Step 15)
     */
    fun startCountdown(incidentType: IncidentType, confidence: ConfidenceLevel) {
        // Guard: only start from Idle. Any other state = an emergency is already active.
        if (_state.value !is CountdownState.Idle) {
            android.util.Log.d(
                "SafeSense/CountdownVM",
                "startCountdown ignored — state is ${_state.value::class.simpleName}"
            )
            return
        }

        viewModelScope.launch {
            _contacts.value = contactRepository.getActiveContacts().first()
            val duration = preferencesRepository.countdownDurationSeconds.first()

            _state.value = CountdownState.CountdownRunning(
                incidentType     = incidentType,
                secondsRemaining = duration,
                confidence       = confidence
            )

            timerJob = launch {
                var remaining = duration
                while (remaining > 0) {
                    delay(1_000)
                    remaining--
                    val current = _state.value
                    if (current is CountdownState.CountdownRunning) {
                        _state.value = current.copy(secondsRemaining = remaining)
                    } else {
                        // State was changed externally (cancelByUser) — stop the timer
                        return@launch
                    }
                }
                // Timer reached zero with no cancellation — dispatch the alert
                dispatchAlert()
            }
        }
    }

    fun cancelByUser() {
        timerJob?.cancel()
        timerJob = null
        _state.value = CountdownState.CancelledByUser
    }

    /**
     * Resets the ViewModel back to Idle.
     * Called by CountdownScreen after showing AlertDispatched or CancelledByUser
     * long enough for the user to read it.
     */
    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _state.value = CountdownState.Idle
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun dispatchAlert() {
        // Step 11 will replace this with real SMS dispatch via SendEmergencyAlertUseCase.
        // For now we transition to AlertDispatched so the UI shows the right state.
        val notified = _contacts.value.size
        _state.value = CountdownState.AlertDispatched(
            contactsNotified = notified,
            timestamp        = System.currentTimeMillis()
        )
    }
}