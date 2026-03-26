package com.example.safesense.domain.model

sealed class AlertResult {
    data class Success(
        val contactsNotified: Int,
        val timestamp: Long
    ) : AlertResult()

    data class Failure(
        val reason: String
    ) : AlertResult()
}
