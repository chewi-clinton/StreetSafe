package com.example.safesense.ui.walkmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.domain.model.AlertStatus
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.usecase.LogIncidentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class WalkModeState {
    // User is filling in the form — not yet started
    data class Setup(
        val destination: String = "",
        val durationMinutes: Int = 20
    ) : WalkModeState()

    // Walk Mode is actively running
    data class Active(
        val destination: String,
        val durationMinutes: Int,
        val secondsRemaining: Int,
        val startedAtLabel: String,   // e.g. "09:23"
        val expectedArrivalLabel: String // e.g. "09:48"
    ) : WalkModeState()

    // User tapped "I'm Safe" — walk mode ended normally
    object Ended : WalkModeState()
}

sealed class WalkModeEvent {
    object TriggerEmergencyCountdown : WalkModeEvent()
}

@HiltViewModel
class WalkModeViewModel @Inject constructor(
    private val logIncidentUseCase: LogIncidentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<WalkModeState>(WalkModeState.Setup())
    val state: StateFlow<WalkModeState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<WalkModeEvent>()
    val events: SharedFlow<WalkModeEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun onDestinationChange(value: String) {
        val current = _state.value as? WalkModeState.Setup ?: return
        _state.value = current.copy(destination = value)
    }

    fun onDurationChange(minutes: Int) {
        val current = _state.value as? WalkModeState.Setup ?: return
        _state.value = current.copy(durationMinutes = minutes)
    }

    fun startWalkMode() {
        val setup = _state.value as? WalkModeState.Setup ?: return
        if (setup.destination.isBlank()) return

        val now = System.currentTimeMillis()
        val arrivalTime = now + (setup.durationMinutes * 60 * 1000L)
        val totalSeconds = setup.durationMinutes * 60

        _state.value = WalkModeState.Active(
            destination          = setup.destination,
            durationMinutes      = setup.durationMinutes,
            secondsRemaining     = totalSeconds,
            startedAtLabel       = timeFormat.format(Date(now)),
            expectedArrivalLabel = timeFormat.format(Date(arrivalTime))
        )

        timerJob = viewModelScope.launch {
            for (secondsLeft in totalSeconds downTo 0) {
                val current = _state.value as? WalkModeState.Active ?: break
                _state.value = current.copy(secondsRemaining = secondsLeft)
                if (secondsLeft == 0) break
                delay(1_000L)
            }
            // Timer expired — trigger emergency countdown
            _events.emit(WalkModeEvent.TriggerEmergencyCountdown)
        }
    }

    fun endWalkMode() {
        val current = _state.value as? WalkModeState.Active
        timerJob?.cancel()
        timerJob = null
        
        viewModelScope.launch {
            if (current != null) {
                logIncidentUseCase.invoke(
                    incidentType = IncidentType.WALK_MODE,
                    latitude = null, // In a real app, we'd get the current location
                    longitude = null
                )
            }
            _state.value = WalkModeState.Ended
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
