package com.example.safesense.data.repository

import com.example.safesense.data.db.dao.IncidentDao
import com.example.safesense.data.db.mapper.IncidentMapper
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IncidentRepositoryImpl @Inject constructor(
    private val dao: IncidentDao
) : IncidentRepository {

    override fun getAllIncidents(): Flow<List<Incident>> =
        dao.getAllIncidents().map { entities ->
            entities.map { IncidentMapper.toDomain(it) }
        }

    override suspend fun getIncidentById(id: Long): Incident? =
        dao.getIncidentById(id)?.let { IncidentMapper.toDomain(it) }

    override suspend fun insertIncident(incident: Incident): Long =
        dao.insertIncident(IncidentMapper.toEntity(incident))

    override suspend fun updateIncident(incident: Incident) =
        dao.updateIncident(IncidentMapper.toEntity(incident))

    override suspend fun deleteIncident(incident: Incident) =
        dao.deleteIncident(IncidentMapper.toEntity(incident))

    override suspend fun deleteAllIncidents() =
        dao.deleteAllIncidents()
}
