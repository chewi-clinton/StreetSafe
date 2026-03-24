package com.example.safesense.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sensitivity: Float = 0.5f,
    val shakeToAlertEnabled: Boolean = true,
    val microphoneDetectionEnabled: Boolean = false,
    val countdownDurationSeconds: Int = 15,
    val selectedLanguage: String = "FR",
    val autoStartOnReboot: Boolean = true
) {
    val countdownDurationLabel: String get() = "${countdownDurationSeconds}s"
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.sensitivity,
        preferencesRepository.shakeToAlertEnabled,
        preferencesRepository.microphoneDetectionEnabled,
        preferencesRepository.countdownDurationSeconds,
        preferencesRepository.selectedLanguage,
        preferencesRepository.autoStartOnReboot
    ) { values ->
        SettingsUiState(
            sensitivity = values[0] as Float,
            shakeToAlertEnabled = values[1] as Boolean,
            microphoneDetectionEnabled = values[2] as Boolean,
            countdownDurationSeconds = values[3] as Int,
            selectedLanguage = values[4] as String,
            autoStartOnReboot = values[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onSensitivityChange(value: Float) {
        viewModelScope.launch {
            preferencesRepository.setSensitivity(value)
        }
    }

    fun onShakeToAlertChange(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShakeToAlertEnabled(enabled)
        }
    }

    fun onMicrophoneDetectionChange(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMicrophoneDetectionEnabled(enabled)
        }
    }

    fun onCountdownDurationClick() {
        viewModelScope.launch {
            val current = uiState.value.countdownDurationSeconds
            val next = when (current) {
                10 -> 15
                15 -> 20
                20 -> 30
                30 -> 10
                else -> 15
            }
            preferencesRepository.setCountdownDurationSeconds(next)
        }
    }

    fun onLanguageSelect(language: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedLanguage(language)
        }
    }

    fun onAutoStartOnRebootChange(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoStartOnReboot(enabled)
        }
    }
}