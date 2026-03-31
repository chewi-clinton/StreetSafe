package com.example.safesense.ui.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.domain.model.AlertResult
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.repository.ContactRepository
import com.example.safesense.domain.usecase.SendEmergencyAlertUseCase
import com.example.safesense.data.preferences.UserPreferencesRepository
import com.example.safesense.sensor.SensorForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// CountdownViewModel.kt
// Location: ui/countdown/CountdownViewModel.kt
//
// WHAT THIS DOES:
//   Manages the MVI state for the full-screen countdown overlay.
//   When the FusionEngine declares an incident, this ViewModel transitions
//   from Idle → CountdownRunning → AlertDispatched (or CancelledByUser).
//
// AUDIT FIX — REMOVED HARDCODED DURATION:
//   OLD: val initialSeconds = if (incidentType == WALK_MODE) 10 else 15
//   PROBLEM: User can configure countdown duration in Settings (10/15/20 seconds)
//       but the ViewModel ignored it. Every user got 10s or 15s regardless
//       of their preference.
//   FIX: Read countdownDurationSeconds from UserPreferences and cache it in a StateFlow.
//       The cached value updates if the user changes settings while the app is open.
//       The countdown uses the cached value when it starts.
//
// WHY NOT READ FROM DataStore INSIDE startCountdown()?
//   startCountdown() may be called from a non-suspend context (UI click on panic
//   button). Reading from DataStore requires a coroutine. By caching in a StateFlow
//   that's updated via collect in init{}, the value is always ready synchronously.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CountdownViewModel @Inject constructor(
    private val sendEmergencyAlertUseCase: SendEmergencyAlertUseCase,
    private val contactRepository: ContactRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val state: StateFlow<CountdownState> = _state.asStateFlow()

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private var countdownJob: Job? = null

    // ── FIX: Cached countdown duration from UserPreferences ──────────────────
    // Default is 15 seconds (the middle option from Settings: 10/15/20).
    // Updated via collect in init{} — always ready synchronously.
    private val _countdownDurationSeconds = MutableStateFlow(15)
    val countdownDurationSeconds: StateFlow<Int> = _countdownDurationSeconds.asStateFlow()

    private var falsePositiveCountInWindow = 0
    private var windowStartTime = 0L
    private val FALSE_POSITIVE_WINDOW_MS = 24 * 60 * 60 * 1000L

    init {
        // Load contacts
        viewModelScope.launch {
            contactRepository.getActiveContacts().collect {
                _contacts.value = it
            }
        }

        // ── FIX: Observe countdown duration from DataStore ──
        // Keeps the cached value updated if user changes settings.
        // Uses a try-catch so a DataStore failure doesn't crash the ViewModel.
        viewModelScope.launch {
            try {
                userPreferencesRepository.countdownDurationSeconds.collect { duration ->
                    _countdownDurationSeconds.value = duration
                }
            } catch (e: Exception) {
                android.util.Log.w(
                    "SafeSense/Countdown",
                    "Failed to read countdown duration, using default 15s: ${e.message}"
                )
            }
        }

        subscribeToIncidents()
    }

    private fun subscribeToIncidents() {
        viewModelScope.launch {
            SensorForegroundService.incidents.collect { incident ->
                // Guard: ignore if a countdown is already running
                if (_state.value !is CountdownState.Idle) return@collect
                startCountdown(incident.type, incident.confidenceLevel)
            }
        }
    }

    fun startCountdown(incidentType: IncidentType, confidence: ConfidenceLevel) {
        if (_state.value is CountdownState.CountdownRunning) return
        countdownJob?.cancel()

        // ── FIX: Use cached value from UserPreferences instead of hardcoded 10/15 ──
        // The build guide specifies a single countdown duration setting (10/15/20s)
        // that applies to ALL incident types, not separate durations per type.
        val initialSeconds = _countdownDurationSeconds.value

        android.util.Log.d(
            "SafeSense/Countdown",
            "Starting countdown: ${initialSeconds}s for $incidentType"
        )

        countdownJob = viewModelScope.launch {
            for (secondsLeft in initialSeconds downTo 1) {
                _state.value = CountdownState.CountdownRunning(
                    incidentType     = incidentType,
                    secondsRemaining = secondsLeft,
                    confidence       = confidence
                )
                delay(1_000L)
            }
            fireAlert(incidentType)
        }
    }

    fun cancelByUser() {
        if (_state.value !is CountdownState.CountdownRunning) return
        countdownJob?.cancel()
        countdownJob = null
        _state.value = CountdownState.CancelledByUser
        trackFalsePositive()
    }

    fun reset() {
        _state.value = CountdownState.Idle
    }

    private suspend fun fireAlert(incidentType: IncidentType) {
        _state.value = CountdownState.Dispatching(incidentType)
        when (val result = sendEmergencyAlertUseCase(incidentType)) {
            is AlertResult.Success -> {
                _state.value = CountdownState.AlertDispatched(
                    contactsNotified = result.contactsNotified,
                    timestamp        = result.timestamp
                )
            }
            is AlertResult.Failure -> {
                _state.value = CountdownState.Error(result.reason)
            }
        }
    }

    private fun trackFalsePositive() {
        val now = System.currentTimeMillis()
        if (now - windowStartTime > FALSE_POSITIVE_WINDOW_MS) {
            falsePositiveCountInWindow = 0
            windowStartTime = now
        }
        falsePositiveCountInWindow++
        if (falsePositiveCountInWindow >= 3) {
            viewModelScope.launch {
                userPreferencesRepository.setShowFalsePositiveNudge(true)
            }
            falsePositiveCountInWindow = 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
