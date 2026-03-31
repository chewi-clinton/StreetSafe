package com.example.safesense.ui.home

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.data.preferences.UserPreferences
import com.example.safesense.domain.model.DetectedIncident
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.repository.IncidentRepository
import com.example.safesense.sensor.SensorForegroundService
import com.example.safesense.sensor.processor.GPSTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val accelerometerActive: Boolean = false,
    val proximityActive: Boolean = false,
    val gpsActive: Boolean = false,
    val audioActive: Boolean = false,
    val recentIncidentCount: Int = 0,
    val recentIncidents: List<Incident> = emptyList(),
    val walkModeActive: Boolean = false,
    val showFalsePositiveNudge: Boolean = true,
    val isMonitoring: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gpsTracker: GPSTracker,
    private val dataStore: DataStore<Preferences>,
    private val incidentRepository: IncidentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ── One-time events for navigation ──────────────────────────────────────
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        // ── Restore monitoring state AND start service if needed ──────────────
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val wasMonitoring = prefs[UserPreferences.IS_MONITORING] ?: false
            _uiState.value = _uiState.value.copy(isMonitoring = wasMonitoring)
            
            // Fix: If state was ON in preferences, ensure the service is actually running
            if (wasMonitoring) {
                startMonitoring()
            }
        }

        // ── Observe GPS fix ───────────────────────────────────────────────────
        gpsTracker.hasValidFix
            .onEach { hasFix ->
                _uiState.value = _uiState.value.copy(gpsActive = hasFix)
            }
            .launchIn(viewModelScope)

        // ── Observe accelerometer live status ─────────────────────────────────
        SensorForegroundService.accelerometerActive
            .onEach { isActive ->
                _uiState.value = _uiState.value.copy(accelerometerActive = isActive)
            }
            .launchIn(viewModelScope)

        // ── Observe incidents from the repository ─────────────────────────────
        incidentRepository.getAllIncidents()
            .onEach { incidents ->
                val now = System.currentTimeMillis()
                val oneDayMillis = 86_400_000L
                val todayIncidents = incidents.filter { now - it.timestampMillis < oneDayMillis }
                _uiState.value = _uiState.value.copy(
                    recentIncidentCount = todayIncidents.size,
                    recentIncidents = incidents.take(5) // Show last 5 incidents
                )
            }
            .launchIn(viewModelScope)

        // ── Observe incidents from the service ────────────────────────────────
        // This is the global listener that triggers the countdown screen
        // even if the user is just looking at the home screen.
        SensorForegroundService.incidents
            .onEach { incident ->
                _events.emit(HomeEvent.NavigateToCountdown(incident))
            }
            .launchIn(viewModelScope)
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun startMonitoring() {
        val intent = Intent(context, SensorForegroundService::class.java).apply {
            action = SensorForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
        _uiState.value = _uiState.value.copy(isMonitoring = true)
    }

    fun stopMonitoring() {
        val intent = Intent(context, SensorForegroundService::class.java).apply {
            action = SensorForegroundService.ACTION_STOP
        }
        context.startService(intent)
        _uiState.value = _uiState.value.copy(isMonitoring = false)
    }

    fun updateSensorStatus(
        proximity: Boolean,
        audio: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            proximityActive = proximity,
            audioActive     = audio
        )
    }

    fun setWalkModeActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(walkModeActive = active)
    }

    fun dismissFalsePositiveNudge() {
        _uiState.value = _uiState.value.copy(showFalsePositiveNudge = false)
    }
}

sealed class HomeEvent {
    data class NavigateToCountdown(val incident: DetectedIncident) : HomeEvent()
}
