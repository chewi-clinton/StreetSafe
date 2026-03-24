package com.example.safesense.data.db.mapper

import com.example.safesense.data.db.entity.ContactEntity
import com.example.safesense.domain.model.EmergencyContact

// Converts in both directions: Entity <-> Domain model
// This is the only place allowed to know about both layers
object ContactMapper {

    // Room entity → domain model (used when reading from database)
    fun toDomain(entity: ContactEntity): EmergencyContact {
        return EmergencyContact(
            id = entity.id,
            name = entity.name,
            phoneNumber = entity.phoneNumber,
            relationship = entity.relationship,
            isActive = entity.isActive
        )
    }

    // Domain model → Room entity (used when writing to database)
    fun toEntity(domain: EmergencyContact): ContactEntity {
        return ContactEntity(
            id = domain.id,
            name = domain.name,
            phoneNumber = domain.phoneNumber,
            relationship = domain.relationship,
            isActive = domain.isActive
        )
    }
}