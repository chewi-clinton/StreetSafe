package com.example.safesense.domain.model

// This is the DOMAIN model — pure Kotlin, zero Android imports.
// The UI and ViewModels work with this. They never see ContactEntity directly.
data class EmergencyContact(
    val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isActive: Boolean = true
)