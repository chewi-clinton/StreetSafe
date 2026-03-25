package com.example.safesense.ui.home

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.data.preferences.UserPreferences
import com.example.safesense.sensor.SensorForegroundService
import com.example.safesense.sensor.processor.GPSTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel.kt
// Location: ui/home/HomeViewModel.kt
//
// STEP 6 CHANGE vs Step 5:
//   - Observes SensorForegroundService.accelerometerActive (a companion object
//     StateFlow) to drive the accelerometer dot on HomeScreen.
//   - Removed the manual updateSensorStatus(accelerometer=...) call for
//     accelerometer — it now comes from the live processor, not PackageManager.
//   - Proximity and audio dots still come from updateSensorStatus() for now
//     (their processors get live StateFlows in Steps 8).
// ─────────────────────────────────────────────────────────────────────────────

data class HomeUiState(
    val accelerometerActive: Boolean = false,
    val proximityActive: Boolean = false,
    val gpsActive: Boolean = false,
    val audioActive: Boolean = false,
    val recentIncidentCount: Int = 0,
    val walkModeActive: Boolean = false,
    val showFalsePositiveNudge: Boolean = true,
    val isMonitoring: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gpsTracker: GPSTracker,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // ── Restore monitoring state ──────────────────────────────────────────
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val wasMonitoring = prefs[UserPreferences.IS_MONITORING] ?: false
            _uiState.value = _uiState.value.copy(isMonitoring = wasMonitoring)
        }

        // ── Observe GPS fix ───────────────────────────────────────────────────
        gpsTracker.hasValidFix
            .onEach { hasFix ->
                _uiState.value = _uiState.value.copy(gpsActive = hasFix)
            }
            .launchIn(viewModelScope)

        // ── STEP 6: Observe accelerometer live status ─────────────────────────
        // SensorForegroundService.accelerometerActive is a companion object
        // StateFlow that AccelerometerProcessor.isActive is piped into whenever
        // the service starts or stops. This means the ViewModel can observe the
        // real processor state without needing to bind to the service.
        SensorForegroundService.accelerometerActive
            .onEach { isActive ->
                _uiState.value = _uiState.value.copy(accelerometerActive = isActive)
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

    /**
     * Called by HomeScreen on composition.
     * Only proximity and audio come from here now.
     * Accelerometer dot → SensorForegroundService.accelerometerActive
     * GPS dot           → GPSTracker.hasValidFix
     */
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