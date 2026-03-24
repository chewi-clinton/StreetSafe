package com.example.safesense.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel.kt
// Location: ui/home/HomeViewModel.kt
//
// PURPOSE:
//   Holds all state the HomeScreen needs to display. Currently uses static /
//   fake values. Real sensor data will be wired in Phase 2 when the
//   ForegroundService is built.
//
// STATE FIELDS:
//   isMonitoring             — whether the monitoring service is running
//   accelerometerActive      — accelerometer sensor available & active
//   proximityActive          — proximity sensor available & active
//   gpsActive                — GPS location available & active
//   audioActive              — microphone / audio sensor active
//   recentIncidentCount      — number of incidents recorded today
//   showFalsePositiveNudge   — whether to show the "Too many false alerts?" banner
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Complete UI state for the Home screen.
 * isMonitoring is removed — the app always monitors.
 * showFalsePositiveNudge is set true by default so the banner is visible
 * during layout development — set it false once the UI is confirmed.
 */
data class HomeUiState(
    val accelerometerActive: Boolean = false,
    val proximityActive: Boolean = false,
    val gpsActive: Boolean = false,
    val audioActive: Boolean = false,
    val recentIncidentCount: Int = 0,
    val walkModeActive: Boolean = false,
    val showFalsePositiveNudge: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Called by HomeScreen on composition with real SensorManager results.
     * Updates all four sensor dots in a single atomic write.
     */
    fun updateSensorStatus(
        accelerometer: Boolean,
        proximity: Boolean,
        gps: Boolean,
        audio: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            accelerometerActive = accelerometer,
            proximityActive     = proximity,
            gpsActive           = gps,
            audioActive         = audio
        )
    }

    /**
     * Called by WalkModeScreen when a destination is set or cleared.
     */
    fun setWalkModeActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(walkModeActive = active)
    }

    /**
     * Dismisses the false-positive nudge banner.
     */
    fun dismissFalsePositiveNudge() {
        _uiState.value = _uiState.value.copy(showFalsePositiveNudge = false)
    }
}