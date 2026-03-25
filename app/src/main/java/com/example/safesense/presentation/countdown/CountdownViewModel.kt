package com.example.safesense.presentation.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.domain.model.IncidentType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CountdownViewModel : ViewModel() {

    private val _state = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val state: StateFlow<CountdownState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun onIncidentDetected(type: IncidentType, confidenceLevel: Float) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var secondsRemaining = 15
            while (secondsRemaining > 0) {
                _state.value = CountdownState.CountdownRunning(
                    type = type,
                    secondsRemaining = secondsRemaining,
                    confidenceLevel = confidenceLevel
                )
                delay(1000L)
                secondsRemaining--
            }
            dispatchAlert()
        }
    }

    fun onUserCancelled() {
        countdownJob?.cancel()
        _state.value = CountdownState.CancelledByUser
        resetToIdle()
    }

    private fun dispatchAlert() {
        // SMS dispatch will be wired here in a later step
        _state.value = CountdownState.AlertDispatched(
            contactsNotified = 0,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun resetToIdle() {
        viewModelScope.launch {
            delay(2000L)
            _state.value = CountdownState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
