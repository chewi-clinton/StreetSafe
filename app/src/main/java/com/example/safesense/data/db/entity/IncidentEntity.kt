package com.example.safesense.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val timestampMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val confidence: String,
    val alertStatus: String,
    val contactsAlerted: Int,
    val totalActiveContacts: Int,
    val gpsAttached: Boolean,
    val cancelledAfterSeconds: Int?,
    val accelerometerPeakValue: Float?,
    val userNotes: String?
)
