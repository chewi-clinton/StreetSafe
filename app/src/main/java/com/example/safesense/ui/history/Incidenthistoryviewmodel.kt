package com.example.safesense.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.model.AlertStatus
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryFilter {
    ALL, SENT, CANCELLED, FALLS, SHAKE, SNATCH, MANUAL
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val groupedIncidents: Map<String, List<Incident>> = emptyMap(),
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val isEmpty: Boolean = false
)

@HiltViewModel
class IncidentHistoryViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(HistoryFilter.ALL)

    private val _allIncidents: StateFlow<List<Incident>> = incidentRepository
        .getAllIncidents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<HistoryUiState> = combine(
        _allIncidents,
        _activeFilter
    ) { incidents, filter ->
        val filtered = applyFilter(incidents, filter)
        val grouped = groupByDate(filtered)
        HistoryUiState(
            isLoading = false,
            groupedIncidents = grouped,
            activeFilter = filter,
            isEmpty = filtered.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun setFilter(filter: HistoryFilter) {
        _activeFilter.value = filter
    }

    fun deleteIncident(incident: Incident) {
        viewModelScope.launch {
            incidentRepository.deleteIncident(incident)
        }
    }

    fun clearAllIncidents() {
        viewModelScope.launch {
            incidentRepository.deleteAllIncidents()
        }
    }

    private fun applyFilter(incidents: List<Incident>, filter: HistoryFilter): List<Incident> =
        when (filter) {
            HistoryFilter.ALL       -> incidents
            HistoryFilter.SENT      -> incidents.filter { it.alertStatus == AlertStatus.SENT }
            HistoryFilter.CANCELLED -> incidents.filter { it.alertStatus == AlertStatus.CANCELLED }
            HistoryFilter.FALLS     -> incidents.filter { it.type == IncidentType.FALL }
            HistoryFilter.SHAKE     -> incidents.filter { it.type == IncidentType.SHAKE }
            HistoryFilter.SNATCH    -> incidents.filter { it.type == IncidentType.COLLISION }
            HistoryFilter.MANUAL    -> incidents.filter { it.type == IncidentType.MANUAL }
        }

    private fun groupByDate(incidents: List<Incident>): Map<String, List<Incident>> {
        val now = System.currentTimeMillis()
        val oneDayMillis = 86_400_000L
        val twoDaysMillis = 2 * oneDayMillis

        return incidents.groupBy { incident ->
            val diff = now - incident.timestampMillis
            when {
                diff < oneDayMillis  -> "TODAY"
                diff < twoDaysMillis -> "YESTERDAY"
                else                 -> {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = incident.timestampMillis
                    val dayName = cal.getDisplayName(
                        java.util.Calendar.DAY_OF_WEEK,
                        java.util.Calendar.LONG,
                        java.util.Locale.ENGLISH
                    )?.uppercase() ?: ""
                    val day   = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val month = cal.getDisplayName(
                        java.util.Calendar.MONTH,
                        java.util.Calendar.SHORT,
                        java.util.Locale.ENGLISH
                    )?.uppercase() ?: ""
                    val year  = cal.get(java.util.Calendar.YEAR)
                    "$dayName — $day $month $year"
                }
            }
        }
    }
}
