package com.example.safesense.ui.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.data.preferences.UserPreferencesRepository
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.repository.ContactRepository
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
    private val contactRepository: ContactRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val state: StateFlow<CountdownState> = _state.asStateFlow()

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private var timerJob: Job? = null

    fun startCountdown(incidentType: IncidentType, confidence: ConfidenceLevel) {
        if (_state.value !is CountdownState.Idle) return

        viewModelScope.launch {
            _contacts.value = contactRepository.getActiveContacts().first()
            val duration = preferencesRepository.countdownDurationSeconds.first()

            _state.value = CountdownState.CountdownRunning(
                incidentType = incidentType,
                secondsRemaining = duration,
                confidence = confidence
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
                        return@launch
                    }
                }
                dispatchAlert()
            }
        }
    }

    fun cancelByUser() {
        timerJob?.cancel()
        timerJob = null
        _state.value = CountdownState.CancelledByUser
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _state.value = CountdownState.Idle
    }

    private fun dispatchAlert() {
        val notified = _contacts.value.size
        _state.value = CountdownState.AlertDispatched(
            contactsNotified = notified,
            timestamp = System.currentTimeMillis()
        )
    }
}