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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    init {
        // Load contacts
        viewModelScope.launch {
            contactRepository.getActiveContacts().collect {
                _contacts.value = it
            }
        }
    }

    /**
     * Starts the countdown for the specified incident.
     * 
     * FIX: This is now called ONLY by the CountdownScreen when it enters the composition
     * using the arguments passed during navigation. This prevents duplicate listeners
     * from firing multiple countdowns.
     */
    fun startCountdown(incidentType: IncidentType, confidence: ConfidenceLevel) {
        if (_state.value is CountdownState.CountdownRunning) return
        countdownJob?.cancel()

        viewModelScope.launch {
            // Read duration from preferences synchronously once when starting
            val duration = try {
                userPreferencesRepository.countdownDurationSeconds.first()
            } catch (e: Exception) {
                15 // fallback
            }

            android.util.Log.d(
                "SafeSense/Countdown",
                "Starting countdown: ${duration}s for $incidentType"
            )

            for (secondsLeft in duration downTo 1) {
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
        countdownJob?.cancel()
        countdownJob = null
        _state.value = CountdownState.CancelledByUser
        trackFalsePositive()
    }

    /**
     * Resets the state back to Idle.
     */
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
        viewModelScope.launch {
            // Just notify repository - logic for nudge happens there or in HomeViewModel
            userPreferencesRepository.setShowFalsePositiveNudge(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
