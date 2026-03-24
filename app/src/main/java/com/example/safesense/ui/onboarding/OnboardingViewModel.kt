package com.example.safesense.ui.onboarding

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingViewModel.kt
// Location: ui/onboarding/OnboardingViewModel.kt
//
// PURPOSE:
//   Tracks what step the user is on (0–4), whether required permissions are
//   granted, and whether onboarding has been completed.
//
//   When onboarding is fully done it writes a boolean flag to DataStore so
//   NavGraph can decide the start destination on every future launch.
//
// STEP MAP:
//   0 → Welcome
//   1 → Permissions (SMS, Location, Notifications)
//   2 → Battery whitelist (Tecno / Infinix only — shown conditionally)
//   3 → Add first emergency contact
//   4 → Sensor check
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents the complete UI state for the Onboarding flow.
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val smsPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val showBatteryWhitelistStep: Boolean = false,
    val batteryWhitelistDone: Boolean = false,
    val hasAtLeastOneContact: Boolean = false,
    val sensorsHealthy: Boolean = false,
    val isComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    application: Application,
    private val dataStore: DataStore<Preferences>
) : AndroidViewModel(application) {

    companion object {
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val needsWhitelist = manufacturer.contains("tecno") || manufacturer.contains("infinix")

        _uiState.value = _uiState.value.copy(
            showBatteryWhitelistStep = needsWhitelist
        )
    }

    fun goToNextStep() {
        val state = _uiState.value
        val currentStepComplete = when (state.currentStep) {
            0 -> true
            1 -> state.smsPermissionGranted && state.locationPermissionGranted
            2 -> !state.showBatteryWhitelistStep || state.batteryWhitelistDone
            3 -> state.hasAtLeastOneContact
            4 -> state.sensorsHealthy
            else -> false
        }

        if (!currentStepComplete) return

        val nextStep = state.currentStep + 1
        if (nextStep >= state.totalSteps) {
            completeOnboarding()
        } else {
            _uiState.value = state.copy(currentStep = nextStep)
        }
    }

    fun goToPreviousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep - 1)
        }
    }

    fun onSmsPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(smsPermissionGranted = granted)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(notificationPermissionGranted = granted)
    }

    fun onBatteryWhitelistConfirmed() {
        _uiState.value = _uiState.value.copy(batteryWhitelistDone = true)
    }

    fun onContactAdded() {
        _uiState.value = _uiState.value.copy(hasAtLeastOneContact = true)
    }

    /**
     * Performs a REAL check for the hardware sensors required by SafeSense.
     * We check for:
     * 1. Accelerometer (used for fall and collision detection)
     * 2. Proximity Sensor (used to prevent false positives in pockets)
     */
    fun runSensorCheck() {
        val sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        val allSensorsPresent = accelerometer != null && proximity != null
        
        _uiState.value = _uiState.value.copy(sensorsHealthy = allSensorsPresent)
    }

    fun onSensorCheckResult(healthy: Boolean) {
        _uiState.value = _uiState.value.copy(sensorsHealthy = healthy)
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETE_KEY] = true
            }
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }
}