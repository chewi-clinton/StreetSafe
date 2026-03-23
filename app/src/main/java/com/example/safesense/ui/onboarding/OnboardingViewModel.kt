package com.example.safesense.ui.onboarding

import android.app.Application
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
 *
 * Why a data class? Because every time one field changes we want the Compose
 * UI to recompose only the parts that need to. StateFlow + data class gives us
 * that with zero boilerplate.
 *
 * @param currentStep           Which of the 5 steps is visible right now (0..4)
 * @param totalSteps            Always 5 — used to drive the progress indicator
 * @param smsPermissionGranted  Whether SEND_SMS is granted
 * @param locationPermissionGranted Whether ACCESS_FINE_LOCATION is granted
 * @param notificationPermissionGranted Whether POST_NOTIFICATIONS is granted (API 33+)
 * @param showBatteryWhitelistStep Whether this device is Tecno or Infinix
 * @param batteryWhitelistDone  User has confirmed they followed the whitelist steps
 * @param hasAtLeastOneContact  Whether the user added their first emergency contact
 * @param sensorsHealthy        Whether accelerometer and proximity pass the check
 * @param isComplete            All required steps done — safe to navigate to Home
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
    // DataStore is injected by Hilt — it is set up in PreferencesModule.kt
    private val dataStore: DataStore<Preferences>
) : AndroidViewModel(application) {

    // ── DataStore key ─────────────────────────────────────────────────────────
    // This key is the single source of truth for "has onboarding been done?"
    // NavGraph reads this at startup to decide which screen to show first.
    companion object {
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }

    // ── UI State ──────────────────────────────────────────────────────────────
    // We use MutableStateFlow privately and expose it as a read-only StateFlow.
    // This stops the UI from accidentally writing to the state directly.
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Initialization ────────────────────────────────────────────────────────
    init {
        // Detect if this device needs the battery whitelist step.
        // Tecno and Infinix both aggressively kill background services.
        // We read the manufacturer string at startup — it never changes at runtime.
        val manufacturer = Build.MANUFACTURER.lowercase()
        val needsWhitelist = manufacturer.contains("tecno") || manufacturer.contains("infinix")

        _uiState.value = _uiState.value.copy(
            showBatteryWhitelistStep = needsWhitelist
        )
    }

    // ── Step navigation ───────────────────────────────────────────────────────

    /**
     * Moves forward ONE step.
     *
     * SAFETY RULE: This function does nothing if the current step is not
     * "complete enough" to advance. This enforces the build-guide requirement:
     * "Cannot skip forward until current step is complete."
     */
    fun goToNextStep() {
        val state = _uiState.value

        // Check that the current step is satisfied before we let the user advance.
        val currentStepComplete = when (state.currentStep) {
            0 -> true // Welcome step — no action required, just reading
            1 -> state.smsPermissionGranted && state.locationPermissionGranted
            // Note: notification permission is optional below API 33
            2 -> !state.showBatteryWhitelistStep || state.batteryWhitelistDone
            3 -> state.hasAtLeastOneContact
            4 -> state.sensorsHealthy
            else -> false
        }

        if (!currentStepComplete) return // Guard — do not advance

        val nextStep = state.currentStep + 1

        if (nextStep >= state.totalSteps) {
            // All steps done — mark onboarding complete
            completeOnboarding()
        } else {
            _uiState.value = state.copy(currentStep = nextStep)
        }
    }

    /**
     * Moves back one step.
     * The user can always go backward — no restrictions.
     */
    fun goToPreviousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep - 1)
        }
    }

    // ── Permission callbacks ──────────────────────────────────────────────────
    // These are called from the Compose screen after the system permission dialog
    // returns a result. The screen uses rememberLauncherForActivityResult to
    // request permissions and then calls these update functions.

    fun onSmsPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(smsPermissionGranted = granted)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(notificationPermissionGranted = granted)
    }

    // ── Battery whitelist callback ────────────────────────────────────────────

    /**
     * Called when the user taps "I Have Done This" on the whitelist screen.
     * We trust the user here — we cannot programmatically verify the setting.
     */
    fun onBatteryWhitelistConfirmed() {
        _uiState.value = _uiState.value.copy(batteryWhitelistDone = true)
    }

    // ── Contact callback ──────────────────────────────────────────────────────

    /**
     * Called when Room confirms at least one active contact exists.
     * The Compose screen should collect from ContactRepository and call this.
     */
    fun onContactAdded() {
        _uiState.value = _uiState.value.copy(hasAtLeastOneContact = true)
    }

    // ── Sensor check callback ─────────────────────────────────────────────────

    /**
     * Called after the sensor health check finishes on Step 4.
     *
     * @param healthy true if accelerometer AND proximity sensor both responded.
     */
    fun onSensorCheckResult(healthy: Boolean) {
        _uiState.value = _uiState.value.copy(sensorsHealthy = healthy)
    }

    // ── Completion ────────────────────────────────────────────────────────────

    /**
     * Writes the completion flag to DataStore and updates UI state.
     *
     * Why DataStore and not SharedPreferences?
     * DataStore is the modern replacement — it is type-safe, coroutine-native,
     * and does not block the main thread. SharedPreferences can cause ANRs if
     * read on the main thread with a large file.
     */
    private fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETE_KEY] = true
            }
            // Update UI after the write succeeds so NavGraph re-reads the flag.
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }
}