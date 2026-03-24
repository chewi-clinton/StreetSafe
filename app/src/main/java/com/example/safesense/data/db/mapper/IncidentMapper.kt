package com.example.safesense.data.db.mapper

import com.example.safesense.data.db.entity.IncidentEntity
import com.example.safesense.domain.model.AlertStatus
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.model.IncidentType

object IncidentMapper {

    fun toDomain(entity: IncidentEntity): Incident = Incident(
        id = entity.id,
        type = IncidentType.valueOf(entity.type),
        timestampMillis = entity.timestampMillis,
        latitude = entity.latitude,
        longitude = entity.longitude,
        confidence = ConfidenceLevel.valueOf(entity.confidence),
        alertStatus = AlertStatus.valueOf(entity.alertStatus),
        contactsAlerted = entity.contactsAlerted,
        totalActiveContacts = entity.totalActiveContacts,
        gpsAttached = entity.gpsAttached,
        cancelledAfterSeconds = entity.cancelledAfterSeconds,
        accelerometerPeakValue = entity.accelerometerPeakValue,
        userNotes = entity.userNotes
    )

    fun toEntity(domain: Incident): IncidentEntity = IncidentEntity(
        id = domain.id,
        type = domain.type.name,
        timestampMillis = domain.timestampMillis,
        latitude = domain.latitude,
        longitude = domain.longitude,
        confidence = domain.confidence.name,
        alertStatus = domain.alertStatus.name,
        contactsAlerted = domain.contactsAlerted,
        totalActiveContacts = domain.totalActiveContacts,
        gpsAttached = domain.gpsAttached,
        cancelledAfterSeconds = domain.cancelledAfterSeconds,
        accelerometerPeakValue = domain.accelerometerPeakValue,
        userNotes = domain.userNotes
    )
}
