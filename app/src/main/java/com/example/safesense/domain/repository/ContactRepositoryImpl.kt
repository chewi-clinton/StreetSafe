package com.example.safesense.data.repository

import com.example.safesense.data.db.dao.ContactDao
import com.example.safesense.data.db.mapper.ContactMapper
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// @Inject tells Hilt: "when you need to build this class, inject ContactDao automatically"
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {

    // .map{} transforms each emission: Flow<List<Entity>> → Flow<List<EmergencyContact>>
    // Every time the database changes, the UI gets fresh domain models automatically
    override fun getAllContacts(): Flow<List<EmergencyContact>> {
        return contactDao.getAllContacts().map { entityList ->
            entityList.map { entity -> ContactMapper.toDomain(entity) }
        }
    }

    override suspend fun insertContact(contact: EmergencyContact) {
        contactDao.insertContact(ContactMapper.toEntity(contact))
    }

    override suspend fun updateContact(contact: EmergencyContact) {
        contactDao.updateContact(ContactMapper.toEntity(contact))
    }

    override suspend fun deleteContact(contact: EmergencyContact) {
        contactDao.deleteContact(ContactMapper.toEntity(contact))
    }
}