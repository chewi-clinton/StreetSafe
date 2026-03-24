package com.example.safesense.domain.repository

import com.example.safesense.domain.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun getAllIncidents(): Flow<List<Incident>>
    suspend fun getIncidentById(id: Long): Incident?
    suspend fun insertIncident(incident: Incident): Long
    suspend fun updateIncident(incident: Incident)
    suspend fun deleteIncident(incident: Incident)
    suspend fun deleteAllIncidents()
}
