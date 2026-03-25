package com.example.safesense.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.sensor.processor.GPSTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel.kt
// Location: ui/home/HomeViewModel.kt
//
// BLOCK 4 CHANGES vs Block 3:
//   - Removed lastGpsDebugText field from HomeUiState (debug display gone)
//   - Replaced the lastLocation observer with a hasValidFix observer.
//     When GPSTracker gets its first fix, hasValidFix flips to true,
//     and we update gpsActive in HomeUiState — the dot turns green.
//   - updateSensorStatus() no longer sets gpsActive from PackageManager.
//     GPS dot status now comes exclusively from GPSTracker.hasValidFix.
//     The other three sensors (accelerometer, proximity, audio) still come
//     from SensorManager as before — those are hardware presence checks,
//     not live data checks.
// ─────────────────────────────────────────────────────────────────────────────

data class HomeUiState(
    val accelerometerActive: Boolean = false,
    val proximityActive: Boolean = false,
    val gpsActive: Boolean = false,       // driven by GPSTracker.hasValidFix
    val audioActive: Boolean = false,
    val recentIncidentCount: Int = 0,
    val walkModeActive: Boolean = false,
    val showFalsePositiveNudge: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gpsTracker: GPSTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Start GPS tracking.
        // In Phase 2, SensorMonitoringService will call start() instead.
        // For now the ViewModel owns the lifecycle so the UI works standalone.
        gpsTracker.start()

        // Observe hasValidFix.
        // The moment GPSTracker receives its first location fix, this flips
        // to true and gpsActive in the UI state updates — the dot turns green
        // without any button press or screen refresh.
        gpsTracker.hasValidFix
            .onEach { hasFix ->
                _uiState.value = _uiState.value.copy(gpsActive = hasFix)
            }
            .launchIn(viewModelScope)
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Called by HomeScreen on composition with real SensorManager results.
     * Updates accelerometer, proximity, and audio dots.
     * GPS is NOT set here — it comes from GPSTracker.hasValidFix above.
     */
    fun updateSensorStatus(
        accelerometer: Boolean,
        proximity: Boolean,
        audio: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            accelerometerActive = accelerometer,
            proximityActive     = proximity,
            audioActive         = audio
            // gpsActive is intentionally omitted — GPSTracker owns it
        )
    }

    fun setWalkModeActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(walkModeActive = active)
    }

    fun dismissFalsePositiveNudge() {
        _uiState.value = _uiState.value.copy(showFalsePositiveNudge = false)
    }

    override fun onCleared() {
        super.onCleared()
        gpsTracker.stop()
    }
}