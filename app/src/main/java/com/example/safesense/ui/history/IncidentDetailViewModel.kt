package com.example.safesense.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentDetailUiState(
    val isLoading: Boolean = true,
    val incident: Incident? = null,
    val error: String? = null
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState.asStateFlow()

    fun loadIncident(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val incident = incidentRepository.getIncidentById(id)
            if (incident != null) {
                _uiState.value = IncidentDetailUiState(isLoading = false, incident = incident)
            } else {
                _uiState.value = IncidentDetailUiState(isLoading = false, error = "Incident not found")
            }
        }
    }

    fun deleteIncident() {
        val incident = _uiState.value.incident ?: return
        viewModelScope.launch {
            incidentRepository.deleteIncident(incident)
        }
    }
}
