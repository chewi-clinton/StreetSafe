package com.example.safesense.domain.repository

import com.example.safesense.domain.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

// Pure Kotlin interface — no Android imports, no Room imports.
// The ViewModel depends on THIS, never on the implementation.
// This is what makes the code testable — you can swap the real DB for a fake in tests.
interface ContactRepository {

    fun getAllContacts(): Flow<List<EmergencyContact>>

    fun getActiveContacts(): Flow<List<EmergencyContact>>

    suspend fun insertContact(contact: EmergencyContact)

    suspend fun updateContact(contact: EmergencyContact)

    suspend fun deleteContact(contact: EmergencyContact)
}