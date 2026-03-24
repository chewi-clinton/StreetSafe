package com.example.safesense.data.db.dao

import androidx.room.*
import com.example.safesense.data.db.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    // Returns a Flow — the UI will automatically refresh whenever the table changes
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveContacts(): Flow<List<ContactEntity>>

    // suspend means this runs on a background thread (never block the main thread)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}